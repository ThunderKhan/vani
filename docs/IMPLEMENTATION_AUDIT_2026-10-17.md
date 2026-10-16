# VĀṆI — implementation audit (17 October 2026)

**Scope:** inspection of `main` at `f42a1b666afc76bd0cf2ea86622b480084f6d39b`,
the independent `m2-ten-language-credibility` branch, the SIH26173 research dossier
and the integration work in `feat/vani-integration-m2-m6-audit`.

**Testing policy:** no new functional, real-device or human-intelligibility test run
was conducted during this implementation pass. Existing test sources and CI
workflows remain in the repository; they are not new experiment evidence.

## What source code now contains

| Area | Source-implementation status | Evidence gate |
| --- | --- | --- |
| M0 | Android Unicode/TCP and Android speech capability probe exists | real offline device/license measurements open |
| M1 | PTT/on-device OS ASR, editable text, local JSON/TCP, platform TTS and playback-receipt feasibility flow | two-phone reproduction open; **not open-source speech compliance** |
| M2 | Previously diverged M2 registry, pack checksum abstraction, ten-language fixtures, score helpers and probing UI integrated into mainline candidate | ten independently audited STT/TTS models and physical evidence open |
| M3 | deterministic critical-field guard, explicit confirmation and bundle metadata | empirical precision, critical-field and human comprehension metrics open |
| M4 | binary codecs, fragmentation, bounded reassembly, SQLite queues, replay protection, AES-GCM fail-closed destination engine, HMAC relay wrapper, bounded routing policies and manual-key local-Wi-Fi diagnostic | key provisioning, BLE, actual relay/mobility integration, authenticated end-user ACK and physical trials open |
| M5 | Python JSONL evaluator, observation schema, ten-language WER/CER, annotated CFA, latency/transfer measures; tests written | physical corpora, simulator, power profiling, ablations, listener trials and reproducible results open |
| M6 | linked M1, M2, M4 Android research screens and explicit warning/capability boundaries | production UX, conversation mode, accessible inbox/alerts, onboarding and judged demonstration open |

## Highest-risk dependencies / truthful limitations

1. **Required open-source speech models:** Android platform-provided recognition/
   synthesis may be local on some devices but can rely on proprietary software,
   undisclosed weights or unsupported languages. This is a feasibility tool, not
   a verified SIH production stack. Validate candidate *source code, weights,
   dataset provenance, licence, redistribution and Android runtime* separately
   for each of Hindi, Gujarati, Marathi, Kannada, Malayalam, Tamil, Telugu,
   Odia, Bengali and English. Absence of required model assets is a blocker,
   not a cue to fabricate success.
2. **Networking:** M1 legacy TCP and M4 experimental binary direct TCP both
   require known peer IPv4 addresses on a real local network. No BLE/GATT,
   Wi-Fi Direct pairing/discovery, multi-peer topology, automatic opportunistic
   routing, energy-aware peer discovery or hardware bridge is integrated.
3. **Security:** M1 SHA-256 is integrity checking, not authentication. M4
   destination bundles use AES-GCM and identity/destination matching only when
   provisioned with the shared secret; HMAC relay protection requires a
   separately provisioned domain key. The M4 diagnostic deliberately has no
   trusted key exchange, access control or loss/theft recovery. Its peer receipt
   is not a final signed delivery or person acknowledgement.
4. **Offline truth:** the application has no cloud dependency by design in its
   direct path, but platform speech-engine behavior and physical airplane-mode
   execution have not yet been verified. This is not a completed offline claim.
5. **Safety:** detector confidence is not calibrated ASR confidence; heuristic
   critical detection can miss or falsely flag non-English/script-sensitive
   terms. Sender confirmation is not proof that recognition was correct.
6. **Android lifecycle:** persistent M4 outbox and manual retry exist; automatic
   restart recovery, background BLE scanning, foreground-service policy,
   notifications, audio interruptions and battery constraints need integrated
   implementation plus controlled device evidence.
7. **Scientific evidence:** nothing in source code establishes a WER, a power
   figure, a network range, an end-to-end latency percentile or ten-language
   intelligibility. Empty or failed recognition must be counted, not excluded.

## Next engineering sequence (not a claim of completion)

1. Freeze a licence-compatible open-source STT/TTS model pack for one language,
   integrate actual local inference and file integrity preflight; scale with
   individually verified packs to all ten, especially Odia.
2. Integrate M4 encrypted binary delivery and persistent retry into the final
   sender/receiver UX; provision identities/keys with an authenticated workflow
   and distinguish peer acceptance, verified device ACK, playback and person ACK.
3. Add true Android BLE discovery/session/fragment transport and an explicit
   multi-hop store-carry-forward service with queue limits and bounded routing.
4. Implement audio capture/VAD/endpointing, hands-free conversation, safe alert
   playback, user inbox, replay and permission/lifecycle handling independently
   of proprietary Android on-device ASR.
5. Build complete M5 scientific experiment runners, low/mid device profiles,
   physical line/sparse-contact topologies, matched audio baseline, human
   listening studies, statistical intervals and security failure tests.
6. Package M6 accessible production UX, licence/provenance ledger, real demo,
   performance report and fallback recording.
7. **Only then conduct the deferred comprehensive testing phase**, record
   failures, fix regressions, and close milestone evidence gates individually.

The implementation branch must not be described as a competition-complete or
production-secure version until these requirements and gates are satisfied.
