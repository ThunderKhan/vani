# Optional Embedded Radio Bridge

## Purpose

Carry the same semantic bundle across a low-rate long-distance radio segment using a phone-connected embedded device.

## Architectural rule

The bridge does not change application semantics. It accepts opaque bundle fragments and emits them on the other side.

## Candidate shape

Android phone <-> BLE/serial <-> ESP32-class controller <-> permitted low-rate radio <-> bridge <-> phone.

## Risks

- regulatory frequency/power/duty-cycle constraints;
- throughput;
- fragmentation;
- antenna dependence;
- unsupported range claims;
- additional cryptographic boundary;
- battery supply.

## Competition role

Extension only. The mandatory Android system must remain complete without it.
