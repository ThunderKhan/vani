# Decision Log

Use this file for concise accepted decisions. Significant architecture choices should receive ADRs.

## D001 — Semantic text, not neural RF waveform
The deployable system communicates compact text-centric semantic bundles above the physical layer.

## D002 — Same-language reconstruction first
Translation is deferred until required language paths are complete.

## D003 — Transport-independent protocol
BLE/Wi-Fi/relay/bridge share one logical bundle.

## D004 — Truthful delivery states
Peer transfer, destination receipt, playback, and human acknowledgement are distinct.

## D005 — Critical fields are first-class
Numbers, negations, places, times, and related fields receive stricter policy and evaluation.

## D006 — Multi-hop is bounded
No unlimited flooding.

## D007 — Offline speech models are independently verified per language
No blanket multilingual support claim.
