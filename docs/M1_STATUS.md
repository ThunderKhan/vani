# M1 — Minimum Complete Offline Loop Status

**Milestone state:** `IMPLEMENTED / PHYSICAL EVIDENCE OPEN`  
**Last updated:** 2026-09-12

M1 implementation is now present in the Android module. M1 remains an evidence gate: source code and CI cannot close the milestone without the two-phone offline run required by `docs/ROADMAP.md`.

## Implemented vertical slice

```text
HOLD PTT
  -> Android on-device recognizer
  -> recognizer endpoint callback
  -> editable transcript
  -> bounded versioned semantic bundle
  -> direct local Wi-Fi TCP
  -> receiver bundle validation
  -> offline Android TTS
  -> playback
  -> ACK
```

### Components

| M1 requirement | Implementation |
|---|---|
| PTT capture | `M1Activity`: hold-to-speak button; microphone permission requested only on use |
| Endpointing | Android recognizer `onEndOfSpeech`; final transcript is accepted only from `onResults` |
| Offline STT | `M1Speech.startOfflineAsr()` uses API 31+ on-device recognizer only; no network fallback |
| Transcript correction | Editable transcript field before sending |
| Semantic bundle | `M1Bundle`: versioned JSON, UTF-8, bounded to 64 KiB |
| Direct transport | `M1Transport`: local TCP on port 42425 |
| Receiver validation | version, size, message-ID consistency, UTF-8/JSON validation |
| Offline TTS | `M1Speech.speakOffline()` rejects voices marked `isNetworkConnectionRequired` |
| ACK | Receiver returns accepted/duplicate/message ID/first-audio marker |
| Truthful states | `QUEUED`, `TRANSFERRED`, `DELIVERED`, `ACKNOWLEDGED`, `FAILED`; duplicate suppression is explicit |
| Timing | monotonic elapsed timing for ASR, transport, TTS first-audio, and end-to-end path |
| Basic retry | one retry after a failed socket attempt |
| Duplicate handling | durable `SharedPreferences` played-message marker; duplicate playback is suppressed |
| Failure handling | peer/ACK/TTS failures become `FAILED`, never an ambiguous `sent` state |

## Deliberate M1 boundary

This is the first vertical slice, not the final VĀṆI architecture. Security envelope, durable outbox semantics beyond the basic state record, fragmentation, relay/store-carry-forward, critical-field safety policy, ten-language production packs, and routing belong to later milestones.

The current transport is local Wi-Fi TCP because it is the smallest direct transport already proven at the protocol-contract level in the repository. It does not imply a mesh or Bluetooth Mesh implementation.

## Evidence still required

M1 cannot be marked complete until a physical run records:

- two real Android phones;
- Internet disabled for the judged path;
- offline STT actually producing the sender transcript;
- receiver playing locally synthesized speech from the received text;
- ACK returned to the sender;
- bundle byte size from the actual serialization;
- ASR endpoint and completion timestamps;
- transport timing;
- TTS first-audio timing;
- end-to-end timing;
- duplicate-input behavior;
- peer-loss or ACK-loss behavior;
- repeatable build/run procedure.

Until that run exists, **M1 is not complete**.
