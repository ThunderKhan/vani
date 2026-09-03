# Privacy Design

## Principles

- process raw speech locally;
- transmit semantic text only by default;
- minimize retained data;
- relay private messages without plaintext access;
- make diagnostics opt-in and redactable;
- separate experiment data from production data.

## Default retention

Proposed:
- raw microphone audio: not retained;
- sent/received transcript: user-visible history subject to deletion settings;
- relay ciphertext: delete on ACK/expiry/storage policy;
- experiment traces: content-free identifiers by default.

## Human research

Any field dataset/listener study requires:
- informed consent;
- clear use statement;
- retention policy;
- de-identification;
- no reuse outside scope without permission.
