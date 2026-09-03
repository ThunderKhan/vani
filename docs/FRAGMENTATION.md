# Fragmentation and Reassembly Specification

## Goal

Carry one logical semantic bundle across transports whose MTU is smaller than the serialized bundle.

## Rules

- message identity belongs to logical bundle;
- fragments use `(message_id, fragment_index, fragment_count)`;
- fragment count bounded before buffer allocation;
- total reconstructed length declared and bounded;
- each fragment independently framed;
- full bundle authenticated end-to-end;
- optional per-fragment integrity may protect transport corruption, but does not replace final authentication.

## Receiver strategy

1. validate metadata bounds;
2. check duplicate fragment;
3. persist or buffer fragment;
4. expire incomplete reassembly;
5. reconstruct only when all fragments exist;
6. validate total length/hash;
7. authenticate/decrypt;
8. deliver exactly once.

## Attack resistance

Reject:
- fragment_count = huge;
- conflicting fragment_count for same message;
- overlapping index data;
- reconstructed size over limit;
- fragments after expiry;
- endless sparse fragment sets.

## Metrics

- fragmentation rate;
- fragments per message;
- reassembly latency;
- reassembly failure rate;
- duplicate fragment rate;
- bytes of framing overhead.
