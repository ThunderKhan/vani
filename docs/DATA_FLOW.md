# Data Flow and Trust Boundaries

## Data classes

### Raw audio
Sensitive. Exists on sender during capture/inference. Not transmitted in normal semantic mode. Not persisted by default.

### Transcript
Sensitive plaintext. Used locally for confirmation and TTS at destination. Encrypted in transit for private messages.

### Critical fields
Structured metadata such as number, time, location, or negation. Consider equally sensitive.

### Routing metadata
May expose source/destination pseudonyms, expiry, priority, and traffic patterns. Minimize exposure.

### Diagnostics
May unintentionally reveal content or identities. Production logs must redact sensitive payloads.

## Trust boundaries

```text
Microphone -> local app (trusted process)
App plaintext -> crypto envelope (sensitive boundary)
Encrypted bundle -> transport/relay (untrusted network)
Receiver transport -> verification (untrusted input)
Verified plaintext -> TTS/UI (trusted rendering boundary)
```

## Validation order for incoming data

1. frame size;
2. protocol framing;
3. version;
4. fragment bounds;
5. authentication/integrity;
6. replay/duplicate state;
7. destination/authorization;
8. expiry;
9. content decoding;
10. presentation.

Never allocate based on attacker-controlled length before bounds checking.
