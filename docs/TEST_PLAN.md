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
