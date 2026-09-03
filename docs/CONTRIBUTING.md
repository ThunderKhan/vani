# Contributing

## Before coding

- read `Context.md`;
- read relevant spec;
- inspect existing interfaces;
- identify acceptance test;
- avoid broad refactors without need.

## Commit discipline

Prefer small vertical commits:
- `feat(protocol): ...`
- `feat(asr): ...`
- `test(delivery): ...`
- `docs(research): ...`
- `fix(ble): ...`

## Required for PR/merge

- tests;
- failure-path coverage;
- no sensitive debug logs;
- documentation updated;
- dependency licence checked;
- performance claim backed by experiment record.

## No-go changes

- cloud dependency in judged path;
- hard-coded fabricated metrics;
- unsafe Android policy bypass;
- unbounded queue/retry;
- custom cryptographic primitive;
- silent protocol semantic change.
