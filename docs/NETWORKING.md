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


## M4 implementation boundary

M4 adds an application-level delivery layer without replacing the existing M1 direct TCP feasibility transport.

- M4Transport/M4RelayLink define the transport boundary for opaque frames.
- RelayEnvelope carries only message identity, expiry, priority, remaining hop/copy budgets and protected bundle bytes.
- ControlledFloodPolicy is bounded by hop and copy budgets and queue/expiry checks.
- BinarySprayAndWaitPolicy splits a bounded copy budget and waits when only one copy remains unless the peer is the destination.
- M4DeliveryStore persists outbox/inbox/relay records, seen-message IDs and incomplete fragments in SQLite.
- Expired records are retained as truthful EXPIRED state rather than being silently converted into success or deleted before evidence can inspect the terminal state.

The physical 3-device relay/store-carry-forward scenario remains deliberately untested until the final physical validation pass.
