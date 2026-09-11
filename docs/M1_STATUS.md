# M1 — Minimum Complete Offline Loop Status

**Milestone state:** `IMPLEMENTATION COMPLETE / PHYSICAL EVIDENCE OPEN`
**Last updated:** 2026-09-12

M1 source implementation is present in the Android vertical slice. The milestone remains open until the required two-phone offline demonstration is recorded.

## Implemented vertical slice

```text
PTT / ASR
  -> transcript surfaced for correction
  -> SemanticBundle v1
  -> UTF-8 bounded local TCP frame + SHA-256
  -> receiver validates + deduplicates
  -> receiver exposes transcript to TTS
  -> ACK
  -> delivery state
```

### Components

| Requirement | Implementation |
|---|---|
| Speech capture | `SpeechProbe` + Android microphone permission |
| Offline ASR path | API 31+ on-device recognizer when the device exposes it; no product-level fallback claim |
| Transcript handoff | ASR result is surfaced into the sender payload field for correction before send |
| Semantic bundle | `SemanticBundle` mirrors the logical schema fields and validates bounds |
| UTF-8 transport | Existing bounded `LinkProtocol` with 64 KiB maximum and SHA-256 |
| Direct transport | Local TCP on port `42424` |
| Receiver validation | Frame integrity followed by `SemanticBundle.decodeUtf8()` validation |
| Duplicate handling | Durable `SharedPreferences` message-ID marker; duplicate bundle is ACKed but not re-played |
| Delivery states | `QUEUED`, `TRANSFERRED`, `DELIVERED`, `ACKNOWLEDGED`, `FAILED` |
| TTS | Android TTS probe reports engine/voice/network requirement and synthesizes audio |
| Evidence | Existing JSON evidence capture and clipboard export |
| CI | Android build/test workflow runs on every push |

## Boundary

M1 intentionally does not implement mesh routing, relay/store-and-forward, fragmentation, production cryptography, critical-information safety policy, or the final ten-language model packs. Those are later milestones and should not be pulled into the M1 proof prematurely.

## Physical gate still open

Implementation completion is not evidence of offline behavior. M1 remains open until two real Android devices demonstrate the full path with Internet unavailable, including offline ASR, editable transcript, bundle transfer, receiver-side TTS playback, ACK, timing, duplicate handling, and a failure/peer-loss case.
