# M5 observation contract (testing deliberately deferred)

The evaluator at `tools/evaluate_m5.py` is implemented, but **no experiment is
recorded as complete**. Do not commit recordings or private transcripts without
consent and an explicit retention policy.

Use a UTF-8 JSONL file with one independently measured event per line:

```json
{"experiment_id":"E14-run-001","git_commit":"REPLACE_WITH_REAL_40_CHAR_COMMIT","device_id":"device-class-and-pseudonym","model_id":"actual-model-or-platform-engine-identifier","language":"hi","transport":"local-wifi","run_kind":"real_device","reference":"सेक्टर 13 में प्रवेश न करें","hypothesis":"सेक्टर 13 में प्रवेश न करें","critical_fields":[{"type":"negation","expected":"न करें","observed":"न करें"}],"delivery_state":"delivered_device","e2e_latency_ms":950,"payload_bytes":120,"audio_reference_bytes":2400}
```

**This example is illustrative only; values are NOT measured and must never be
copied into reported results.** The scorer does not generate example evidence.

Run during the final testing phase:

```bash
python tools/evaluate_m5.py experiments/m5/observations.jsonl --output results/m5-report.json
```

Required identification: experiment ID, real git commit, pseudonymous device ID,
real model/engine ID, language, actual transport and real-device/simulation kind.
Reference and hypothesis must be measured transcriptions for the same utterance.

`critical_fields` must be independently annotated. Missing annotations are
recorded as unknown, not successes. For TTS, use a separate human-blinded listener
transcript as the hypothesis when measuring intelligibility; do not confuse the
sender ASR hypothesis with human perception of receiver audio.

WER uses whitespace segmentation and CER uses Unicode code points with
whitespace removed; both preserve punctuation/case. This is a declared baseline
and is **not** a replacement for language-specific tokenization analysis.

Record raw hardware, model, thermal, noise, sampling, transport topology and
observer metadata outside private field values. Do not pool simulator outputs
with physical results. Do not claim compression relative to a specific codec
unless `audio_reference_bytes` measures that codec under the identical
utterance and capture settings.
