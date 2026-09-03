# ADR-001 — Use a Transport-Independent Semantic Bundle

## Status
Accepted.

## Context
Speech should travel over BLE, Wi-Fi, relays, and possibly a low-rate bridge without rewriting application semantics.

## Decision
Define a single versioned logical semantic bundle above transport.

## Consequences
Positive:
- common delivery semantics;
- easier simulator/hardware bridge;
- reproducible test vectors.

Negative:
- protocol design/testing work;
- strict compatibility responsibility.
