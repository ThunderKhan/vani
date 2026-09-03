# Routing Research Plan

## Research question

Can a bounded, priority- and semantic-risk-aware forwarding policy improve delivery of critical messages under a fixed byte/energy budget compared with established DTN baselines?

## Baselines

### Direct-only
Lowest overhead. Fails when destination unavailable.

### Controlled flooding
High reach, potentially severe duplicate and energy cost.

### Binary Spray-and-Wait
Message begins with copy budget L. Copy tokens split across contacts until holders wait for destination.

### PRoPHET-style
Uses encounter history to estimate future delivery likelihood.

## Candidate Syntax6 policy

Inputs:
- message priority;
- critical-field risk;
- remaining lifetime;
- copy budget;
- queue occupancy;
- peer encounter score;
- battery policy;
- prior transfer attempts.

Output:
- whether to forward;
- how many copy tokens;
- queue ordering;
- ACK/redundancy policy.

## Objective

A conceptual multi-objective loss:

`J = alpha*(1-delivery) + beta*delay + gamma*bytes + delta*energy + eta*stale + zeta*starvation`

Weights must be declared per experiment.

## Anti-abuse

Priority-aware routing requires:
- authenticated priority where privileged;
- per-origin quotas;
- rate limiting;
- maximum copies;
- queue reservations;
- fairness tests.

## Experimental scenarios

- static line topology;
- random waypoint simulation;
- community/cluster mobility;
- partition and courier;
- dense contention;
- low-battery relay;
- mixed routine/distress load.

## Required outputs

- delivery probability vs copy budget;
- delay CDF;
- overhead ratio;
- energy proxy;
- fairness;
- expiry;
- critical-message success.
