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


## D008 — M4 codec remains experimental until independent agreement
A deterministic compact binary codec is implemented behind an abstraction with a Kotlin test vector and independent Python reference encoder. The byte-level format is not frozen until independent codec agreement passes.

## D009 — SQLite replaces M1 SharedPreferences for durable delivery
M1's SharedPreferences duplicate marker remains part of the disposable feasibility path. M4 uses a transactional SQLite delivery store for outbox/inbox/relay records, replay state, and fragments.

## D010 — Relay metadata is separated from protected content
Relays operate on an opaque protected bundle plus bounded routing metadata. They do not need STT/TTS or plaintext semantic content to forward a message.

## D011 — Routing is policy-driven and bounded
Controlled flooding and binary Spray-and-Wait are explicit routing policies with hop, copy, expiry, queue, duplicate and priority constraints. No unlimited flood is permitted.
