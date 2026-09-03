# Model Selection and Device Policy

## Principle

A model is not “best” because it has the lowest WER. Deployment is a Pareto problem.

Dimensions:
- WER/CER;
- critical-field accuracy;
- latency;
- RTF;
- RAM;
- model size;
- energy;
- operator compatibility;
- licence;
- language coverage.

## Candidate device tiers

### Tier L
Constrained CPU/RAM. Prefer int8 models and compact voices.

### Tier M
Balanced model with stronger accuracy.

### Tier H
Higher-accuracy local path where hardware permits.

## Selection process

1. licence audit;
2. desktop sanity test;
3. Android conversion;
4. operator compatibility;
5. cold/warm benchmark;
6. per-language accuracy;
7. noise test;
8. critical-field test;
9. energy/thermal test;
10. Pareto analysis.

## Quantization ladder

- FP32 baseline;
- FP16;
- dynamic int8;
- calibrated static int8;
- QAT if needed;
- 4-bit/research compression only with evidence.

## Policy safety

The runtime may adapt model profile, but:
- critical messages should not silently downgrade accuracy;
- policy changes must be logged;
- user may see “reduced-performance mode”;
- unsupported language/model combination must fail explicitly.
