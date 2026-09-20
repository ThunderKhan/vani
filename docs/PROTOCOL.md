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


## 16. M4 implementation decisions

The repository now contains an experimental byte-level implementation behind the SemanticBundleCodec interface.

- Current experimental codec: deterministic fixed-order binary fields, UTF-8 strings, explicit major/minor version bytes.
- Current bounds: 4 KiB transcript, 32 critical fields, 8 KiB logical bundle, 16 hops, 128 fragments, bounded retries/copies.
- Lifetime is represented with sender creation and expiry epoch milliseconds. Physical clock-skew behavior remains an evidence item; relays additionally carry a bounded remaining-hop/copy envelope.
- Framing uses a bounded binary frame with message UUID, fragment index/count, total logical length, payload length and CRC32.
- The logical bundle is authenticated/encrypted separately from relay metadata. AES-GCM is used through the platform cryptographic provider; no custom cipher is introduced.
- Relay forwarding operates on opaque protected payloads and bounded routing metadata. Relays do not need speech models or plaintext access.
- The codec is explicitly experimental/not frozen. A Python reference encoder and Kotlin implementation share a deterministic test vector, but the protocol is not considered byte-level frozen until independent codec agreement satisfies the specification.
- Unknown major versions and unknown critical enum values are rejected safely.

The M4 implementation therefore makes the protocol concrete enough for deterministic testing without silently declaring the final wire format frozen.
