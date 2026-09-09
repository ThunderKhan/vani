import copy
import importlib.util
import json
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MODULE_PATH = ROOT / "tools" / "m0_validate.py"
SPEC = importlib.util.spec_from_file_location("m0_validate", MODULE_PATH)
assert SPEC and SPEC.loader
m0_validate = importlib.util.module_from_spec(SPEC)
sys.modules["m0_validate"] = m0_validate
SPEC.loader.exec_module(m0_validate)


class M0ContractTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.manifest = json.loads((ROOT / "models/manifests/m0_candidates.json").read_text(encoding="utf-8"))
        cls.schema = json.loads((ROOT / "experiments/schema/experiment-record.schema.json").read_text(encoding="utf-8"))
        cls.template = json.loads((ROOT / "experiments/m0/first-device-run.template.json").read_text(encoding="utf-8"))

    def test_repository_contracts_validate(self):
        summary = m0_validate.validate_repo(ROOT)
        self.assertEqual(summary["asr_languages_covered"], 10)
        self.assertEqual(summary["tts_languages_covered"], 10)

    def test_missing_required_language_fails(self):
        broken = copy.deepcopy(self.manifest)
        broken["required_languages"] = [row for row in broken["required_languages"] if row["code"] != "or"]
        with self.assertRaises(m0_validate.ContractError):
            m0_validate.validate_candidate_manifest(broken)

    def test_baseline_only_does_not_satisfy_coverage(self):
        broken = copy.deepcopy(self.manifest)
        for candidate in broken["candidates"]:
            if candidate["role"] == "asr" and "or" in candidate["languages"] and candidate["status"] == "candidate":
                candidate["languages"].remove("or")
        with self.assertRaises(m0_validate.ContractError):
            m0_validate.validate_candidate_manifest(broken)

    def test_experiment_schema_has_exact_language_set(self):
        m0_validate.validate_experiment_schema(self.schema)

    def test_template_cannot_pretend_hardware_success(self):
        broken = copy.deepcopy(self.template)
        broken["result"] = "pass"
        with self.assertRaises(m0_validate.ContractError):
            m0_validate.validate_template(broken)


if __name__ == "__main__":
    unittest.main()
