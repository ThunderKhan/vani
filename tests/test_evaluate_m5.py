"""Contract tests: use SYNTHETIC fixtures; not SIH results. Execution deferred."""
import importlib.util
import json
from pathlib import Path

SCRIPT = Path(__file__).resolve().parents[1] / "tools" / "evaluate_m5.py"
spec = importlib.util.spec_from_file_location("vani_evaluate_m5", SCRIPT)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


def observation(**changes):
    row = {
        "experiment_id": "synthetic-only",
        "git_commit": "synthetic-no-commit",
        "device_id": "synthetic",
        "model_id": "synthetic",
        "language": "hi",
        "transport": "synthetic",
        "run_kind": "simulation",
        "reference": "सेक्टर 13 में प्रवेश न करें",
        "hypothesis": "सेक्टर 13 में प्रवेश न करें",
    }
    row.update(changes)
    return row


def test_score_identical_and_missing_negation():
    correct = module.score("Do not enter", "Do not enter")
    wrong = module.score("Do not enter", "Do enter")
    assert correct["word_edits"] == 0
    assert wrong["word_edits"] >= 1


def test_critical_fields_only_scored_when_annotated():
    unknown = module.summarise([observation()])
    assert unknown["critical_field_accuracy"] is None
    known = module.summarise([observation(
        critical_fields=[{"type": "negation", "expected": "न करें", "observed": "करें"}]
    )])
    assert known["critical_field_accuracy"] == 0.0


def test_real_and_simulated_never_pooled(tmp_path):
    file = tmp_path / "records.jsonl"
    rows = [observation(), observation(
        experiment_id="other", run_kind="real_device", device_id="physical"
    )]
    file.write_text("\n".join(json.dumps(x, ensure_ascii=False) for x in rows), encoding="utf-8")
    result = module.evaluate(file)
    assert result["cohorts"]["simulation"]["aggregate"]["observations"] == 1
    assert result["cohorts"]["real_device"]["aggregate"]["observations"] == 1


def test_invalid_language_rejected():
    try:
        module.clean_observation(observation(language="xx"), 1)
    except ValueError:
        return
    assert False, "unknown language was accepted"
