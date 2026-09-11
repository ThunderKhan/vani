# M0 / M1 Quality Audit

**Audit date:** 2026-09-12
**Repository revision audited:** `2029202d298c77df7954f8deec9c0bfe56442d2d` and subsequent cleanup commits on `main`
**Scope:** M0 contracts, M0 Android feasibility probe, M1 Android vertical slice, tests, CI, and milestone documentation.

## Audit method

The review checks four dimensions:

1. **Requirement traceability** — every M0/M1 exit criterion is explicitly classified as implemented, evidence-open, or blocked.
2. **Failure behavior** — malformed input, oversize input, duplicate messages, peer/ACK loss, unavailable speech services, and network-isolation mistakes are considered.
3. **Code-quality heuristics** — placeholder/TODO markers, dead duplicate implementations, unsafe assertions, silent failure, unchecked bounds, ambiguous state, and unsupported claims.
4. **Reproducibility** — unit tests and GitHub Actions must execute the repository contracts/build; physical-device claims remain separate evidence gates.

## M0 criterion review

| Criterion | Status | Finding |
|---|---|---|
| 10-language ASR candidate coverage | PASS (repository contract) | Validator requires the exact ten-language set and candidate coverage. This is not model execution evidence. |
| 10-language TTS candidate coverage | PASS (repository contract) | Same boundary: candidate inventory only. |
| Licence assumptions | OPEN | Several exact weights/voice artifacts remain `review_required`; this is correctly surfaced rather than hidden. |
| Offline ASR on low/mid Android | OPEN | Requires a real-device run. |
| Offline TTS on low/mid Android | OPEN | Requires a real-device run. |
| Two-phone Unicode transfer | OPEN | Protocol and Android probe exist; physical exchange remains unverified. |
| Cold/warm latency | OPEN | Evidence templates exist; measurements cannot be fabricated. |
| Model footprint/RAM | OPEN | Requires selected artifact + device measurements. |
| Highest remaining risk | PASS | Risk register is part of the M0 contract/documentation set. |

## M1 criterion review

| Criterion | Status | Finding |
|---|---|---|
| PTT | IMPLEMENTED | `M1Activity` uses a real press/release touch lifecycle. |
| Endpointing | IMPLEMENTED / PHYSICAL CHECK OPEN | Relies on recognizer `onEndOfSpeech`; physical behavior still needs measurement. |
| Offline STT | IMPLEMENTED / PHYSICAL CHECK OPEN | API 31+ on-device recognizer is required; there is no network fallback. |
| Editable transcript | IMPLEMENTED | Sender transcript is an editable field before sending. |
| Local validation + semantic bundle | IMPLEMENTED | Version, fields, language tag, bounds, timestamps and UTF-8 validity are validated. |
| Direct local transport | IMPLEMENTED / PHYSICAL CHECK OPEN | TCP framing and bounded reads exist on port 42425. |
| Receiver validation | IMPLEMENTED | Frame size/magic/ID plus semantic bundle validation are performed. |
| Offline TTS | IMPLEMENTED / PHYSICAL CHECK OPEN | Network-required Android voices are rejected. |
| Playback | IMPLEMENTED | TTS synthesis is started on receiver and completion is awaited by the receive handler. |
| ACK | IMPLEMENTED | ACK contains acceptance, duplicate state, message ID and TTS first-audio timing. |
| Truthful delivery states | IMPLEMENTED | Sender persists queued/transferred/delivered/acknowledged/failed states. |
| Stage timing | PARTIAL | ASR, transport and receiver TTS timing exist; tomorrow's run must verify the timestamps against actual wall-clock behavior. |
| Retry | IMPLEMENTED | One bounded retry exists after a failed socket attempt. |
| Duplicate handling | IMPLEMENTED | Durable message-ID playback marker suppresses duplicate playback. |
| Peer/ACK failure | IMPLEMENTED | Failed socket/ACK paths end in `FAILED`; ACK loss cannot become success. |
| Repeatable procedure | DOCUMENTED | M1 status and implementation docs exist; physical run procedure still needs its evidence record. |

## Code-quality findings and actions

### Fixed during this audit

- Removed duplicate root-level M1 bundle/state/session wrappers that were not used by the actual `m1` package.
- Restored the dedicated M0 XML layout so the legacy M0 activity does not silently point at an M1-only layout.
- Hardened M1 bundle validation with strict UTF-8 decoding instead of silently accepting replacement characters.
- Added validation for source/destination lengths, language-tag shape, timestamps, expiry, priority and ACK policy.
- Expanded M1 unit coverage for Unicode, size limits, digest changes, invalid priority/version and malformed UTF-8.

### Remaining intentional prototype-level smells

- `M1Transport.Server` catches protocol/handler exceptions to return a negative ACK. This is intentional at the wire boundary, but production logging/diagnostics belong in a later hardening pass.
- The current M1 transport computes no separate authenticated transport MAC; integrity is bounded framing plus semantic validation. Cryptographic security is explicitly a later milestone.
- Android platform TTS is only accepted when the selected voice reports that network connectivity is not required. Physical offline execution remains the proof standard.
- The M0 legacy activity and M1 activity coexist deliberately so the feasibility probe can remain available while M1 is exercised. They are not duplicate implementations of the same product path.

## AI-slop assessment

No generic placeholder/TODO implementation pattern was found in the repository search performed for this audit. The M1 path has concrete protocol constants, bounded parsing, explicit states, real Android APIs, and failure handling rather than mocked success paths.

The audit does **not** claim that a detector can mathematically prove "zero AI-generated code". The useful engineering standard is stronger: every implementation claim must correspond to a concrete behavior, bounded input, explicit failure path, test, or documented evidence gate. Code that merely looks plausible without a validation path is not accepted as M0/M1 evidence.

## Current verdict

**M0:** repository contracts are healthy; physical feasibility evidence and exact dependency/licence decisions remain open.

**M1:** implementation is structurally present and materially exercised by JVM/unit-testable components, but the milestone is **not complete** until the physical two-phone offline run passes.

**Quality:** no obvious placeholder/slop pattern remains in the reviewed M0/M1 implementation. The remaining issues are known prototype boundaries rather than hidden claims of completeness.
