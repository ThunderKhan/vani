# Observability and Experiment Provenance

## Required timestamps

- speech start;
- speech end;
- VAD activation;
- endpoint decision;
- STT start;
- first partial;
- STT final;
- validation start/end;
- bundle encoded;
- enqueued;
- first byte/frame sent;
- peer accepted;
- relay accepted/forwarded;
- destination reassembled;
- verified;
- TTS start;
- first audio;
- playback end;
- ACK creation/receipt.

## Experiment record

```json
{
  "experiment_id": "",
  "git_commit": "",
  "build_variant": "",
  "device": {},
  "model": {},
  "language": "",
  "fixture": "",
  "topology": "",
  "seed": null,
  "metrics": {},
  "failure_reason": null
}
```

## Logging rules

Production:
- no raw audio;
- no private transcript by default;
- pseudonymous IDs;
- bounded retention.

Research mode:
- explicitly labelled;
- consented fixtures;
- exportable structured logs.
