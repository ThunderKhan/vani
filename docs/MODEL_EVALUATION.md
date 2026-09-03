# Model Evaluation Protocol

## Language matrix

Every candidate must receive an explicit row for:
Hindi, Gujarati, Marathi, Kannada, Malayalam, Tamil, Telugu, Odia, Bengali, English.

No “multilingual = supported” shortcut.

## ASR evaluation

Report:
- raw WER;
- normalized WER;
- CER;
- number accuracy;
- negation accuracy;
- location/entity accuracy;
- critical field accuracy;
- message-level semantic success;
- cold/warm latency;
- RTF;
- peak RAM.

## TTS evaluation

Report:
- human transcription intelligibility;
- pronunciation failure labels;
- MOS/naturalness where feasible;
- time to first audio;
- total synthesis time;
- RTF;
- peak RAM;
- model size.

## Dataset hygiene

- speaker-disjoint test where possible;
- no threshold tuning on final test set;
- record normalization version;
- document dataset licence/version;
- identify code-switching;
- preserve per-language results.

## Statistics

- bootstrap confidence intervals;
- paired utterance comparisons;
- macro-average plus per-language;
- repeated device runs;
- randomized listener order.
