#!/usr/bin/env python3
"""VĀṆI M5 evaluation: score recorded observations without inventing results.

Usage:
  python tools/evaluate_m5.py experiments/m5/observations.jsonl --output results/m5.json

Input is newline-delimited JSON with explicit experiment/device/model provenance.
Only real-device observations are included by default; simulated observations
must be opted into, and are always reported as a separate cohort.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import math
import statistics
import sys
import unicodedata
from collections import defaultdict
from pathlib import Path
from typing import Any

LANGUAGES = ("hi", "gu", "mr", "kn", "ml", "ta", "te", "or", "bn", "en")
DELIVERED_STATES = {"delivered_device", "playback_started", "acknowledged_person"}
MANDATORY = ("experiment_id", "git_commit", "device_id", "model_id", "language",
             "transport", "run_kind", "reference", "hypothesis")


def normalize(text: str) -> str:
    if not isinstance(text, str):
        raise ValueError("reference, hypothesis and field values must be strings")
    return " ".join(unicodedata.normalize("NFC", text).split())


def edit_distance(a: list[Any], b: list[Any]) -> int:
    previous = list(range(len(b) + 1))
    for i, left in enumerate(a, 1):
        row = [i]
        for j, right in enumerate(b, 1):
            row.append(min(row[-1] + 1, previous[j] + 1,
                           previous[j - 1] + (left != right)))
        previous = row
    return previous[-1]


def score(reference: str, hypothesis: str) -> dict[str, int | float | None]:
    ref = normalize(reference)
    hyp = normalize(hypothesis)
    a, b = ref.split() if ref else [], hyp.split() if hyp else []
    ac = [c for c in ref if not c.isspace()]
    bc = [c for c in hyp if not c.isspace()]
    we = edit_distance(a, b)
    ce = edit_distance(ac, bc)
    return {
        "word_edits": we, "reference_words": len(a),
        "character_edits": ce, "reference_characters": len(ac),
    }


def percentile(values: list[float], percent: float) -> float | None:
    if not values:
        return None
    ordered = sorted(values)
    return ordered[max(0, math.ceil(percent / 100 * len(ordered)) - 1)]


def clean_observation(obj: Any, line: int) -> dict[str, Any]:
    if not isinstance(obj, dict):
        raise ValueError(f"line {line}: each observation must be an object")
    for key in MANDATORY:
        if key not in obj or not isinstance(obj[key], str) or not obj[key].strip():
            raise ValueError(f"line {line}: missing/nonempty string required for {key}")
    if obj["language"] not in LANGUAGES:
        raise ValueError(f"line {line}: unsupported language {obj['language']!r}")
    if obj["run_kind"] not in ("real_device", "simulation"):
        raise ValueError(f"line {line}: run_kind must be real_device or simulation")
    if len(obj["reference"]) > 8192 or len(obj["hypothesis"]) > 8192:
        raise ValueError(f"line {line}: transcript size exceeds evaluation bound")
    fields = obj.get("critical_fields")
    if fields is not None:
        if not isinstance(fields, list) or len(fields) > 32:
            raise ValueError(f"line {line}: critical_fields must be a list of at most 32")
        for field in fields:
            if not isinstance(field, dict) or not all(
                isinstance(field.get(k), str) for k in ("type", "expected", "observed")
            ):
                raise ValueError(f"line {line}: critical fields require type/expected/observed strings")
    for key in ("e2e_latency_ms", "payload_bytes", "audio_reference_bytes"):
        if key in obj:
            val = obj[key]
            if isinstance(val, bool) or not isinstance(val, (int, float)) or not math.isfinite(val) or val < 0:
                raise ValueError(f"line {line}: {key} must be nonnegative finite numeric")
    if "delivery_state" in obj and obj["delivery_state"] not in (
        "queued", "transferred", "relayed", "delivered_device",
        "playback_started", "acknowledged_person", "expired", "failed"
    ):
        raise ValueError(f"line {line}: invalid delivery state")
    return obj


def summarise(records: list[dict[str, Any]]) -> dict[str, Any]:
    if not records:
        return {"observations": 0, "wer": None, "cer": None,
                "critical_field_accuracy": None, "delivery_ratio": None,
                "e2e_latency_ms": None, "compression_ratio": None}
    word_errors = ref_words = char_errors = ref_chars = 0
    critical_correct = critical_count = annotated = 0
    delivered = observed_delivery = 0
    latencies: list[float] = []
    compression: list[float] = []
    for item in records:
        s = score(item["reference"], item["hypothesis"])
        word_errors += s["word_edits"]
        ref_words += s["reference_words"]
        char_errors += s["character_edits"]
        ref_chars += s["reference_characters"]
        if "critical_fields" in item:
            annotated += 1
            for field in item["critical_fields"]:
                critical_count += 1
                critical_correct += normalize(field["expected"]) == normalize(field["observed"])
        if "delivery_state" in item:
            observed_delivery += 1
            delivered += item["delivery_state"] in DELIVERED_STATES
        if "e2e_latency_ms" in item:
            latencies.append(float(item["e2e_latency_ms"]))
        if item.get("payload_bytes", 0) > 0 and "audio_reference_bytes" in item:
            compression.append(item["audio_reference_bytes"] / item["payload_bytes"])
    return {
        "observations": len(records),
        "reference_words": ref_words,
        "word_edits": word_errors,
        "wer": word_errors / ref_words if ref_words else None,
        "reference_characters": ref_chars,
        "character_edits": char_errors,
        "cer": char_errors / ref_chars if ref_chars else None,
        "critical_field_accuracy": critical_correct / critical_count if critical_count else None,
        "critical_fields_scored": critical_count,
        "observations_with_critical_annotations": annotated,
        "delivery_ratio": delivered / observed_delivery if observed_delivery else None,
        "delivery_state_observations": observed_delivery,
        "e2e_latency_ms": (
            {"p50": percentile(latencies, 50), "p90": percentile(latencies, 90),
             "p95": percentile(latencies, 95), "max": max(latencies), "n": len(latencies)}
            if latencies else None
        ),
        # Median across per-message ratios, not a pooled channel compression ratio.
        "median_per_message_audio_to_bundle_ratio": statistics.median(compression) if compression else None,
        "compression_observations": len(compression),
    }


def evaluate(path: Path) -> dict[str, Any]:
    raw = path.read_bytes()
    observations: list[dict[str, Any]] = []
    for lineno, text in enumerate(raw.decode("utf-8-sig").splitlines(), 1):
        if text.strip():
            observations.append(clean_observation(json.loads(text), lineno))
    if not observations:
        raise ValueError("no observations; refusing to produce an empty benchmark")
    cohorts = defaultdict(list)
    for record in observations:
        cohorts[record["run_kind"]].append(record)
    output: dict[str, Any] = {
        "schema_version": 1,
        "source_sha256": hashlib.sha256(raw).hexdigest(),
        "scoring_policy": {
            "normalization": "Unicode NFC and collapsed whitespace; case/punctuation retained",
            "wer_tokenization": "whitespace",
            "cer_tokenization": "Unicode code points excluding whitespace",
            "critical_fields": "exact normalized field comparison with annotated expected/observed",
            "missing_data": "null, never imputed as success",
            "latency": "nearest-rank percentiles of user-supplied measured values",
            "simulated_values": "kept distinct from physical observations",
        },
        "cohorts": {},
    }
    for kind, rows in sorted(cohorts.items()):
        by_language = {language: summarise([row for row in rows if row["language"] == language])
                       for language in LANGUAGES}
        vals = [s["wer"] for s in by_language.values() if s["wer"] is not None]
        output["cohorts"][kind] = {
            "aggregate": summarise(rows),
            "macro_wer_across_observed_languages": statistics.mean(vals) if vals else None,
            "languages_with_reference_words": len(vals),
            "languages": by_language,
        }
    return output


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("records", type=Path, help="UTF-8 JSONL observation file")
    parser.add_argument("--output", type=Path, help="write scored JSON here; default stdout")
    args = parser.parse_args()
    try:
        result = evaluate(args.records)
        data = json.dumps(result, indent=2, ensure_ascii=False, allow_nan=False) + "\n"
        if args.output:
            args.output.parent.mkdir(parents=True, exist_ok=True)
            args.output.write_text(data, encoding="utf-8")
        else:
            sys.stdout.write(data)
    except (OSError, UnicodeError, ValueError, json.JSONDecodeError) as exc:
        print(f"evaluation rejected: {exc}", file=sys.stderr)
        return 2
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
