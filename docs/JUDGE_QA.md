# Judge Q&A

## Why not transmit compressed audio?

We benchmark against compressed audio on the same link. Semantic text can be dramatically smaller for many messages, but the system loses speaker identity/emotion and depends on speech recognition accuracy. We present both benefits and limitations.

## Is this just BitChat plus STT?

No. We independently specify the semantic bundle and focus on multilingual edge speech, critical-information preservation, uncertainty-aware policy, measurable delivery semantics, and cross-layer evaluation. BitChat-like relaying is one architectural reference/baseline.

## Does mesh increase range?

Only when intermediate contacts exist. We do not claim magic range. Multi-hop and store-carry-forward are measured under defined topologies.

## Why ten languages?

They are required by the problem. Each must be verified individually; “multilingual model” is not sufficient evidence.

## What if ASR gets a dangerous number wrong?

Critical numbers/negations/locations receive special detection and confirmation. The product exposes uncertainty rather than silently inventing certainty.

## Is it fully offline?

The judged path is tested in airplane mode with internet blocked. All STT/TTS and delivery processing are local.

## How do you know a message arrived?

We distinguish transferred, relayed, destination-delivered, playback-started, and human-acknowledged states.

## Why not translation?

Same-language reconstruction is the core requirement. Translation is useful but would add error and scope before the mandatory path is complete.

## Security?

Private content is designed for end-to-end authenticated encryption; relays are content-blind. We use established cryptographic libraries rather than custom crypto.

## Publication novelty?

The strongest research claim is cross-layer: critical semantic risk influences confirmation, redundancy, acknowledgement, and bounded routing, evaluated against speech/audio/network baselines.
