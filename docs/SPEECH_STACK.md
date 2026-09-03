# Speech Stack

## Goal

Run all speech processing locally on Android while balancing accuracy, latency, memory, storage, and energy across ten languages.

## Pipeline

```text
PCM capture
 -> VAD
 -> endpointing
 -> ASR
 -> text normalization
 -> confidence/critical span analysis
 -> transport
 -> TTS
 -> audio playback
```

## Audio

Default proposal:
- mono PCM;
- 16 kHz where model-compatible;
- monotonic timestamps;
- bounded ring buffer.

## VAD

Requirements:
- cheap idle path;
- stable in common environmental noise;
- configurable speech-start and speech-end thresholds;
- measurement harness.

Metrics:
- false positive rate;
- missed speech;
- clipping;
- endpoint latency;
- idle CPU/energy.

## ASR architecture candidates

- CTC;
- streaming Conformer/FastConformer;
- RNN-T;
- compact multilingual encoder-decoder;
- per-language compact models.

Candidate families must be verified, not assumed:
- AI4Bharat Indic speech models;
- MMS;
- Whisper derivatives;
- Vakyansh;
- other licence-compatible open models.

## TTS candidates

- VITS variants;
- FastSpeech2/FastPitch + compact vocoder;
- language-specific compact voices;
- shared multilingual architectures.

## Model loading

Do not load all ten stacks simultaneously. Use:
- installable offline language packs;
- one active language model set;
- preflight pack validation;
- checksums;
- device-aware profile.

## Runtime candidates

Benchmark:
- LiteRT/TFLite;
- ONNX Runtime Mobile;
- ExecuTorch;
- native optimized runtimes if open and suitable.

## Required speech metrics

ASR:
- WER;
- CER;
- critical field accuracy;
- cold/warm latency;
- RTF;
- RAM.

TTS:
- time to first audio;
- RTF;
- intelligibility;
- critical pronunciation;
- RAM;
- size.
