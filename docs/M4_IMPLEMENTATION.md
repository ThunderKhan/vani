# M4 Implementation Status

Status: code implementation added; physical validation intentionally deferred.

M4 adds a transport-independent semantic delivery layer beside the existing M1/M3 feasibility path.

Implemented:
- bounded deterministic semantic bundle codec behind an interface;
- experimental byte-level test vector plus independent Python reference encoder;
- bounded framing and deterministic fragmentation/reassembly;
- duplicate and malformed-fragment rejection;
- persistent outbox/inbox/relay records using SQLite;
- persistent seen-message state and fragment recovery;
- expiry, retry, queue-size, message-count and per-origin limits;
- priority-aware eviction;
- authenticated protected-message boundary using an established platform primitive;
- content-blind relay envelope;
- controlled-flood and binary Spray-and-Wait routing policies;
- hop and copy limits;
- explicit delivery-state machine and ACK correlation;
- restart recovery tests.

The experimental codec is not frozen. The repository keeps the codec behind an abstraction and requires independent codec agreement before treating the byte layout as final.

The relay layer does not require STT/TTS and does not treat relay acceptance as destination delivery.

The existing M1 direct TCP path and M3 safety guard remain unchanged. M4 is not physically validated yet, and no three-device relay/store-forward result is claimed.

## Validation classification

Implemented = code exists.

Tested in code = deterministic unit/instrumentation tests exist and are runnable.

Physically validated = deliberately still pending.

Measured = no M4 physical performance numbers have been recorded yet.

## October integration (research branch)

The integration branch adds `M4LocalWifiTransport` (bounded frame transport with only
adjacent-peer receipts), `M4DirectActivity` (manually supplied shared key,
authenticated binary messaging, receiver-side platform TTS, one-hour retained
outbox with user-triggered retry), `AuthenticatedRelayEnvelopeCodec` (HMAC
routing-domain wrapper), atomically stored seen markers and inbox records, and
bounded in-memory reassembly. `M4DeliveryEngine` now requires an explicitly
provisioned `MessageProtector` and checks source/destination identity.

**Not implemented:** actual BLE scanning/GATT transport, automatic relay
scheduler, physical third-phone routing, signed end-recipient receipts, human
ACK workflow, production key exchange, and a final app-scope replacement for
M1 JSON/TCP. No successful device test or authenticated mesh claim is made.
