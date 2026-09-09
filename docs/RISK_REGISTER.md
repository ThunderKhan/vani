# Risk Register

| Risk | Severity | Early test | Mitigation |
|---|---|---|---|
| A required language lacks deployable model | Critical | M0 language audit | alternative model family / language packs |
| TTS licence prevents redistribution | Critical | licence matrix | replace early |
| **All-ten speech coverage exists on paper but no practical Android packaging path exists** | **Critical** | **M0 exact-artifact + real-device probe** | **separate coverage from deployment; benchmark compact first path; use installable packs/shared models only after measured evidence** |
| **Exact model-weight/dataset licence scope remains ambiguous for a shortlisted artifact** | **Critical** | **M0 provenance record with exact version/checksum** | **do not freeze candidate until code/weights/data/redistribution are independently recorded** |
| Low-end Android inference too slow | High | real-device benchmark | int8, smaller model, runtime tuning |
| BLE behavior varies by vendor | High | multi-device tests | transport fallback |
| Background scanning restricted | High | lifecycle test | foreground emergency mode |
| Endpointing cuts speech | High | E03 | tune/risk-aware endpoint |
| Critical number misrecognized | Critical | safety corpus | confirmation/read-back |
| Mesh flood drains battery | High | routing benchmark | bounded copies/quotas |
| ACK semantics ambiguous | High | protocol state tests | explicit states |
| Fragment abuse causes memory pressure | High | fuzz test | hard bounds |
| Demo relies on hidden internet | Critical | airplane-mode CI/manual gate | offline dependency audit |
| Ten-language APK too large | High | storage model | installable packs |
| Publication novelty too weak | Medium | baseline review | cross-layer research focus |
| Team integration conflicts | Medium | module ownership | interfaces + ADRs |

## Current M0 highest-risk assumption

The immediate blocker is no longer merely finding names of multilingual models. It is proving a **legally distributable and practically deployable Android speech path** while preserving a credible route to all ten languages. Candidate coverage and mobile feasibility are tracked separately in `models/manifests/m0_candidates.json`.
