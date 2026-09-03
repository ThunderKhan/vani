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
