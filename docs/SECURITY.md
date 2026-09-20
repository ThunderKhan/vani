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


## M4 implementation

M4 uses:
- AES-GCM for end-to-end authenticated encryption of the semantic bundle;
- Android Keystore for device-local long-lived AES key material where the provider is used;
- HMAC-SHA-256 for relay routing metadata authentication;
- SHA/CRC checks only as non-security integrity diagnostics where appropriate.

Android guidance recommends Android Keystore for long-term key storage and established AES/GCM primitives rather than custom cryptography. The implementation follows that boundary. citeturn1search0

### Key-management limitation

The M4 cryptographic primitives are implemented, but peer/group provisioning, rotation, revocation, and lost-device recovery are not yet a complete operational trust-management protocol. Test keys are explicitly available for deterministic code-level tests and must not be treated as production credentials.

### Relay confidentiality

Relays receive protected payload bytes and the minimum routing metadata needed for forwarding. They do not receive plaintext transcript content from the M4 engine.

### Replay and duplicate handling

Seen message IDs persist in SQLite across process restart. Fragment identities are also persisted so duplicate fragments can be suppressed without allocating another reassembly buffer.

### Priority integrity

Priority and expiry are carried in the routing envelope and authenticated by HMAC-SHA-256 when a shared relay-domain authentication key is configured. The final destination still validates the protected semantic bundle before treating priority as authoritative.
