# Sixfold Quality Audit — M0 + M1

**Audit date:** 2026-09-12
**Scope:** repository state on `main`, with emphasis on M0/M1 contracts, Android implementation, tests, CI, documentation consistency, failure handling, and evidence integrity.
**Method:** three independent top-to-bottom passes followed by three independent bottom-to-top passes. The same code was re-read in both directions, with cross-file checks between implementation, tests, schemas, milestone definitions, and status documents.

## Audit standard

The audit treats a feature as real only when implementation, interface, failure path, test/evidence contract, and documentation agree. Static review cannot prove source provenance or prove physical offline behavior. In particular, M0 and M1 remain open until the required real-device evidence exists.

## Passes

### Top-to-bottom pass 1 — structural integrity
- Enumerated the repository tree and Android module.
- Checked M0/M1 source boundaries and deleted duplicate/root-level M1 abstractions.
- Checked manifest, Gradle configuration, resources, source packages, and tests.
- Found a stale `MainActivity` referencing deleted `M1Session`/`SemanticBundle` classes and view IDs that no longer existed in the restored M0 layout. This explained the earlier Android CI failure path.
- Fixed `MainActivity` so it is a standalone M0 instrument again.

### Top-to-bottom pass 2 — behavioral contracts
- Traced M0 frame creation, bounds, UTF-8 handling, SHA-256 generation, ACK generation, and sender-side verification.
- Traced M1 PTT, on-device ASR gating, editable transcript, bundle encoding, TCP transfer, receiver validation, TTS, ACK, delivery state, and retry behavior.
- Found that M1 documentation claimed SHA-256 integrity across the wire, while the original M1 transport only computed a local bundle digest and did not transmit/compare it. Fixed the M1 wire protocol to carry and verify the hash in both directions.
- Found permissive UTF-8 decoding in the M0 protocol helper. Fixed it to reject malformed/unmappable UTF-8 fields.

### Top-to-bottom pass 3 — adversarial quality review
- Looked for fake success paths, placeholder implementations, dead abstractions, unchecked bounds, misleading offline claims, broad error swallowing, and test cases that merely exercise happy paths.
- Confirmed M1 rejects network-backed ASR fallback and rejects a selected TTS voice when the platform reports that network connectivity is required.
- Confirmed bundle size, version, IDs, language tag, priority, timestamp, expiry range, ACK policy, and UTF-8 validity are bounded/validated.
- Confirmed M0 validator explicitly refuses to declare M0 complete from repository consistency alone.
- Added malformed UTF-8 framing coverage to the M0 tests.

### Bottom-to-top pass 1 — evidence integrity
- Started from milestone exit claims and traced each claim back to code and experiment contracts.
- M0 still correctly reports `NOT COMPLETE`; physical Unicode, offline ASR/TTS, measurements, and licence/provenance closure remain gates.
- M1 correctly reports `IMPLEMENTATION COMPLETE / PHYSICAL EVIDENCE OPEN` rather than claiming physical success.
- Checked that offline status is not inferred from Wi-Fi presence alone and that `EXTRA_PREFER_OFFLINE` is not treated as proof.

### Bottom-to-top pass 2 — documentation/code consistency
- Cross-checked M0 requirement ledger, milestone quick reference, M1 implementation notes, M1 status, protocol schema, Android code, and tests.
- Removed stale `SemanticBundle` implementation references from M1 status/implementation docs and aligned them with the actual `M1Bundle` package.
- Added an explicit M1-to-M0 navigation path because the M1 activity is the launcher while M0 remains a required evidence instrument.
- Confirmed the logical protocol schema is still a draft broader than the M1 implementation; fields belonging to later safety/security/routing work are not silently represented as implemented M1 behavior.

### Bottom-to-top pass 3 — release-gate review
- Reviewed CI configuration, Android SDK/JDK/Gradle setup, manifest permissions, source/test coverage, and current GitHub Actions state.
- M0 contract CI has a successful run for the malformed-UTF-8 test commit.
- The Android CI run for the current Android source/test commit was still executing at the final audit checkpoint; therefore this audit does not claim a final green Android CI result until that run completes.
- The previous Android CI failure is treated as a real defect, not ignored: it occurred before the cleanup that restored `MainActivity` consistency.

## Findings

### Fixed — critical
1. **Broken M0 activity compilation boundary.** `MainActivity` still referenced deleted experimental M1 classes and removed layout IDs. Fixed by restoring it to a standalone M0 transport/evidence instrument.
2. **M1 integrity claim was stronger than implementation.** SHA-256 was calculated but not carried and verified across the M1 wire. Fixed by adding a 32-byte hash to the data frame and ACK and verifying both sides.

### Fixed — high
3. **M0 UTF-8 field decoder accepted malformed byte sequences through replacement behavior.** Fixed with a strict UTF-8 decoder using reporting error actions.
4. **M0 probe accessibility was weakened by M1 becoming the launcher while M0 had no in-app entry point.** Fixed by adding an explicit M0 launch button in M1.

### Accepted prototype boundaries
5. **M1 transport is intentionally unauthenticated.** Integrity is not authentication. Production cryptography is a later milestone and is explicitly outside M1.
6. **M1 uses platform speech APIs rather than final ten-language model packs.** This is an M1 feasibility slice, not M2 language completion.
7. **M1 duplicate state uses SharedPreferences.** Adequate for a disposable two-phone slice, not a final durable messaging database.
8. **M1 server has a broad boundary catch around malformed peer input and lifecycle closure.** This keeps the feasibility probe alive; production diagnostics should become more explicit later.

## Remaining blockers

- Physical two-phone M1 run with Internet unavailable.
- Physical offline ASR proof on representative Android hardware.
- Physical receiver-side offline TTS proof.
- Measured cold/warm latency, RAM, model footprint, and device envelope.
- Ten-language M0 candidate licence/provenance freeze.
- Canonical organizer statement/reference and unresolved scoring 20%.
- Android CI final result for the latest Android source/test commit at audit time.

## Slop / low-effort review

No obvious placeholder/TODO-driven implementation pattern was found in the reviewed M0/M1 source. The implementation contains concrete bounds, failure paths, state transitions, tests, and evidence gates. The audit does **not** claim that source provenance can be mathematically determined from code appearance; instead, the engineering bar is that code must have a defensible reason to exist and be connected to a real contract, behavior, test, or evidence requirement.

## Verdict

**Repository quality after this audit: materially improved and structurally coherent, but M0 and M1 are still not complete.**

The two most serious defects found during this sixfold review were repaired directly on `main`. The remaining uncertainty is now primarily empirical rather than hidden behind fake implementation claims: the physical offline behavior, resource measurements, exact dependency licensing, and final organizer requirements still have to be demonstrated and frozen.
