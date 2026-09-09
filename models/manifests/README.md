# M0 Speech Candidate Manifests

This directory contains machine-checkable **feasibility candidates**, not final model selections.

`m0_candidates.json` answers only the first M0 question: *is there at least one plausible STT and TTS path worth investigating for every required language?*

It deliberately separates:

- language coverage;
- code licence;
- model-weight licence;
- dataset provenance;
- redistribution status;
- Android deployability.

A candidate may satisfy language coverage while still being blocked on licence scope, model conversion, storage, latency, RAM, or device compatibility.

## Status vocabulary

Candidate status:
- `candidate`: counts toward the M0 coverage inventory;
- `baseline_only`: useful comparison but not a complete required-language solution;
- `reference_only`: informative implementation/reference, not currently shortlisted.

Licence status:
- `provisional_clear`: primary source currently supports the stated licence, but exact selected artifact still needs its recorded checksum/provenance;
- `review_required`: licence or redistribution scope is not yet sufficiently pinned for a final distribution decision;
- `incompatible_without_distribution_decision`: using the component can materially change project distribution obligations.

Android status:
- `not_proven`: no Syntax6 real-device evidence yet;
- `documented_mobile_candidate`: upstream explicitly targets mobile-class deployment;
- `documented_third_party_runtime`: a mobile runtime exists, but Syntax6 has not validated it;
- `portable_phone_candidate`: upstream describes phone/embedded portability;
- `documented_android_support`: upstream documents Android support.

## Rule

Never turn `candidate` into `selected` merely because CI passes. M0 selection requires real-device evidence and a completed licence/provenance record for the exact artifact.
