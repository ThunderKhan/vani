# M0 — Truth and Feasibility Status

**Milestone state:** `IN PROGRESS`  
**Last updated:** 2026-09-12

M0 is an evidence gate. Repository contracts, CI, or a successful API call do **not** mean the milestone is complete.

## Gate status

| Gate | Status | Evidence / next action |
|---|---|---|
| Working requirement ledger + open questions | READY / official artifact link still open | `docs/M0_REQUIREMENTS.md` |
| Exact ten-language candidate STT/TTS inventory | READY as a candidate inventory | `models/manifests/m0_candidates.json` |
| Licence/provenance audit | IN PROGRESS | Several candidates remain `review_required`; exact selected artifacts/checksums are not frozen |
| Offline ASR on real low/mid Android | OPEN / probe implemented | `M0-ASR-ANDROID-001`; `SpeechProbe` uses Android on-device recognition when the OS exposes it and records capability/results |
| Offline TTS on real low/mid Android | OPEN / probe implemented | `M0-TTS-ANDROID-001`; `SpeechProbe` records engine/voice and `networkConnectionRequired` |
| Two-phone Unicode transfer with internet unavailable | PROBE IMPLEMENTED / PHYSICAL RUN OPEN | `android/feasibility-probe`; execute `M0-UNICODE-LINK-001` |
| Cold/warm latency + model footprint + memory observations | OPEN / recording contracts implemented | `experiments/m0/asr-device-run.template.json`, `experiments/m0/tts-device-run.template.json` |
| Experiment/result metadata contract | READY | `experiments/schema/experiment-record.schema.json` |
| Initial risk register | READY | `docs/RISK_REGISTER.md` |

## Implemented M0 speech probe

The Android feasibility probe now contains a platform speech instrumentation layer in addition to the Unicode link test. It:
- checks whether any Android recognition service exists;
- checks API 31+ on-device recognizer availability;
- prefers the on-device recognizer when Android exposes one;
- records recognition elapsed time and transcript/error state;
- requests microphone permission only when ASR is invoked;
- initializes Android TextToSpeech for a requested locale;
- records the selected engine, voice, language support, and `networkConnectionRequired` flag;
- synthesizes a short utterance so the runtime path is exercised rather than merely enumerated.

The probe deliberately does **not** turn `EXTRA_PREFER_OFFLINE`, a successful TTS call, or an available voice into an offline claim. Actual offline evidence still requires controlled physical-device execution.

## Candidate coverage finding

The current inventory contains at least one plausible STT and one plausible TTS candidate for every required language. This is **coverage feasibility only**.

It does not prove:
- that the same model family is suitable on Android;
- that all selected model weights are redistributable under one clean product policy;
- that latency/RAM/storage are acceptable;
- that accuracy is acceptable per language;
- that the project can ship all ten language packs within a practical footprint.

Whisper remains a useful baseline for several required languages but is not counted as the all-ten solution because the current language table does not include Odia/Oriya.

## Current implementation boundary

The M0 probe is intentionally disposable. It is a measurement instrument, not the production VĀṆI UI and not the final mesh/security protocol. The next production milestone should reuse only interfaces and evidence-backed components rather than copying probe assumptions wholesale.

## M0 exit declaration

**NOT COMPLETE.** Physical Unicode-link evidence plus real-device offline ASR/TTS evidence and the corresponding licence/provenance freeze are still required.
