# M1 — Minimum Complete Offline Loop Status

**Milestone state:** `IMPLEMENTATION COMPLETE / PHYSICAL EVIDENCE OPEN`
**Last updated:** 2026-09-12

M1 source implementation is present in the Android vertical slice. The milestone remains open until the required two-phone offline demonstration is recorded.

## Implemented vertical slice

```text
PTT / on-device ASR
  -> editable transcript
  -> M1Bundle v1
  -> bounded UTF-8 local TCP frame + SHA-256
  -> receiver validates + deduplicates
  -> receiver-side offline-safe TTS
  -> ACK + hash verification
  -> delivery state
```

### Components

| Requirement | Implementation |
|---|---|
| Speech capture | `M1Speech` + Android microphone permission |
| Offline ASR path | API 31+ on-device recognizer when the device exposes it; no network-backed fallback |
| Transcript handoff | ASR result is surfaced into the editable sender field before send |
| Semantic bundle | `M1Bundle` v1 with bounded JSON encoding and strict validation |
| UTF-8 transport | M1 TCP frame with 64 KiB maximum and SHA-256 carried across the wire |
| Direct transport | Local TCP on port `42425` |
| Receiver validation | Hash verification followed by `M1Bundle.decode()` validation |
| Duplicate handling | Durable `SharedPreferences` message-ID marker; duplicate bundle is ACKed but not re-played |
| Delivery states | `QUEUED`, `TRANSFERRED`, `DELIVERED`, `ACKNOWLEDGED`, `FAILED` |
| TTS | Android TTS rejects a selected voice when `isNetworkConnectionRequired == true` |
| M0 access | M1 UI can launch the standalone M0 feasibility probe |
| CI | Android build/test workflow runs on every Android source/configuration push |

## Boundary

M1 intentionally does not implement mesh routing, relay/store-and-forward, fragmentation, production cryptography, critical-information safety policy, or the final ten-language model packs. Those are later milestones and should not be pulled into the M1 proof prematurely.

## Physical gate still open

Implementation completion is not evidence of offline behavior. M1 remains open until two real Android devices demonstrate the full path with Internet unavailable, including offline ASR, editable transcript, bundle transfer, receiver-side TTS playback, ACK, timing, duplicate handling, and a failure/peer-loss case.
