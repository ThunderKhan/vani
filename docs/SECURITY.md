# Security Architecture

## Security goals

- private content confidential from passive observers and relays;
- message integrity;
- authentic sender identity where required;
- authentic final acknowledgements;
- replay protection;
- bounded resource use;
- priority abuse resistance.

## Non-goals

- anonymity against a global observer;
- perfect metadata privacy;
- protection after a fully compromised unlocked endpoint;
- guaranteed radio availability.

## Identity

Use device/user identity material stored through Android-supported secure facilities where appropriate.

The exact identity model must support:
- peer verification;
- group authorization;
- key rotation;
- lost-device recovery strategy.

## Encryption

Use established AEAD through audited libraries. No custom cipher.

## Authentication

Priority/control messages may require signatures or authenticated role assertions.

## Replay

Persist seen message IDs or replay windows across restart.

## ACK security

Destination ACKs are authenticated and refer to exact message ID and state.

## Resource safety

Before expensive processing:
- bound frame size;
- bound fragment count;
- validate lengths;
- reject unsupported versions;
- rate-limit suspicious peers.

## Logging

Never log:
- raw keys;
- raw audio;
- private transcript by default;
- plaintext private bundles in production diagnostics.

## Security testing

- tampered ciphertext;
- forged sender;
- forged ACK;
- replay;
- duplicate amplification;
- malformed lengths;
- fragment bombing;
- forged distress priority;
- queue exhaustion.
