# Milestones — Quick Reference

> **Do not use this file as an independent roadmap.**
>
> The authoritative milestone definitions, deliverables, exit criteria, evidence requirements, and Tier mapping live in [`ROADMAP.md`](./ROADMAP.md).

VĀṆI uses exactly one implementation sequence:

```text
M0 -> M1 -> M2 -> M3 -> M4 -> M5 -> M6
```

Older references to `Phase 0` through `Phase 6` mean the corresponding `M0` through `M6` milestone.

## M0 — Truth and Feasibility

**Question answered:** Is the official problem technically and legally feasible on real Android hardware?

**Exit gate:** ten-language candidates/licence risks are known, offline STT/TTS has run on representative Android hardware, two phones exchange local Unicode data, and feasibility measurements are recorded.

## M1 — Minimum Complete Offline Loop

**Question answered:** Does the fundamental VĀṆI thesis work end to end?

**Exit gate:** two physical phones complete PTT → offline STT → semantic bundle → direct local transport → offline TTS → ACK with internet disabled, truthful delivery states, timing instrumentation, and basic failure handling.

## M2 — Ten-Language Credibility

**Question answered:** Can the system credibly satisfy the official ten-language requirement?

**Exit gate:** every required language has individually verified offline STT and TTS paths, provenance/licence records, per-language evaluation, and representative resource measurements.

## M3 — Safety-Aware Messaging

**Question answered:** Does the system handle critical meaning and uncertainty explicitly?

**Exit gate:** critical fields such as negations, numbers, locations, times, quantities, identities, and emergency terms have detection/metadata, safe confirmation or warning behavior, and reproducible scoring/tests.

## M4 — Resilient Semantic Delivery

**Question answered:** Can messages survive intermittent local connectivity without unsafe or ambiguous delivery semantics?

**Exit gate:** bounded canonical bundles, fragmentation, queues, expiry, replay protection, duplicate suppression, relay/store-carry-forward, restart recovery, and routing baselines are tested; a physical multi-device relay/store-forward scenario succeeds.

## M5 — Scientific Evidence

**Question answered:** Which parts of the system actually help, under what conditions, and at what cost?

**Exit gate:** speech/network/end-to-end benchmarks, baselines, ablations, resource measurements, uncertainty/critical-field metrics, routing measurements, variability/confidence reporting, and experiment provenance are reproducible.

## M6 — Competition-Ready Product

**Question answered:** Can Team Syntax6 demonstrate and defend the measured system reliably before SIH judges?

**Exit gate:** the offline live demo, accessible UX, diagnostics, failure recovery, documentation, licences/model provenance, pitch/Q&A, fallback recording, and reproducible build/run instructions are complete.

---

## Milestones vs maturity tiers

These are different axes:

| Concept | Purpose |
|---|---|
| **M0–M6** | Sequential engineering/evidence gates. |
| **Tier 1 — Credible MVP** | Product maturity reached through M0–M2. |
| **Tier 2 — Competition-grade / winning prototype** | Product maturity reached by adding M3–M5. |
| **M6 — Competition-ready** | SIH hardening, packaging, and demonstration gate. |
| **Tier 3 — Publication-grade extension** | Research maturity built primarily on M5 evidence; it may continue beyond competition hardening. |

A tier never authorizes skipping a milestone.

For the full specification, use [`docs/ROADMAP.md`](./ROADMAP.md).
