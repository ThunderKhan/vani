# Test Plan

## Unit tests

- normalization;
- critical span parsing;
- priority policy;
- expiry;
- delivery transitions;
- deduplication;
- queue eviction;
- ACK correlation;
- copy-budget logic.

## Property/fuzz tests

- protocol serialization round-trip;
- malformed frames;
- arbitrary Unicode;
- fragment order;
- duplicate fragments;
- extreme length fields;
- unknown fields;
- version negotiation.

## Android instrumentation

- microphone permission;
- Bluetooth permission;
- lifecycle;
- process recreation;
- background/foreground service;
- audio focus;
- model loading.

## Interoperability

- app version N ↔ N;
- N ↔ N+minor;
- different Android vendors;
- BLE ↔ Wi-Fi route changes.

## Security tests

- tamper;
- replay;
- forged ACK;
- forged distress;
- queue flood;
- fragment bomb.

## Physical tests

- two-phone offline loop;
- multi-hop;
- partition merge;
- process death;
- reconnect.

## Regression policy

Any discovered protocol or safety bug gets a regression test where practical.


## M4 code-level coverage added

- canonical semantic-bundle deterministic vector;
- independent codec cross-check;
- malformed UTF-8;
- malformed major/minor protocol versions;
- unknown critical extension;
- oversized logical bundle;
- bounded frame encoding;
- oversized frame rejection;
- frame CRC tampering;
- routing HMAC tampering;
- out-of-order fragments;
- duplicate fragments;
- missing fragments;
- conflicting/invalid fragment metadata;
- AES-GCM tampering and wrong-AAD rejection;
- durable queue eviction;
- durable expiry state;
- durable replay suppression;
- restart recovery;
- bounded retry/backoff;
- ACK correlation;
- controlled-flood hop/duplicate bounds;
- binary Spray-and-Wait copy splitting;
- direct engine reassembly/delivery;
- relay engine forwarding without destination-delivery overclaim.

## M4 physical validation status

Physical multi-phone relay, store-carry-forward, radio loss/reconnect, Android process death, lifecycle/background restrictions, power cost, latency distributions, and real BLE/Wi-Fi adapter behavior remain intentionally untested. These are M4/M5 evidence activities, not claims of completion.
