# Evaluation Framework

## Core principle

The unit of success is not “ASR produced text.” It is “the intended recipient received and correctly understood the actionable meaning.”

## Metrics

### WER
`(S + D + I) / N`

### CER
Character-level analogue.

### RTF
`processing_time / audio_duration`

### Critical Field Accuracy
`correct critical fields / all critical fields`

### Semantic Message Success Rate
A message succeeds if:
- delivered before expiry;
- intended action preserved;
- all mandatory critical fields preserved;
- receiver reconstruction is intelligible.

### Delivery ratio
`delivered before expiry / created`

### Relay overhead
`(bundle transmissions - delivered bundles) / delivered bundles`

### End-to-end latency

`endpoint + STT + validation + queue + network + reassembly + TTS startup + playback start`

### Energy per success
Total relevant device energy divided by successfully delivered messages.

## Reporting

Always report:
- P50/P90/P95 latency;
- per-language values;
- failure count;
- device;
- model;
- runtime;
- build commit;
- network topology;
- number of trials.

## Baselines

- compressed audio over same link;
- manual text entry;
- direct-only semantic text;
- controlled flood;
- Spray-and-Wait;
- contact-history routing;
- multiple speech model configurations.

## Cross-layer experiments

- endpoint threshold vs WER/latency;
- confidence confirmation vs CFA/latency;
- bundle size vs fragmentation/delivery;
- routing priority vs fairness;
- model tier vs success/energy;
- relay policy vs battery.
