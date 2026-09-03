# Benchmark Plan

## Device metadata

Capture:
- manufacturer/model;
- SoC;
- RAM;
- Android version;
- thermal state;
- battery state;
- power mode;
- app build;
- runtime threads.

## Speech benchmark

At minimum per language:
- clean speech;
- fan/traffic/crowd-like noise;
- numeric instruction;
- location instruction;
- negation;
- ordinary sentence;
- code-mixed case when realistic.

## Network benchmark

Message sizes:
- small routine;
- typical instruction;
- upper bound;
- fragmented.

Scenarios:
- short-range direct;
- edge-of-reliable-contact;
- disconnect/reconnect;
- relay;
- queue backlog.

## Outputs

CSV/JSON rows should include:
`experiment_id, commit, device, model, language, condition, timings, memory, size, success, failure_reason`.
