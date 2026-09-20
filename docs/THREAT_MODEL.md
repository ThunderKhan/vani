# Threat Model

## Adversaries

### Passive observer
Can hear BLE/Wi-Fi radio traffic.

### Malicious peer
Participates in discovery and sends arbitrary frames.

### Malicious relay
Receives bundles it may be asked to forward.

### Compromised endpoint
Has app/device access.

### Resource attacker
Attempts battery drain, queue exhaustion, or fragment abuse.

## Assets

- message plaintext;
- critical information;
- identity;
- acknowledgement truth;
- priority integrity;
- key material;
- availability;
- experiment integrity.

## Threats

- eavesdropping;
- traffic analysis;
- modification;
- replay;
- sender impersonation;
- forged ACK;
- priority escalation;
- Sybil peers;
- queue starvation;
- oversized bundles;
- fragment explosion;
- forced inference/audio playback;
- log injection;
- stale alert replay.

## Mitigations

- end-to-end authenticated encryption;
- signed/authenticated control messages;
- strict bounds;
- expiry;
- replay state;
- quotas;
- rate limits;
- user-controlled relay resource ceilings;
- safe UI rendering;
- no automatic execution of received text.

## Residual risks

- metadata leakage;
- device compromise;
- RF jamming/interference;
- OS restrictions;
- social engineering;
- incorrect but high-confidence ASR output.


## M4 concrete mitigations

### Oversized input / fragment bombing
The frame, bundle, extension, critical-field, fragment-count, total-size and queue limits are checked before large allocations. Reassembly is bounded to the configured maximum logical bundle size.

### Malicious relay
The relay stores protected payload bytes rather than invoking speech processing. It cannot turn receipt of an opaque bundle into destination delivery state.

### Replay
Message IDs are persisted in the M4 seen table. Fragment identities are persisted separately. Restart does not clear replay state.

### Routing metadata tampering
The M4 frame carries an HMAC-SHA-256 routing tag when the relay-domain key is configured. Tampering with destination, priority, expiry, hop or copy metadata therefore fails verification.

### Resource exhaustion
Queue item/byte budgets, bounded copies, bounded hops, bounded retries, expiry and deterministic eviction prevent a single peer from causing unbounded retained forwarding state.

### Residual security risks
The current M4 implementation does not yet solve peer enrollment, group-key distribution, key rotation/revocation, Sybil resistance, metadata confidentiality against a traffic analyst, or compromise of an unlocked endpoint. These remain explicit limitations rather than hidden assumptions.
