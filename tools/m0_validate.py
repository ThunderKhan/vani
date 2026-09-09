#!/usr/bin/env python3
"""Validate VANI M0 feasibility contracts using only the Python standard library.

This validator proves repository consistency, not physical-device feasibility.
It must never report M0 complete merely because manifests are structurally valid.
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any

REQUIRED_LANGUAGES = {
    "hi": "Hindi",
    "gu": "Gujarati",
    "mr": "Marathi",
    "kn": "Kannada",
    "ml": "Malayalam",
    "ta": "Tamil",
    "te": "Telugu",
    "or": "Odia",
    "bn": "Bengali",
    "en": "English",
}

COUNTED_CANDIDATE_STATUSES = {"candidate"}
LICENSE_STATUSES = {
    "provisional_clear",
    "review_required",
    "incompatible_without_distribution_decision",
}
ANDROID_STATUSES = {
    "not_proven",
    "documented_mobile_candidate",
    "documented_third_party_runtime",
    "portable_phone_candidate",
    "documented_android_support",
}
REDISTRIBUTION_STATUSES = {"provisional_clear", "review_required", "conditional"}


class ContractError(ValueError):
    pass


def _load_json(path: Path) -> dict[str, Any]:
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except FileNotFoundError as exc:
        raise ContractError(f"missing file: {path}") from exc
    except json.JSONDecodeError as exc:
        raise ContractError(f"invalid JSON in {path}: {exc}") from exc
    if not isinstance(value, dict):
        raise ContractError(f"top-level JSON value must be an object: {path}")
    return value


def validate_candidate_manifest(manifest: dict[str, Any]) -> dict[str, Any]:
    if manifest.get("schema_version") != 1:
        raise ContractError("candidate manifest schema_version must be 1")

    language_rows = manifest.get("required_languages")
    if not isinstance(language_rows, list):
        raise ContractError("required_languages must be a list")

    discovered: dict[str, str] = {}
    for row in language_rows:
        if not isinstance(row, dict):
            raise ContractError("each required_languages row must be an object")
        code = row.get("code")
        name = row.get("name")
        if not isinstance(code, str) or not isinstance(name, str):
            raise ContractError("language rows require string code and name")
        if code in discovered:
            raise ContractError(f"duplicate language code: {code}")
        discovered[code] = name

    if discovered != REQUIRED_LANGUAGES:
        missing = sorted(set(REQUIRED_LANGUAGES) - set(discovered))
        extra = sorted(set(discovered) - set(REQUIRED_LANGUAGES))
        renamed = sorted(
            code for code in set(discovered) & set(REQUIRED_LANGUAGES)
            if discovered[code] != REQUIRED_LANGUAGES[code]
        )
        raise ContractError(
            f"required language set mismatch; missing={missing}, extra={extra}, renamed={renamed}"
        )

    candidates = manifest.get("candidates")
    if not isinstance(candidates, list) or not candidates:
        raise ContractError("candidates must be a non-empty list")

    coverage = {"asr": {code: [] for code in REQUIRED_LANGUAGES}, "tts": {code: [] for code in REQUIRED_LANGUAGES}}
    ids: set[str] = set()
    pending_licence: list[str] = []
    android_unproven: list[str] = []

    required_fields = {
        "id", "role", "status", "languages", "source", "code_license",
        "weights_license", "dataset_provenance", "license_status",
        "android_status", "redistribution_status", "notes", "evidence",
    }

    for candidate in candidates:
        if not isinstance(candidate, dict):
            raise ContractError("every candidate must be an object")
        missing_fields = sorted(required_fields - set(candidate))
        if missing_fields:
            raise ContractError(f"candidate missing fields {missing_fields}: {candidate.get('id', '<unknown>')}")

        candidate_id = candidate["id"]
        if not isinstance(candidate_id, str) or not candidate_id:
            raise ContractError("candidate id must be a non-empty string")
        if candidate_id in ids:
            raise ContractError(f"duplicate candidate id: {candidate_id}")
        ids.add(candidate_id)

        role = candidate["role"]
        if role not in {"asr", "tts"}:
            raise ContractError(f"candidate {candidate_id}: role must be asr or tts")

        status = candidate["status"]
        if status not in {"candidate", "baseline_only", "reference_only"}:
            raise ContractError(f"candidate {candidate_id}: invalid status {status!r}")

        languages = candidate["languages"]
        if not isinstance(languages, list) or not languages:
            raise ContractError(f"candidate {candidate_id}: languages must be a non-empty list")
        unknown_languages = sorted(set(languages) - set(REQUIRED_LANGUAGES))
        if unknown_languages:
            raise ContractError(f"candidate {candidate_id}: unknown languages {unknown_languages}")

        evidence = candidate["evidence"]
        if not isinstance(evidence, list) or not evidence or not all(isinstance(x, str) and x.startswith("https://") for x in evidence):
            raise ContractError(f"candidate {candidate_id}: evidence must contain HTTPS source URLs")

        if candidate["license_status"] not in LICENSE_STATUSES:
            raise ContractError(f"candidate {candidate_id}: invalid license_status")
        if candidate["android_status"] not in ANDROID_STATUSES:
            raise ContractError(f"candidate {candidate_id}: invalid android_status")
        if candidate["redistribution_status"] not in REDISTRIBUTION_STATUSES:
            raise ContractError(f"candidate {candidate_id}: invalid redistribution_status")

        if candidate["license_status"] != "provisional_clear" or candidate["redistribution_status"] != "provisional_clear":
            pending_licence.append(candidate_id)
        if candidate["android_status"] == "not_proven":
            android_unproven.append(candidate_id)

        if status in COUNTED_CANDIDATE_STATUSES:
            for code in languages:
                coverage[role][code].append(candidate_id)

    missing_asr = [code for code, model_ids in coverage["asr"].items() if not model_ids]
    missing_tts = [code for code, model_ids in coverage["tts"].items() if not model_ids]
    if missing_asr or missing_tts:
        raise ContractError(f"candidate coverage incomplete; missing ASR={missing_asr}, missing TTS={missing_tts}")

    return {
        "asr_languages_covered": len(REQUIRED_LANGUAGES) - len(missing_asr),
        "tts_languages_covered": len(REQUIRED_LANGUAGES) - len(missing_tts),
        "pending_licence_candidates": sorted(set(pending_licence)),
        "android_unproven_candidates": sorted(set(android_unproven)),
        "coverage": coverage,
    }


def validate_experiment_schema(schema: dict[str, Any]) -> None:
    if schema.get("type") != "object":
        raise ContractError("experiment schema top-level type must be object")
    required = set(schema.get("required", []))
    must_have = {
        "schema_version", "experiment_id", "git_commit", "build_variant",
        "started_at_utc", "offline_conditions", "device_profile",
        "runtime_configuration", "language", "result", "raw_stage_metrics",
        "derived_metrics",
    }
    missing = sorted(must_have - required)
    if missing:
        raise ContractError(f"experiment schema required list is missing: {missing}")

    properties = schema.get("properties")
    if not isinstance(properties, dict):
        raise ContractError("experiment schema properties must be an object")
    language_enum = properties.get("language", {}).get("enum")
    if set(language_enum or []) != set(REQUIRED_LANGUAGES):
        raise ContractError("experiment schema language enum must exactly match required languages")


def validate_template(template: dict[str, Any]) -> None:
    if template.get("result") != "blocked":
        raise ContractError("unexecuted M0 template must remain result=blocked")
    offline = template.get("offline_conditions")
    if not isinstance(offline, dict):
        raise ContractError("template offline_conditions must be an object")
    if offline.get("internet_blocked") is not False:
        raise ContractError("template must not claim internet_blocked=true before execution")
    if not template.get("failure_reason"):
        raise ContractError("blocked template must explain why it is blocked")


def validate_repo(root: Path) -> dict[str, Any]:
    manifest = _load_json(root / "models/manifests/m0_candidates.json")
    experiment_schema = _load_json(root / "experiments/schema/experiment-record.schema.json")
    template = _load_json(root / "experiments/m0/first-device-run.template.json")

    summary = validate_candidate_manifest(manifest)
    validate_experiment_schema(experiment_schema)
    validate_template(template)
    return summary


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1])
    args = parser.parse_args(argv)

    try:
        summary = validate_repo(args.root)
    except ContractError as exc:
        print(f"M0 contract validation: FAIL\n{exc}", file=sys.stderr)
        return 1

    print("M0 contract validation: PASS")
    print(f"ASR candidate coverage: {summary['asr_languages_covered']}/10")
    print(f"TTS candidate coverage: {summary['tts_languages_covered']}/10")
    print(f"Candidates needing licence/redistribution review: {len(summary['pending_licence_candidates'])}")
    print(f"Candidates with Syntax6 Android deployability unproven: {len(summary['android_unproven_candidates'])}")
    print("M0 complete: NO — real-device offline ASR/TTS, Unicode transport, and measured resource evidence are still mandatory.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
