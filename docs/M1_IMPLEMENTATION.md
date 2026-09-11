# M1 Implementation

M1 is the first complete offline speech-to-speech vertical slice at the implementation level. The milestone remains physically open until the two-phone offline evidence gate passes.

## Implemented

1. Capture speech through the Android speech interface.
2. Require the Android on-device recognizer on API 31+ for the judged M1 ASR path.
3. Present the transcript for editing before transmission.
4. Encode the transcript as a versioned `M1Bundle`.
5. Bound the bundle to the existing 64 KiB transport limit.
6. Transfer the UTF-8 bundle over direct local TCP on port `42425`.
7. Carry and verify SHA-256 for the bundle across the M1 wire frame and ACK.
8. Track `QUEUED -> TRANSFERRED -> DELIVERED -> ACKNOWLEDGED` and `FAILED` states.
9. Decode and validate the received `M1Bundle` before invoking TTS.
10. Keep the TTS path explicitly offline-safe by rejecting voices that require network connectivity.
11. Record timing and device evidence through the existing M0 evidence infrastructure where applicable.

## Deliberate boundaries

- The Android platform recognizer is not treated as proof of offline ASR merely because `EXTRA_PREFER_OFFLINE` is set. The physical experiment must demonstrate the device-side behavior with Internet unavailable.
- API 31+ on-device recognition is required for M1; there is no network-backed fallback in `M1Speech`.
- Android `TextToSpeech` voice metadata is checked; a voice marked as network-required is rejected.
- The current TCP transport is a local feasibility implementation. It is not the final M4 multi-hop transport, authentication, or production cryptography design.
- M1 is not declared complete until two physical Android devices demonstrate the full loop with Internet unavailable.

## Current code boundary

The M1 implementation lives under `android/feasibility-probe/src/main/java/dev/syntax6/vani/m0probe/m1/`. `M1Bundle` is the logical M1 message model and uses bounded JSON as the current transport encoding. The separate `MainActivity` remains the M0 transport/speech feasibility instrument and is reachable from the M1 activity for testing.
