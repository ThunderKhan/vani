# M4 Protocol Test Vectors

Status: **provisional implementation vectors; canonical byte-level protocol is not frozen yet.**

The protocol contract requires two independent codecs to agree before the byte-level encoding is frozen. M4 currently contains `CanonicalBinarySemanticCodec` and `ReferenceSemanticCodec`, and both are cross-checked by `M4ProtocolTest`.

## Vector 001 — multilingual semantic bundle

Logical values:

| Field | Value |
|---|---|
| message_id | 00000000-0000-0000-0000-000000000001 |
| conversation_id | incident-1 |
| source_id | alpha |
| destination_id | bravo |
| language | hi-IN |
| priority | URGENT |
| message_type | TEXT |
| created_at_epoch_ms | 1700000000000 |
| expires_after_ms | 30000 |
| hop_limit | 8 |
| copy_budget | 4 |
| ack_policy | PLAYBACK |
| safety_action | CONFIRM |
| transcript | नमस्ते |
| critical_fields | none |
| extensions | none |

Canonical bytes:

`56414e34010001002430303030303030302d303030302d303030302d3030303030303030303030310a696e636964656e742d3105616c70686105627261766f0568692d494e0000018bcfe568000000000000007530080402010012e0a4a8e0a4aee0a4b8e0a58de0a4a4e0a5870000`

Expected length: `116` bytes.

## Required negative vectors

The test suite also covers malformed UTF-8, unsupported major version, oversized logical bundles, unknown critical extensions, oversized frames, frame CRC tampering, routing metadata authentication tampering, invalid fragment bounds, duplicate/out-of-order/missing fragments, replay/duplicate delivery, AES-GCM authentication failure, and ACK state corruption.

## Freeze rule

Do not treat Vector 001 as a final wire-format freeze until the independent codecs continue to agree after the remaining M4 protocol fields and interoperability decisions are settled.
