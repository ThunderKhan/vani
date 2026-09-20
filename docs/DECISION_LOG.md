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


## D008 — M4 persistence is SQLite-backed
M4 uses a durable SQLite outbox/inbox/replay/fragment store. The M1 SharedPreferences feasibility store remains unchanged.

## D009 — Protected payload plus authenticated routing envelope
Private semantic content is protected with AES-GCM. Minimum routing metadata is carried outside the protected payload and authenticated with HMAC-SHA-256 when a relay-domain key is configured.

## D010 — Two independent codecs before wire-format freeze
The canonical binary codec and an independently implemented reference codec must continue to produce identical bytes before the protocol byte layout is declared frozen.

## D011 — Routing is bounded by policy
Controlled flooding and binary Spray-and-Wait are implemented as bounded routing policies. Hop count, copy budget, expiry, queue limits, duplicate suppression and retry bounds are mandatory policy inputs.

## D012 — Relay state is not destination delivery
A relay can transition a message to RELAYED/forwarded state, but only destination-authenticated receipt can produce DELIVERED_DEVICE, followed by playback and human acknowledgement states.
