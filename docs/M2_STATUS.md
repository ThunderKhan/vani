# M2 — Ten-Language Credibility Status

**Milestone state:** `IMPLEMENTATION STARTED / PHYSICAL LANGUAGE EVIDENCE OPEN`

M2 is the ten-language credibility milestone. It is not complete merely because ten locale constants exist. The repository must eventually demonstrate each language's offline STT and TTS path on target Android hardware and preserve the corresponding provenance/licence and evaluation evidence.

## Implemented in this branch

- explicit registry for the ten SIH26173 languages and their `*-IN` locale tags;
- language-aware, conservative NFC/whitespace normalization;
- deterministic per-language evaluation fixtures covering native scripts plus numbers, times, and negation;
- M2 speech facade over the existing Android on-device STT and local TTS adapters;
- asynchronous TTS capability probing with explicit network-voice rejection;
- M2 Android research screen for selected-language and all-language capability probing;
- language-path manifest recording the current platform-provider boundary and verification status;
- M2 entry point from the existing M1 activity.

## Deliberate boundary

The current repository does **not** contain ten independently distributed STT/TTS neural model packs. The M2 implementation therefore does not claim that all ten languages are verified. Android platform speech capability is an integration path only; target-device language support must be exercised and recorded before a language can be marked verified.

No cloud fallback is introduced. A missing on-device recognizer is reported as unavailable. A TTS voice that reports `isNetworkConnectionRequired` is rejected.

## Physical evidence still required

For each of the ten languages:

1. run the offline ASR path on target hardware;
2. run the offline TTS path on target hardware;
3. record model/engine provenance and licence status;
4. record a reproducible evaluation fixture and scoring configuration;
5. record latency/RTF, memory, and storage/resource observations;
6. preserve failures instead of marking unsupported paths as successful.

M2 remains open until those evidence items exist. The team's intentional decision to postpone physical testing for M0/M1/M2 does not convert implementation into verification.

## M1 relationship

The existing M1 direct loop is reused rather than forked. M2 only expands the speech/language surface; it does not add mesh routing, safety policy, production cryptography, or resilient store-carry-forward behavior. Those belong to later milestones.

## Quality rules

- No generic `multilingual` label is treated as ten-language proof.
- No language is silently substituted with another locale.
- No transliteration is performed by the transport normalizer.
- No network-backed recognizer or voice is used as a fallback.
- No benchmark number is recorded until it is measured.
- Missing/corrupt language assets must produce an explicit failure state.
