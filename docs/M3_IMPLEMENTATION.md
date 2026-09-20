# M3 Implementation — Safety-Aware Messaging

Status: implementation started; physical validation intentionally deferred.

## Implemented

- deterministic recognizers for negation, numbers, coordinates, time, quantities, location-like phrases, and emergency terms;
- Unicode NFC normalization while retaining the raw transcript;
- explicit safety actions SEND, WARN, and CONFIRM;
- sender confirmation before transmission when policy triggers;
- critical-field metadata carried in the semantic bundle;
- strict validation of field count, spans, and confidence bounds;
- receiver state distinguishes safety-bearing messages;
- unit tests for English/Hindi critical cases and bundle round-trip;
- no LLM-based repair of critical values.

## Important limitation

The recognizers are deterministic heuristics, not learned semantic understanding. Detector confidence values are policy priors and must not be presented as calibrated ASR confidence. False positives and false negatives require measurement.

M3 establishes the safety contract and implementation boundary first. Later evidence work must measure detector precision/recall, critical-field accuracy, end-to-end message success, and latency overhead.

## Safety policy

Risky content is not silently rewritten. The sender sees the exact transcript and must explicitly confirm it before transmission. The receiver receives the same transcript plus structured critical-field metadata.

This is a safeguard, not proof that the message is correct.

## Physical validation

Physical testing is intentionally deferred. Code-level deterministic tests and implementation can advance without claiming that the M3 physical exit gate has passed.

## Next milestone

M4 adds durable bounded delivery, fragmentation/reassembly, replay protection, authenticated integrity, relay/store-carry-forward, routing baselines, and restart recovery.
