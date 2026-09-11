# Sixfold Quality Audit V2 — M0 + M1

**Audit date:** 2026-09-12
**Scope:** current `main` after the previous sixfold audit, focused on M0/M1 source, tests, Android lifecycle, protocol framing, evidence integrity, documentation consistency, and low-effort/AI-slop patterns.
**Method:** three fresh top-to-bottom passes followed by three fresh bottom-to-top passes. Each pass was treated as independent; findings from an earlier pass were not assumed correct merely because they were already documented.

## Pass 1 — Top to bottom: repository and compilation boundary

- Re-enumerated the repository tree and Android source/test boundaries.
- Confirmed the M0 and M1 implementations have distinct package boundaries.
- Confirmed the old root-level duplicate M1 abstractions are absent from the current tree.
- Confirmed `MainActivity` is again an M0 activity and its referenced view IDs belong to the M0 layout.
- Confirmed M1 is exposed from its own UI without replacing the M0 instrument.

**Finding:** no remaining duplicate M1 class boundary was found. The prior M0 activity regression is not present in the current source.

## Pass 2 — Top to bottom: protocol and data-flow audit

### M0

- Frame magic/version checked before payload use.
- Device metadata is bounded to 512 UTF-8 bytes.
- Payload is bounded to 64 KiB.
- Empty payloads are now rejected consistently at write, read, and client-send boundaries.
- UTF-8 decoding is strict and rejects malformed sequences.
- SHA-256 is calculated and verified before receiver evidence is emitted.
- ACK length and hash are compared by the sender.
- A malformed peer connection no longer terminates the M0 server loop; the client boundary is isolated and reported.

### M1

- Message ID is bounded and frame/bundle IDs must agree.
- Bundle size is bounded.
- Bundle UTF-8 is strict.
- Bundle hash is sent on the wire and verified by the receiver and sender ACK path.
- Receiver validation precedes TTS.
- Duplicate IDs suppress a second TTS playback.
- Delivery state transitions are explicit.

**Finding:** no new framing/integrity bypass was found in the reviewed paths.

## Pass 3 — Top to bottom: lifecycle, concurrency, and failure-path audit

- M0 server now isolates per-peer failures rather than letting malformed input escape through the accept loop.
- M1 server already isolates malformed peer input at its per-client handler boundary.
- M1 retry is bounded to one retry rather than an unbounded loop.
- M1 recognizer creation/start failures are now converted to explicit ASR failure results rather than crashing the activity.
- M1 TTS `speak()` failures are now handled.
- TTS completion/error callbacks are made single-shot with an atomic guard so a synchronous `speak()` failure and a later callback cannot report two completions.
- Activity teardown destroys speech resources, stops servers, and shuts down executors.

**Finding:** several previously untested lifecycle/error edges were hardened during this audit.

## Pass 4 — Bottom to top: evidence and measurement integrity

- Started from the M0/M1 exit gates and traced every implementation claim backward.
- M0 status still correctly says physical evidence is open.
- M1 status still correctly says implementation is complete but physical evidence is open.
- Offline claims remain gated on `NET_CAPABILITY_VALIDATED` rather than Wi-Fi presence alone.
- `EXTRA_PREFER_OFFLINE` is not treated as proof.
- Receiver TTS is rejected when the selected voice explicitly reports network dependence.
- M0 evidence explicitly warns that its network check is not packet-capture proof.
- M1 timing was re-read critically. The field previously named `transportMillis` included the receiver TTS/ACK wait, so the name overstated what was measured. It has been renamed to `endToEndMillis`; the UI now reports it as end-to-end rather than pretending it is transport-only.

**Finding:** the largest measurement-language issue discovered here was corrected rather than left as a documentation caveat.

## Pass 5 — Bottom to top: semantic and documentation consistency

- M1 implementation documentation now names the actual `M1Bundle`, `M1Message`, `M1Transport`, `M1Speech`, and `M1DeliveryStore` boundary.
- M1 status describes the actual direct TCP implementation and physical evidence gate.
- Milestone definitions place expiry, replay protection, fragmentation, relay/store-forward, and routing in M4 rather than falsely requiring them in M1.
- The M1 message object currently carries `priority`, `expiresAfterMillis`, and `ackPolicy`. These fields are validated and serialized, but M1 does not yet enforce expiry or vary behavior by ACK policy. This is an intentional semantic-schema carry-forward, not evidence that M1 has implemented M4 delivery policy. It should remain explicitly documented or be removed before a production protocol freeze.
- M0 candidate inventory continues to mark exact weight/licence/provenance closure and Android deployability as open where appropriate.

**Finding:** no false M4 feature claim was found. The three carried semantic fields are the main remaining “declared but not operational” area and are not allowed to become hidden implementation claims.

## Pass 6 — Bottom to top: anti-slop and release-gate audit

Checked for:
- placeholder functions;
- fake success paths;
- unused duplicate abstractions;
- silent network fallbacks;
- unbounded input;
- unexplained constants where a protocol constant exists;
- catch-and-ignore around functional errors;
- dead documentation claims;
- tests that only exercise happy paths.

The reviewed M0/M1 source contains concrete protocol bounds, explicit failure handling, actual state transitions, and tests for malformed data and edge sizes. No obvious placeholder/TODO-driven implementation pattern was found.

One intentional prototype limitation remains: the M1 transport is unauthenticated. SHA-256 here provides integrity detection, not sender authentication or confidentiality. That is correctly outside M1's scope.

## Findings fixed in this audit

1. **M0 malformed peer could kill the server loop.** Fixed with a per-client safety boundary.
2. **M0 protocol accepted empty payloads while the UI required 1..64 KiB.** Fixed so the protocol and UI agree.
3. **M0 client UI could show an ACK as successful even if the returned hash did not match.** Fixed so transfer success requires both acceptance and integrity.
4. **M1 transport metric was mislabeled as transport time even though it measured the complete ACK round trip, including receiver-side TTS.** Fixed by naming/reporting it `endToEndMillis`.
5. **M1 ASR creation/start exceptions could escape the speech adapter.** Fixed with explicit failure results.
6. **M1 TTS could potentially report completion more than once on immediate `speak()` failure plus callback.** Fixed with a single-shot completion guard.
7. **M0 malformed-UTF-8 and empty-payload edge cases lacked explicit tests.** Added coverage.

## Remaining issues / accepted boundaries

### High importance
- **Physical M0/M1 evidence remains absent.** Static audit cannot prove real offline ASR/TTS or two-phone behavior.
- **Exact model-weight licence/provenance decisions remain open.** Candidate inventory is not legal clearance.
- **The final 20% of scoring information remains unresolved in the working ledger.**

### Medium importance
- M1 `expiresAfterMillis` is not enforced.
- M1 `ackPolicy` is currently metadata rather than a behavior switch.
- M1 uses `SharedPreferences` for duplicate/delivery state, acceptable for a disposable feasibility slice but not a final messaging store.
- M1 local TCP is unauthenticated and unencrypted; production security belongs later.
- M0 evidence records a network-capability snapshot, not a packet capture or independently verified network isolation proof.

### Low importance / prototype quality
- M0 server handles one client synchronously. This is adequate for a two-phone probe but intentionally not a concurrency design.
- The platform TTS adapter currently rejects a network-required selected voice instead of actively selecting among available local voices; this may produce a false negative on devices that have both local and network voices. This should be improved during the M0 speech experiment if hardware testing exposes it.

## Final verdict

**No new critical architectural defect remains hidden in the reviewed M0/M1 paths. Several real quality defects were found and repaired during this second sixfold audit.**

The repository is now at the point where further speculative coding has diminishing value. The next high-value step is controlled physical testing: two phones, Internet unavailable, offline ASR, editable transcript, Unicode bundle transfer, receiver-side offline TTS, ACK, duplicate replay, peer-loss failure, and measured resource/latency evidence.

M0 and M1 must remain open until those empirical gates pass.
