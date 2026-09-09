# M0 — Truth and Feasibility Status

**Milestone state:** `IN PROGRESS`  
**Last updated:** 2026-09-09

M0 is an evidence gate. Repository contracts or Android CI passing do **not** mean the milestone is complete.

## Gate status

| Gate | Status | Evidence / next action |
|---|---|---|
| Working requirement ledger + open questions | READY / official artifact link still open | `docs/M0_REQUIREMENTS.md` |
| Exact ten-language candidate STT/TTS inventory | READY as a candidate inventory | `models/manifests/m0_candidates.json`; run `python tools/m0_validate.py` |
| Licence/provenance audit | IN PROGRESS | Several candidates remain `review_required`; exact selected artifacts/checksums are not frozen |
| Offline ASR on real low/mid Android | OPEN | Run `M0-ASR-ANDROID-001`; do not substitute emulator/desktop timing |
| Offline TTS on real low/mid Android | OPEN | Run `M0-TTS-ANDROID-001` |
| Two-phone Unicode transfer with internet unavailable | **PROBE IMPLEMENTED / PHYSICAL RUN OPEN** | `android/feasibility-probe`; execute `M0-UNICODE-LINK-001` on two physical phones and commit the resulting evidence JSON |
| Cold/warm latency + model footprint + memory observations | OPEN | Record in experiment JSON from the actual speech-device runs |
| Experiment/result metadata contract | READY | `experiments/schema/experiment-record.schema.json` |
| Initial risk register | READY, updated for current M0 blocker | `docs/RISK_REGISTER.md` |

## Implemented M0 transport probe

`android/feasibility-probe` is a disposable native Android instrument for the direct Unicode-transfer gate. It:
- hosts a bounded local TCP receiver on port 42424;
- sends arbitrary UTF-8 payloads, including the ten-script fixture;
- rejects payloads above 64 KiB before serialization/allocation;
- verifies SHA-256 before accepting received bytes;
- returns an explicit acknowledgement containing payload hash/length;
- records sender-side monotonic round-trip timing;
- records airplane-mode and `NET_CAPABILITY_VALIDATED` state;
- records device/RAM/battery observations and the Git commit embedded at build time;
- marks an otherwise successful transfer `blocked` rather than `pass` if validated Internet is present or the build commit is unknown.

This probe is not the production protocol and deliberately makes no mesh, security, STT, or TTS claim.

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

The transport probe should be physically executed as soon as two phones are available, but implementation attention can now move in parallel to the first Android speech-runtime probe. The first speech probe should optimize for learning, not final model quality, and must record real latency/RAM/footprint without being mislabeled as ten-language completion.

## M0 exit declaration

**NOT COMPLETE.** Physical Unicode-link evidence plus real-device offline ASR/TTS evidence are still required.
