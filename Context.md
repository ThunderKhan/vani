# Team Syntax6 — Engineering Context

This repository implements the SIH26173 offline multilingual voice-transceiver problem as a cross-layer system joining edge speech inference, critical-information safety, compact semantic transport, local networking, delay-tolerant relaying, and constrained Android deployment.

## Governing thesis

A user should still be able to communicate an actionable voice message when ordinary internet and voice infrastructure fail, provided some local device-to-device connectivity exists.

The system therefore:
- captures speech locally;
- recognizes it locally;
- represents meaning as a compact text-centric bundle;
- protects critical fields and uncertainty;
- routes the bundle across available local paths;
- reconstructs intelligible speech locally;
- records truthful delivery and acknowledgement states.

## What this project is not

- not a cloud speech wrapper;
- not a generic offline messenger;
- not a BitChat clone;
- not a custom RF physical layer;
- not a translation-first product;
- not a benchmark-only research repo;
- not a demo that works only on a flagship phone.

## Required languages

Hindi, Gujarati, Marathi, Kannada, Malayalam, Tamil, Telugu, Odia, Bengali, English.

## Engineering test for every feature

For any implementation proposal answer:
1. Which requirement or field failure does it address?
2. How will we prove it works?
3. What does it cost in latency, RAM, storage, CPU, energy, radio overhead, or complexity?
4. Does it work offline?
5. What happens when it fails?
6. Can the team explain it under judge scrutiny?

## Safety doctrine

Numbers, negations, coordinates, places, times, quantities, identities, and emergency terms must never be treated like ordinary tokens. When uncertainty is materially high, the system should expose that uncertainty and prefer confirmation, retransmission, spelling, or receiver warning over fabricated certainty.

## Delivery doctrine

The product distinguishes:
`created -> validated -> queued -> transferred -> relayed -> delivered -> played -> acknowledged`

Intermediate states must never be presented as final human receipt.
