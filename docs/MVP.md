# MVP Specification

## Relationship to the roadmap

This document defines **Tier 1 — Credible MVP** product scope. It is not an alternate milestone sequence.

The canonical implementation order is defined in [`ROADMAP.md`](./ROADMAP.md):

```text
M0 -> M1 -> M2 -> M3 -> M4 -> M5 -> M6
```

For VĀṆI, Tier 1 MVP is reached only after **M0, M1, and M2** are complete:

- **M0** proves feasibility, licences, and first real-device offline operation;
- **M1** proves the complete two-phone offline communication loop;
- **M2** proves credible support for all ten required languages.

Advanced multi-hop routing, store-carry-forward research, and publication-grade evaluation are later milestones and are not required to call the direct ten-language product thesis a credible MVP.

## MVP definition

For this project, MVP means **minimum credible end-to-end system**, not a toy demo.

The MVP must prove the complete communication thesis on physical Android devices with internet disabled.

## Required MVP vertical slice

```text
PTT press
 -> capture speech
 -> endpoint utterance
 -> offline STT
 -> show/edit/confirm transcript
 -> construct authenticated semantic bundle
 -> direct BLE/local-Wi-Fi transfer
 -> validate on receiver
 -> offline TTS
 -> playback
 -> acknowledgement returned
```

## MVP acceptance criteria

### Offline
- airplane-mode test;
- network access blocked;
- cold app start succeeds;
- no remote inference;
- no remote authentication requirement.

### Speech
- at least one complete real-device flow before scaling;
- all ten languages represented before MVP is declared complete;
- language pack presence checked before recording;
- transcript errors visible and correctable;
- TTS produces intelligible speech on receiver.

### Protocol
- `protocol_version`;
- `message_id`;
- source/destination;
- language;
- priority;
- transcript;
- expiry;
- ACK policy;
- integrity/authentication data;
- bounded payload length;
- deterministic serialization.

### Delivery
- queue;
- retry timeout;
- deduplication;
- received/delivered distinction;
- acknowledgement path.

### Metrics
- endpoint latency;
- STT latency;
- bundle size;
- network latency;
- TTS first-audio latency;
- end-to-end latency;
- peak RAM;
- model footprint.

## MVP demonstration

1. Disable internet on both phones.
2. Select a language.
3. Speak a realistic instruction.
4. Show transcript.
5. Show semantic packet size.
6. Send.
7. Play synthesized speech remotely.
8. Acknowledge on destination.
9. Show true delivery state at sender.
10. Display measured pipeline timings.

## MVP failure requirements

The demo remains defensible when:
- peer disconnects;
- language model is unavailable;
- STT confidence is insufficient;
- a duplicate arrives;
- a packet fails validation;
- ACK is lost;
- receiver TTS fails.

Each case must produce a truthful UI state.

## MVP exit gate

Tier 1 MVP is complete only when the roadmap exit gates for **M0, M1, and M2** have passed in addition to the acceptance criteria in this document.

No multi-hop, adaptive routing, publication claim, or hardware extension should distract from the project until this complete path is stable.
