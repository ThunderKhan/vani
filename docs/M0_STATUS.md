# M0 — Truth and Feasibility Status

**Milestone state:** `IN PROGRESS`  
**Last updated:** 2026-09-09

M0 is an evidence gate. Repository contracts passing CI do **not** mean the milestone is complete.

## Gate status

| Gate | Status | Evidence / next action |
|---|---|---|
| Working requirement ledger + open questions | READY / official artifact link still open | `docs/M0_REQUIREMENTS.md` |
| Exact ten-language candidate STT/TTS inventory | READY as a candidate inventory | `models/manifests/m0_candidates.json`; run `python tools/m0_validate.py` |
| Licence/provenance audit | IN PROGRESS | Several candidates remain `review_required`; exact selected artifacts/checksums are not frozen |
| Offline ASR on real low/mid Android | OPEN | Run `M0-ASR-ANDROID-001`; do not substitute emulator/desktop timing |
| Offline TTS on real low/mid Android | OPEN | Run `M0-TTS-ANDROID-001` |
| Two-phone Unicode transfer with internet unavailable | OPEN | Run `M0-UNICODE-LINK-001` on two physical Android phones |
| Cold/warm latency + model footprint + memory observations | OPEN | Record in experiment JSON from the actual device run |
| Experiment/result metadata contract | READY | `experiments/schema/experiment-record.schema.json` |
| Initial risk register | READY, updated for current M0 blocker | `docs/RISK_REGISTER.md` |

## Candidate coverage finding

The current inventory contains at least one plausible STT and one plausible TTS candidate for every required language. This is **coverage feasibility only**.

It does not prove:
- that the same model family is suitable on Android;
- that all selected model weights are redistributable under one clean product policy;
- that latency/RAM/storage are acceptable;
- that accuracy is acceptable per language;
- that the project can ship all ten language packs within a practical footprint.

One important negative result is preserved explicitly: Whisper is a useful baseline for several required languages but is not counted as the all-ten solution because the current language table does not include Odia/Oriya.

## Next highest-risk assumption

> **Can Syntax6 produce at least one legally distributable, genuinely offline Android STT+TTS path on modest hardware without creating an impractical language-pack/storage architecture?**

The first hardware probe should optimize for **learning**, not final model quality. A compact Hindi/English ASR/TTS path is acceptable for the first device run as long as it records real latency/RAM/footprint and does not get mislabeled as ten-language completion.

## M0 exit declaration

**NOT COMPLETE.** Physical-device evidence is intentionally absent at this point and must remain visibly open until measured.
