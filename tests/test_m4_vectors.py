import json
import pathlib
import subprocess

ROOT = pathlib.Path(__file__).resolve().parents[1]

def test_experimental_vector_matches_independent_reference():
    fixture = json.loads((ROOT / "protocol/test-vectors/m4-v1-experimental.json").read_text())
    actual = subprocess.check_output(["python3", str(ROOT / "tools/protocol_reference.py")], text=True).strip()
    assert actual == fixture["canonical_hex"]
