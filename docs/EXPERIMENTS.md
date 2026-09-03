# Experiment Catalogue

## E00 — Offline proof
Two phones, internet blocked. Verify no network dependency.

## E01 — STT language coverage
Per-language fixed corpus. WER/CER/CFA.

## E02 — TTS intelligibility
Human transcription and pronunciation labels.

## E03 — Endpointing
Noise/silence/speech boundary sweep. Measure cut/delay.

## E04 — Direct BLE
Transfer success, latency, reconnect, MTU/fragment effects.

## E05 — Direct local Wi-Fi
Same metrics under equivalent bundle loads.

## E06 — Three-phone line topology
A cannot directly reach C; B relays.

## E07 — Store-carry-forward
Destination absent at send time, later contact.

## E08 — Routing baseline comparison
Direct, controlled flood, Spray-and-Wait, PRoPHET-like.

## E09 — Critical-information guard
Compare with/without selective confirmation.

## E10 — Quantization ladder
FP32/FP16/int8 and later research variants.

## E11 — Low-device benchmark
CPU/RAM/thermal/energy under sustained operation.

## E12 — Audio vs semantic bundle
Raw PCM and realistic compressed-audio baseline.

## E13 — Security abuse
Replay, forged ACK, oversized fragments, priority abuse.

## E14 — Full end-to-end semantic success
Held-out speaker -> STT -> validation -> disrupted route -> TTS -> blinded listener.

## E15 — Code-switching
Controlled code-mixed phrases for required languages.

Every experiment gets:
- hypothesis;
- fixed config;
- seed/topology;
- inclusion/exclusion rules;
- raw data;
- derived metrics;
- result summary;
- negative results.
