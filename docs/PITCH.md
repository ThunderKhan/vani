# Competition Pitch

## One-line

**MeshVoice lets people speak across infrastructure failure by converting local speech into tiny safety-aware semantic bundles that can move directly or through nearby devices and become speech again at the destination.**

## Problem

Voice calls fail when networks fail. Raw audio is expensive. Typing is not always possible. Cloud speech is unavailable exactly when infrastructure is gone.

## Insight

For many operational messages, the actionable meaning is much smaller than the waveform.

## Solution

Offline STT -> critical-information validation -> compact authenticated bundle -> local/direct/relay transport -> offline TTS.

## Why it is more than STT + TTS

- ten-language edge deployment;
- critical-information safety;
- truthful delivery states;
- transport-independent protocol;
- delay-tolerant relaying;
- device-aware model selection;
- measured end-to-end success.

## Scientific differentiator

We optimize **correctly understood critical meaning per byte and per unit energy**, not merely WER.

## Demo proof

Two phones, airplane mode, real speech, real local transfer, local TTS, real acknowledgement, measured latency.
