# VĀṆI Engineering Roadmap

> **Canonical execution roadmap for the repository.**
>
> This document defines the only milestone vocabulary used for implementation: **M0 through M6**. Older references to `Phase 0` through `Phase 6` mean the corresponding milestone and should be read as `M0` through `M6`.

## 1. How to read this roadmap

VĀṆI uses two different concepts that must not be confused:

### Engineering milestones — M0 to M6

Milestones are **sequential evidence gates**. They answer:

> What must be demonstrated and recorded before the project advances?

A milestone is not complete because code exists. It is complete only when its required behavior has been exercised and the evidence is reproducible.

### Product/research maturity — Tier 1 to Tier 3

Tiers describe **scope and maturity**, not an alternate implementation sequence:

- **Tier 1 — Credible MVP:** the complete offline two-phone thesis plus credible ten-language coverage.
- **Tier 2 — Competition-grade / winning prototype:** safety-aware messaging, resilient delivery, and strong measured evidence.
- **Tier 3 — Publication-grade extension:** broader experiments, stronger baselines/ablations, and defensible original research contributions.

The team always follows **M0 → M1 → M2 → M3 → M4 → M5 → M6**. Tiers are labels applied to the capability reached along that path.

## 2. Milestone completion rule

A milestone may be marked complete only when all of the following are true:

1. its required deliverables exist;
2. its exit criteria pass on the stated target environment;
3. relevant failure paths have been exercised;
4. claims are backed by reproducible measurements or test artifacts;
5. affected architecture/protocol/evaluation documentation is current;
6. unresolved risks are recorded rather than hidden.

Measured results must identify the app/git revision, device, model/runtime configuration, language or fixture, and relevant transport/test conditions.

---

# M0 — Truth and Feasibility

## Objective

Prove that the official SIH problem is technically and legally feasible before building the full product.

## Why this milestone exists

The highest-risk assumptions are ten-language model availability, licences, on-device performance, and local phone-to-phone transport. Failure in any one of these can invalidate the architecture if discovered too late.

## Required deliverables

- frozen copy/reference of the official SIH26173 requirements and known open questions;
- candidate STT/TTS matrix covering all ten required languages;
- code/model/data licence audit for shortlisted components;
- first offline ASR inference on representative Android hardware;
- first offline TTS synthesis on representative Android hardware;
- direct Unicode data transfer between two physical Android phones over at least one intended local transport;
- experiment/result metadata schema;
- initial risk register.

## Exit criteria

M0 is complete only when:

- [ ] every required language has at least one candidate STT path and one candidate TTS path, or an explicit blocker is documented;
- [ ] no shortlisted critical dependency has an unresolved licence assumption that would make distribution clearly unsafe;
- [ ] at least one STT candidate runs fully offline on a real low- or mid-range Android target;
- [ ] at least one TTS candidate runs fully offline on a real low- or mid-range Android target;
- [ ] two physical phones exchange a Unicode payload without internet infrastructure;
- [ ] cold/warm latency, model footprint, and memory observations from the feasibility run are recorded rather than guessed;
- [ ] the next highest-risk assumption is named in `docs/RISK_REGISTER.md`.

## Evidence

Store or link:

- model/licence matrix;
- device/runtime notes;
- reproducible inference command/build configuration;
- first transport test result;
- experiment record(s).

## Explicitly out of scope

- polished UI;
- multi-hop routing;
- ten-language production integration;
- publication claims;
- optional radio bridge.

---

# M1 — Minimum Complete Offline Loop

## Objective

Prove the fundamental VĀṆI communication thesis end to end on two real Android phones.

## Required vertical slice

```text
PTT
 -> audio capture
 -> endpointing
 -> offline STT
 -> visible/editable transcript
 -> local validation
 -> semantic bundle
 -> direct local transport
 -> receiver validation
 -> offline TTS
 -> playback
 -> acknowledgement
```

## Required deliverables

- PTT capture and explicit state machine;
- VAD/endpointing sufficient for the selected test language;
- one fully offline STT path;
- transcript display and correction/confirmation path;
- bounded, versioned semantic bundle sufficient for direct delivery;
- direct BLE or local-Wi-Fi transport;
- receiver-side offline TTS;
- acknowledgement return path;
- truthful delivery states;
- stage-wise monotonic timestamps;
- basic disconnect/retry/duplicate handling.

## Exit criteria

M1 is complete only when:

- [ ] two physical Android phones complete the full speech → text → bundle → transport → speech → ACK loop;
- [ ] internet access is disabled for the judged path and no remote STT/TTS/authentication service is required;
- [ ] the sender distinguishes at least queued/transferred/delivered/acknowledged states rather than displaying an ambiguous `sent` state;
- [ ] the receiver plays locally synthesized speech derived from the actually received text;
- [ ] endpoint, STT, transport, TTS-first-audio, and end-to-end timing markers are recorded;
- [ ] bundle bytes are measured from the real serialized message;
- [ ] duplicate input does not cause duplicate message identity or uncontrolled duplicate playback;
- [ ] peer loss or ACK loss results in a truthful non-success state;
- [ ] the complete path can be repeated from written instructions.

## Evidence

- physical-device test record;
- airplane/offline test record;
- serialized bundle fixture or test vector;
- latency trace;
- failure-path notes/tests;
- short reproducible demo procedure.

## Explicitly out of scope

- claiming all ten languages are complete;
- multi-hop mesh claims;
- adaptive routing;
- publication novelty claims;
- hardware bridge.

---

# M2 — Ten-Language Credibility

## Objective

Turn the single-language vertical slice into a credible implementation of the official ten-language requirement.

Required languages:

1. Hindi
2. Gujarati
3. Marathi
4. Kannada
5. Malayalam
6. Tamil
7. Telugu
8. Odia
9. Bengali
10. English

## Required deliverables

- language-pack/runtime abstraction;
- verified STT path for every language;
- verified TTS path for every language;
- model/voice manifests with provenance and checksums where practical;
- language-aware normalization tests;
- native-script and numeric/critical-token fixtures;
- per-language ASR evaluation;
- per-language TTS intelligibility evaluation protocol/results;
- model size, RAM, latency/RTF, and storage observations by relevant device/model configuration;
- missing/corrupt model-pack behavior.

## Exit criteria

M2 is complete only when:

- [ ] every required language executes an offline speech → text path on target Android hardware;
- [ ] every required language executes an offline text → speech path on target Android hardware;
- [ ] language support is demonstrated individually rather than inferred from a generic `multilingual` model label;
- [ ] every active model/voice has recorded provenance and licence status;
- [ ] per-language WER/CER or another explicitly justified ASR evaluation is reproducible;
- [ ] TTS intelligibility is evaluated with a documented method rather than team impression alone;
- [ ] model/runtime resource measurements exist for representative device classes;
- [ ] the app refuses or clearly reports a missing/corrupt language pack instead of pretending support;
- [ ] the complete M1 direct loop works in representative languages from different scripts/language families.

## Evidence

- ten-language model matrix;
- test corpus/fixture manifest;
- per-language result tables;
- model manifests/checksums/licence notes;
- device benchmark records.

## Explicitly out of scope

- translation between languages;
- automatic language detection as a required dependency;
- unsupported claims of equal accuracy across languages.

---

# M3 — Safety-Aware Messaging

## Objective

Prevent the system from treating operationally critical information as ordinary text.

## Required deliverables

- normalization policy that preserves raw and normalized transcript forms where required;
- deterministic or otherwise auditable critical-field extraction for fields such as negation, numbers, coordinates, locations, times, quantities, identities, and emergency terms;
- confidence adapter/calibration strategy where the underlying ASR exposes usable uncertainty information;
- sender confirmation/read-back behavior for risky content;
- structured critical-field metadata in the semantic bundle;
- receiver uncertainty/warning presentation;
- acknowledgement policy hooks for critical messages;
- tests for semantic corruption cases.

## Exit criteria

M3 is complete only when:

- [ ] annotated critical fields can be identified and serialized without silently changing the transcript;
- [ ] low-confidence or policy-triggering critical content produces an explicit safe behavior such as confirm, repeat, correct, or warn;
- [ ] the system never silently uses a language model to `repair` a life-critical value as if it were certain;
- [ ] receiver UI distinguishes uncertain critical content from verified/ordinary content;
- [ ] tests include negation, number/time/quantity, and location/name failure cases;
- [ ] Critical Field Accuracy or the chosen equivalent scoring procedure is defined and reproducible;
- [ ] safety behavior is included in the physical two-phone demonstration path.

## Evidence

- critical-field fixtures;
- confirmation/warning test cases;
- metric/scoring code or procedure;
- recorded failure examples;
- updated protocol/UI documentation.

---

# M4 — Resilient Semantic Delivery

## Objective

Make delivery durable, bounded, secure-by-explicit-design, and resilient to intermittent connectivity without confusing relay progress with destination receipt.

## Required deliverables

- canonical compact semantic-bundle encoding/test vectors;
- bounded framing and fragmentation/reassembly;
- durable outbox/inbox queues;
- expiry/lifetime behavior;
- replay protection and durable duplicate suppression;
- authenticated integrity/security integration using established primitives/libraries;
- transport abstraction for direct local transports;
- application-level relay path;
- store-carry-forward behavior;
- bounded routing policies and queue/storage limits;
- controlled-flood baseline;
- binary Spray-and-Wait baseline;
- restart/recovery behavior;
- truthful states for queued, relayed/forwarded, delivered, played, acknowledged, expired, and failed.

## Exit criteria

M4 is complete only when:

- [ ] direct delivery remains reliable after the resilient-delivery layer is introduced;
- [ ] malformed/oversized/expired/replayed bundles are rejected according to documented policy;
- [ ] fragmentation/reassembly has deterministic tests and bounds;
- [ ] a message survives process/app restart when the documented durability policy says it should;
- [ ] at least three physical devices demonstrate a real relay or store-carry-forward scenario;
- [ ] intermediate relay acceptance is never presented as destination delivery;
- [ ] duplicate paths do not create unbounded forwarding or repeated final playback;
- [ ] controlled flooding and bounded-copy routing can be evaluated under the same scenario definition;
- [ ] relay storage, retry, copy, hop, lifetime, and priority behavior are bounded;
- [ ] private relay content is protected according to the documented threat model.

## Evidence

- protocol test vectors;
- fragmentation/fuzz/property tests where practical;
- physical relay/store-forward experiment;
- route/queue traces;
- restart/replay tests;
- security/threat-model updates.

## Explicitly out of scope

- claiming standardized Bluetooth Mesh unless actually implemented;
- guaranteed range/delivery claims;
- unbounded flooding;
- custom cryptography;
- making an optional LoRa/radio bridge a dependency of the Android core.

---

# M5 — Scientific Evidence

## Objective

Establish which parts of VĀṆI actually help, under what conditions, and at what resource cost.

## Required deliverables

- automated/reproducible speech benchmark harness;
- reproducible network impairment/simulation scenarios;
- physical-device scenarios;
- experiment registry with configuration provenance;
- ASR/TTS accuracy and intelligibility results;
- end-to-end latency distributions;
- RAM/CPU/storage/energy measurements or documented proxies;
- delivery probability/delay/overhead measurements;
- Message/Semantic Message Success Rate and critical-field metrics;
- compressed-audio and relevant text/direct-delivery baselines;
- routing baselines;
- ablations for claimed contributions;
- confidence intervals/variability reporting where appropriate;
- negative results and limitations.

## Exit criteria

M5 is complete only when:

- [ ] public claims trace to a reproducible experiment record and code/configuration revision;
- [ ] all ten languages are reported separately before any macro summary is used;
- [ ] latency is reported as a distribution including tail behavior, not only one average;
- [ ] semantic/text compression is compared against a clearly defined realistic compressed-audio baseline as well as raw audio where useful;
- [ ] controlled flood and bounded-copy routing are compared under equivalent scenarios if routing benefits are claimed;
- [ ] every claimed novel component has a baseline and/or ablation capable of isolating its contribution;
- [ ] critical-field preservation is evaluated separately from ordinary WER/CER;
- [ ] physical-device evidence exists for claims that depend on Android radio/audio/power/lifecycle behavior;
- [ ] experiment provenance is sufficient for another engineer to reproduce a representative result.

## Evidence

- experiment registry;
- raw and derived result files;
- evaluation scripts;
- benchmark tables/plots;
- baseline and ablation reports;
- limitations document;
- draft research results.

---

# M6 — Competition-Ready Product

## Objective

Turn the measured system into a robust, understandable, failure-resistant SIH demonstration and submission.

## Required deliverables

- polished accessible Android UX;
- onboarding and offline language-pack checks;
- clear offline/peer/delivery states;
- production-safe urgent-message behavior through Android-supported APIs;
- diagnostics view driven by real pipeline data;
- complete live-demo script;
- intentionally tested failure/recovery sequence;
- fallback recording of the real system;
- architecture/protocol/security/licence/model/evaluation/limitations documentation;
- judge-facing metrics summary;
- pitch, poster/report, and Q&A material;
- reproducible build/run instructions.

## Exit criteria

M6 is complete only when:

- [ ] the primary two-phone demo works with internet disabled;
- [ ] the ten-language claim is backed by M2/M5 evidence;
- [ ] safety-aware behavior from M3 is visible in the demo or judge walkthrough;
- [ ] resilient-delivery behavior claimed publicly is backed by M4/M5 evidence;
- [ ] dashboards show real measured data or are clearly labelled when using fixtures/simulation;
- [ ] no UI state overclaims human receipt or network delivery;
- [ ] licences/model/data provenance can be explained under judge scrutiny;
- [ ] known limitations and Android policy constraints are documented honestly;
- [ ] a teammate can clone/build/run the representative system from written instructions;
- [ ] the demo has a tested fallback that does not fabricate live behavior.

## Evidence

- release/demo build;
- final demo checklist;
- offline run recording;
- documentation index;
- final licence/model inventory;
- judge Q&A and pitch materials.

---

# 3. Maturity-tier mapping

The maturity tiers are **not additional milestones** and must never be used to skip milestone gates.

| Maturity label | Meaning | Roadmap relationship |
|---|---|---|
| **Tier 1 — Credible MVP** | Complete offline product thesis with credible ten-language support | Requires **M0, M1, and M2**. Basic direct-delivery reliability belongs inside M1; advanced relay/DTN does not. |
| **Tier 2 — Competition-grade / winning prototype** | Adds critical-information protection, resilient delivery, and strong evidence | Built by completing **M3, M4, and M5** on top of Tier 1. |
| **Competition-ready** | SIH submission/demo quality | Requires **M6** in addition to the capabilities actually claimed. |
| **Tier 3 — Publication-grade extension** | Research scope beyond the competition minimum | Builds primarily on **M5** evidence and may continue before, during, or after M6; it never replaces competition hardening. |

### Important consequence

`Tier 1`, `Tier 2`, and `Tier 3` describe **how far the system/research has matured**.

`M0` through `M6` describe **the order in which engineering evidence must be established**.

Do not create a second phase numbering scheme.

---

# 4. Current-work rule

At any point, the active engineering task should be traceable to one milestone.

Every substantial issue/commit/experiment should be able to answer:

```text
Milestone:
Required deliverable advanced:
Validation performed:
Evidence produced:
Remaining blocker/risk:
```

Work from a later milestone may be explored experimentally, but it must not be treated as a reason to bypass an earlier exit gate.

---

# 5. Relationship to other documents

- `docs/MILESTONES.md` is a concise index to this roadmap, not an independent milestone specification.
- `docs/MVP.md` defines the Tier 1 product acceptance surface and should be read together with M1 and M2.
- `docs/TASK_BREAKDOWN.md` lists work packages; the roadmap determines when they become necessary.
- `docs/DEFINITION_OF_DONE.md` defines final project completion; M0–M6 define progression toward it.
- `docs/TEST_PLAN.md`, `docs/EVALUATION.md`, and `docs/EXPERIMENTS.md` define how milestone claims are validated.
- `docs/RISK_REGISTER.md` records blockers that can prevent a milestone from closing.

When any of these documents conflict on execution order or milestone naming, **this roadmap is authoritative unless an accepted ADR or explicit repository decision supersedes it**.
