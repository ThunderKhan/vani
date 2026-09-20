# Syntax6 Semantic Bundle Protocol — Design Contract

> Status: **v1 design specification. Byte-level encoding must be frozen only after implementation test vectors pass across at least two independent codecs.**

## 1. Purpose

Provide a compact, transport-independent representation of an offline voice-derived message with explicit safety, delivery, expiry, and authentication semantics.

## 2. Design requirements

- binary production encoding;
- deterministic canonical serialization;
- Unicode-safe;
- bounded;
- fragmentable;
- authenticated;
- replay-aware;
- transport-independent;
- forward-compatible where safe;
- usable on low-rate links.

CBOR is a leading candidate because compact canonical representations and mature implementations exist, but final codec choice must be licence/runtime audited.

## 3. Logical envelope

| Field | Required | Notes |
|---|---:|---|
| protocol_version | yes | integer major/minor policy |
| message_id | yes | globally collision-resistant identifier |
| conversation_id | optional | incident/group/thread |
| source_id | yes | authenticated sender/pseudonymous identity |
| destination | yes | peer or group |
| created_age_basis | yes | monotonic-relative semantics where possible |
| expiry | yes | bounded lifetime |
| hop_limit | yes | relay bound |
| language_code | yes | BCP-47-like controlled project enum |
| priority | yes | routine/urgent/distress with authorization policy |
| message_type | yes | text-speech, ACK, control, capability |
| transcript | conditional | present for semantic speech message |
| critical_fields | optional | typed safety metadata |
| confidence_summary | optional | explicitly non-calibrated unless proven |
| ack_policy | yes | none/device/person/played |
| copy_budget | optional | routing mode dependent |
| fragment_info | conditional | transport framing metadata |
| payload_hash | yes | exact logical payload digest |
| security_data | yes | authenticated encryption/signature metadata |

## 4. Message identity

`message_id` identifies the logical message independent of:
- fragments;
- hops;
- transports;
- retries;
- relay copies.

Retries must never mint a new ID.

## 5. Priority

Suggested enum:
- `0 ROUTINE`
- `1 URGENT`
- `2 DISTRESS`

`DISTRESS` may require authenticated authorization or strict anti-abuse policy. UI selection alone must not guarantee privileged forwarding.

## 6. ACK policy

- `NONE`
- `DEVICE_RECEIVED`
- `PLAYBACK_STARTED`
- `PERSON_ACKNOWLEDGED`

ACKs identify the target `message_id`, state, receiver identity, and authentication proof.

## 7. Delivery states

```text
CREATED
VALIDATED
QUEUED
TRANSFERRED
RELAYED
DELIVERED_DEVICE
PLAYBACK_STARTED
ACKNOWLEDGED_PERSON
EXPIRED
FAILED
```

State transitions are monotonic except local retry substate.

## 8. Expiry

Wall clocks cannot be assumed synchronized. The implementation should encode sufficient lifetime information so each hop can decrement or validate age robustly. Exact mechanism must be finalized in the byte-level spec.

## 9. Unknown fields

Unknown optional fields should be ignored only if they do not change safety/security interpretation. Unknown critical extension markers cause safe rejection.

## 10. Bounds

Initial proposed limits, subject to measurement:
- transcript: 4 KiB UTF-8;
- critical-field count: 32;
- maximum logical bundle: 8 KiB before fragmentation;
- hop limit: <= 16;
- fragment count: <= 128;
- retry count: bounded by transport policy.

These are proposals, not measured final values.

## 11. Canonical encoding

The codec must define:
- map key IDs/order;
- integer width rules;
- Unicode normalization;
- boolean/null representation;
- extension namespace;
- canonical signature bytes.

## 12. Security envelope

Do not design new cryptography. Use established AEAD and signature primitives through vetted libraries.

Private messages:
- encrypt payload end-to-end;
- authenticate associated routing metadata as appropriate;
- relays see only minimum routing metadata.

## 13. Test vectors

Every protocol release must include:
- minimal message;
- multilingual Unicode message;
- maximum legal field sizes;
- critical-field example;
- ACK example;
- fragmentation example;
- malformed/oversized cases;
- tampered authentication example;
- unknown-version example.

## 14. Compatibility

Major version mismatch: reject unless explicit compatibility layer exists.  
Minor version: accept compatible optional extensions by policy.

## 15. Non-goals

- custom physical-layer framing;
- novel cryptography;
- preserving original voice waveform;
- guaranteed delivery.


## 11.1 M4 implementation status

M4 now has a bounded binary candidate implementation in the m4 SemanticCodec source plus an independently implemented reference codec in ReferenceSemanticCodec.

The current candidate uses:
- fixed field order rather than a map;
- big-endian integer encoding;
- UTF-8 strings with u8/u16 byte lengths;
- explicit major/minor version bytes;
- bounded critical-field and extension counts;
- sorted non-critical extensions by numeric extension ID;
- explicit rejection of unknown critical extensions.

This is **not yet a final protocol freeze**. The two codecs are cross-checked by tests and a deterministic vector is stored under docs/test-vectors/, but the repository's two-independent-codec freeze rule remains in force.

The protected semantic payload uses AES-GCM. Relay routing metadata is separately authenticated with HMAC-SHA-256. These are established primitives; no custom cryptography is introduced.

## 11.2 M4 frame

The M4 frame is a bounded transport envelope around a fragment. It carries only the metadata needed for forwarding and validation:
- frame type;
- logical message ID;
- destination ID;
- priority;
- absolute expiry timestamp;
- authenticated routing tag;
- declared total protected payload size;
- hop limit;
- copy budget;
- fragment index/count;
- fragment payload;
- CRC32 for transport-corruption detection.

The CRC is not a security primitive. End-to-end AES-GCM authentication remains authoritative for private content.

## 11.3 M4 persistence

M4 persistence is SQLite-backed and separate from the M1 SharedPreferences feasibility store. It persists:
- outbox records;
- inbox delivery state;
- seen message IDs;
- incomplete fragments.

Queue count and byte budgets are enforced before insertion. Expired records are transitioned to EXPIRED; in-flight records are recovered to QUEUED after restart when their expiry has not passed.

## 11.4 Expiry and clock model

The current implementation carries an absolute creation time plus bounded lifetime and derives an expiry timestamp for routing/persistence. Physical clock-skew behavior is still an M4 evidence item and is not claimed solved for arbitrary unsynchronized devices.

## 11.5 Security boundary

Relays operate on protected payload bytes. They may inspect minimum routing metadata required to forward a message, but they do not invoke STT/TTS or decode private transcript content. Destination delivery requires successful end-to-end authentication and destination matching.
