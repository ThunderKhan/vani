# Risk Register

| Risk | Severity | Early test | Mitigation |
|---|---|---|---|
| A required language lacks deployable model | Critical | M0 language audit | alternative model family / language packs |
| TTS licence prevents redistribution | Critical | licence matrix | replace early |
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
