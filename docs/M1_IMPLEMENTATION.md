# M1 Implementation

M1 is the first complete offline speech-to-speech vertical slice.

## Implemented

1. Capture speech through the Android speech interface.
2. Prefer the Android on-device recognizer on API 31+.
3. Present the transcript for editing before transmission.
4. Encode the transcript as a versioned semantic bundle.
5. Bound the bundle to the existing 64 KiB transport limit.
6. Transfer the UTF-8 bundle over direct local TCP.
7. Verify SHA-256 integrity and return an ACK.
8. Track `QUEUED -> TRANSFERRED -> DELIVERED -> ACKNOWLEDGED` and `FAILED` states.
9. Decode and validate the received semantic bundle.
10. Keep the TTS path explicitly offline-safe by rejecting voices that require network connectivity.
11. Record timing and device evidence for later M0/M1 runs.

## Deliberate boundaries

- The Android platform recognizer is not treated as proof of offline ASR unless the device exposes an on-device recognizer and the physical experiment demonstrates it.
- `EXTRA_PREFER_OFFLINE` is a preference, not evidence.
- Android `TextToSpeech` voice metadata is recorded; a voice marked as network-required must not be accepted as an offline product voice.
- The current TCP transport remains a local feasibility implementation. It is not the final M4 multi-hop transport, authentication, or encryption design.
- M1 is not declared complete until two physical Android devices demonstrate the full loop with Internet unavailable.

## Current code boundary

The M1 implementation lives beside the disposable M0 probe so the experiment can be replaced cleanly after feasibility is established. `SemanticBundle` is the logical M1 message model and uses JSON only as the current bounded transport encoding.
