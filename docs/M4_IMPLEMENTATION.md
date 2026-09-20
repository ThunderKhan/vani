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
