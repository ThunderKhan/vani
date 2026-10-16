# M5 — Scientific evaluation implementation status

**State: scorer + schema + synthetic contract tests added; real benchmarks deferred.**

Available now:
- `tools/evaluate_m5.py`: deterministic Unicode-aware baseline WER/CER;
- exact separately annotated critical-field accuracy;
- observed delivery ratio, nearest-rank P50/P90/P95 and compression ratios;
- ten-language and macro breakdowns;
- physical versus simulated cohort separation;
- input hash and explicit scoring policy;
- `experiments/schema/m5-observation.schema.json`;
- `tests/test_evaluate_m5.py` (written but execution postponed by request).

The scoring script does **not** conduct or synthesize measurements. It intentionally
refuses empty inputs and records missing fields as unknown.

Not yet implemented or established:
- actual M0/M1/M2/M4 physical trial records;
- ten-language ASR benchmark corpus and human TTS intelligibility study;
- source-level stage timing from all Android pipeline steps;
- energy/RAM/CPU/temperature runner and reproducible profiler records;
- controlled flood versus Spray-and-Wait comparable trace simulation;
- calibration and semantic confirmation ablations;
- compressed audio baseline under equivalent link conditions;
- confidence intervals at speaker level;
- reproducible empirical M5 result tables.

A code implementation is not an empirical M5 pass. Execute the final test plan
with physical phones, fixed fixtures, measured audio and consented listeners.
