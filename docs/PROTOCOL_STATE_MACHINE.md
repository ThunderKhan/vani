# Protocol and Delivery State Machines

## Sender

```text
DRAFT
 -> RECOGNIZING
 -> NEEDS_CONFIRMATION?
 -> VALIDATED
 -> QUEUED
 -> TRANSFERRING
 -> TRANSFERRED
 -> DELIVERED_DEVICE
 -> PLAYBACK_STARTED
 -> ACKNOWLEDGED_PERSON
```

Terminal:
`CANCELLED`, `EXPIRED`, `FAILED`.

Rules:
- `TRANSFERRED` means a peer accepted bytes, not the destination.
- `DELIVERED_DEVICE` requires destination-authenticated receipt.
- `PLAYBACK_STARTED` requires destination playback event.
- `ACKNOWLEDGED_PERSON` requires explicit human acknowledgement.
- expiry can terminate any queued/relay stage.

## Receiver

```text
FRAME_RECEIVED
 -> REASSEMBLED
 -> VERIFIED
 -> DEDUP_CHECK
 -> PERSISTED
 -> DELIVERED_DEVICE
 -> TTS_READY
 -> PLAYBACK_STARTED
 -> USER_ACK?
```

## Relay

```text
RECEIVED
 -> BOUNDS_CHECKED
 -> VERIFIED_AS_RELAYABLE
 -> DUPLICATE_CHECK
 -> QUEUED
 -> FORWARDED
 -> RETAIN_OR_DELETE
```

Relays never emit destination delivery ACKs.

## Idempotence

Processing the same valid bundle multiple times must not:
- replay TTS;
- duplicate inbox entries;
- generate duplicate human ACKs;
- multiply relay copies beyond policy.
