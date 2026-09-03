# ADR-002 — Distinguish Transfer, Delivery, Playback, and Acknowledgement

## Status
Accepted.

## Decision
Do not use generic “sent” as a terminal state.

States explicitly distinguish:
peer transfer, relay, destination receipt, playback start, and human acknowledgement.

## Reason
Operational communication needs truthful semantics.
