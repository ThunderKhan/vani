# M4 Implementation — Resilient Semantic Delivery

Status: **implementation substantially present; physical M4 evidence gate intentionally open.**

## Scope implemented

### Protocol
- transport-independent M4 semantic bundle model;
- bounded canonical binary codec;
- independent reference codec for cross-validation;
- deterministic provisional test vector;
- major/minor version handling;
- unknown critical-extension rejection;
- strict field, fragment, frame, hop, copy and queue bounds.

### Framing and fragmentation
- bounded binary frame envelope;
- frame CRC for transport corruption detection;
- authenticated routing metadata using HMAC-SHA-256;
- deterministic message identity across fragments/retries/hops;
- bounded fragmentation;
- out-of-order reassembly;
- duplicate-fragment suppression;
- conflicting fragment metadata rejection;
- incomplete reassembly expiry support.

### Persistence
- SQLite-backed durable M4 outbox;
- durable inbox state;
- durable seen-message replay state;
- durable fragments;
- bounded queue item/byte budgets;
- deterministic priority/expiry eviction;
- restart recovery of in-flight outbox records.

M1's SharedPreferences delivery store remains unchanged. M4 introduces its own durable persistence boundary rather than silently changing the M1 implementation.

### Security
- AES-GCM authenticated encryption for the protected semantic payload;
- Android Keystore key provider for device-local key material;
- HMAC-SHA-256 for relay routing metadata;
- authentication is checked before delivery/relay decisions that depend on the protected frame;
- no custom cryptography.

The repository still needs an explicit peer/group key provisioning and rotation protocol before claiming production-ready end-to-end key management. The current cryptographic layer is an established-primitive integration boundary, not a claim that a complete trust-management system has been physically validated.

### Routing
- routing interface;
- controlled-flood baseline with bounded hops/copies;
- binary Spray-and-Wait baseline with bounded copy splitting;
- Syntax6 priority-aware policy skeleton;
- expiry, duplicate, queue, battery and copy-budget decisions;
- bounded retry/backoff policy.

### Delivery semantics
The M4 state vocabulary is:

CREATED → VALIDATED → QUEUED → TRANSFERRED → RELAYED → DELIVERED_DEVICE → PLAYBACK_STARTED → ACKNOWLEDGED_PERSON

with EXPIRED and FAILED as terminal/error states.

Relay acceptance is represented as RELAYED and never as destination delivery.

## Implemented vs tested vs physically validated

| Area | Implemented | Tested in code | Physically validated | Measured |
|---|---|---|---|---|
| Canonical semantic codec | yes | yes | no | no |
| Independent codec cross-check | yes | yes | no | no |
| Frame bounds/CRC | yes | yes | no | no |
| Fragmentation/reassembly | yes | yes | no | no |
| Durable outbox/inbox | yes | instrumentation tests added | no | no |
| Replay/duplicate persistence | yes | instrumentation tests added | no | no |
| AES-GCM integrity/confidentiality | yes | yes | no | no |
| Routing metadata HMAC | yes | yes | no | no |
| Controlled flood | yes | yes | no | no |
| Binary Spray-and-Wait | yes | yes | no | no |
| Retry bounds | yes | yes | no | no |
| Restart recovery | yes | instrumentation test added | no | no |
| Direct M1 loop | unchanged | existing M1 tests | intentionally deferred | no |
| Real BLE/Wi-Fi relay | adapter contract/engine boundary only | protocol-level tests | **not yet** | no |
| 3-device store-carry-forward | engine supports the model | simulated/code-level only | **not yet** | no |
| Android power/latency/network evidence | instrumentation hooks exist | partial | **not yet** | no |

## Important architectural boundary

The M4 engine operates above transport. It receives and emits opaque frames through the transport contract. Relays do not require STT/TTS and do not need plaintext semantic content to store or forward a protected bundle.

The current repository still retains M1's direct TCP feasibility transport. M4 does not claim that TCP is the final mesh transport and does not claim standardized Bluetooth Mesh.

## Known limitations before the M4 exit gate

1. Physical multi-device relay/store-carry-forward has not been run yet by design.
2. BLE/Wi-Fi adapter implementations are not being represented as complete merely because the transport abstraction exists.
3. Final canonical wire format is not frozen until the independent codec agreement remains stable after remaining protocol decisions.
4. Peer/group key provisioning, rotation, revocation and lost-device recovery are not yet a complete operational trust-management protocol.
5. Routing metrics, energy cost, delivery probability and latency distributions remain unmeasured.
6. Queue limits are engineering bounds and require workload measurement before being described as optimal.
7. Android lifecycle/background execution behavior for resilient relay service remains physically unvalidated.

## M4 evidence rule

M4 is **not complete** under the canonical roadmap until its physical relay/store-carry-forward evidence gate is exercised and reproducibly recorded. Code existence and passing unit/instrumentation tests do not close that gate.
