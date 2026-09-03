# Networking Architecture

## Objectives

- direct communication first;
- transport abstraction;
- bounded overhead;
- truthful delivery;
- intermittent contact support;
- measurable energy/latency trade-offs.

## Direct transport hierarchy

Candidate transports:
1. local Wi-Fi when mutually available;
2. Wi-Fi Direct where device support is reliable;
3. BLE GATT for universal low-rate local path;
4. optional serial/BLE hardware bridge.

Selection must be measured, not hard-coded from assumptions.

## Peer discovery

Discovery is expensive. Policies should:
- adapt scan duty cycle;
- expose battery implications;
- respect Android background restrictions;
- avoid permanent aggressive scanning;
- separate foreground emergency mode from routine mode.

## Multi-hop

Application-level forwarding may use nearby phones. It is not equivalent to Bluetooth SIG Mesh.

Baseline algorithms:
- direct-only;
- controlled flood;
- binary Spray-and-Wait;
- PRoPHET-style encounter prediction.

## Store-carry-forward

Bundles may wait on a device when no route exists. Required:
- durable storage;
- expiry;
- queue limits;
- per-origin quotas;
- deletion policy;
- restart recovery;
- acknowledgement propagation.

## Network metrics

- delivery ratio;
- delay P50/P95;
- transmissions per delivered message;
- bytes per delivered message;
- queue residence time;
- expiry fraction;
- duplicate suppression rate;
- energy proxy;
- fairness between priority classes.

## Physical validation

At least:
- 2-phone direct;
- 3-phone line topology;
- partition-and-merge/courier scenario;
- congestion scenario;
- disconnect/reconnect;
- relay process restart.
