# Syntax6 MeshVoice — SIH 2026 / SIH26173

> **Offline multilingual semantic voice communication for infrastructure-denied environments.**

Syntax6 MeshVoice converts spoken language into compact, safety-aware semantic bundles on-device, carries those bundles across direct or intermittently connected local links, and reconstructs intelligible speech at the destination without cloud infrastructure.

## Required language coverage

Hindi, Gujarati, Marathi, Kannada, Malayalam, Tamil, Telugu, Odia, Bengali, and English.

## Core pipeline

```text
Speech
 -> VAD + endpointing
 -> offline STT
 -> normalization + critical-information validation
 -> compact authenticated semantic bundle
 -> BLE / local Wi-Fi / relay / store-carry-forward
 -> verification + deduplication + delivery state
 -> offline TTS
 -> playback + acknowledgement
```

## Documentation map

| Area | Primary documents |
|---|---|
| Product | `docs/PRD.md`, `docs/MVP.md`, `docs/USER_STORIES.md`, `docs/UX_SPEC.md` |
| System | `docs/ARCHITECTURE.md`, `docs/COMPONENTS.md`, `docs/DATA_FLOW.md` |
| Protocol | `docs/PROTOCOL.md`, `docs/PROTOCOL_STATE_MACHINE.md`, `docs/FRAGMENTATION.md` |
| Speech/ML | `docs/SPEECH_STACK.md`, `docs/MODEL_SELECTION.md`, `docs/MODEL_EVALUATION.md` |
| Networking | `docs/NETWORKING.md`, `docs/ROUTING_RESEARCH.md`, `docs/TRANSPORT_ADAPTERS.md` |
| Safety/Security | `docs/CRITICAL_INFORMATION_SAFETY.md`, `docs/SECURITY.md`, `docs/PRIVACY.md`, `docs/THREAT_MODEL.md` |
| Research | `docs/RESEARCH.md`, `docs/EVALUATION.md`, `docs/EXPERIMENTS.md`, `docs/PUBLICATION_PLAN.md` |
| Execution | **`docs/ROADMAP.md` (canonical M0–M6 roadmap)**, `docs/MILESTONES.md` (quick reference), `docs/TASK_BREAKDOWN.md`, `docs/RISK_REGISTER.md` |
| Validation | `docs/TEST_PLAN.md`, `docs/BENCHMARK_PLAN.md`, `docs/DEFINITION_OF_DONE.md` |
| Competition | `docs/DEMO.md`, `docs/JUDGE_QA.md`, `docs/PITCH.md` |
| Governance | `docs/CONTRIBUTING.md`, `docs/DECISION_LOG.md`, `docs/LICENSING.md` |

## Roadmap convention

Implementation follows exactly one sequential engineering roadmap:

```text
M0 Truth and Feasibility
 -> M1 Minimum Complete Offline Loop
 -> M2 Ten-Language Credibility
 -> M3 Safety-Aware Messaging
 -> M4 Resilient Semantic Delivery
 -> M5 Scientific Evidence
 -> M6 Competition-Ready Product
```

Older `Phase 0`–`Phase 6` wording refers to the same M0–M6 sequence and should not be treated as a separate roadmap.

`Tier 1`, `Tier 2`, and `Tier 3` describe **product/research maturity**, not implementation order. See `docs/ROADMAP.md` for the authoritative mapping and exit gates.

## Non-negotiable principles

1. **Offline really means offline.**
2. **All ten languages are individually verified.**
3. **Critical meaning matters more than average transcript quality.**
4. **Delivery state must be truthful.**
5. **Transport is replaceable; the semantic bundle is stable.**
6. **No invented cryptography.**
7. **Claims must trace to reproducible measurements.**
8. **Low/mid-range Android devices are first-class targets.**
9. **Mesh behavior is measured, never hand-waved.**
10. **The final product is a communications instrument, not a chat-app reskin.**

## Source-of-truth order

1. Latest official SIH statement and organizer clarifications.
2. Reproducible measurements from the implementation.
3. Official platform docs, model cards, licences, standards, and primary papers.
4. Accepted repository specifications and ADRs.
5. Planning/context documents.
6. Secondary sources.

## First engineering objective

Prove one complete, instrumented, fully offline vertical slice:

**PTT speech -> local STT -> validated semantic bundle -> direct local transport -> local TTS -> acknowledgement**

Only after that path is reliable should the team add multi-hop routing, advanced safety policies, model adaptation, and optional radio bridges.
