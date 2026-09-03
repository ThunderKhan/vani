# Live Demo Script

## Core demo

1. Show both phones in airplane mode.
2. Show language packs available locally.
3. Select Hindi/Tamil/Odia or another challenging language.
4. Press PTT and speak a realistic instruction.
5. Show endpoint event and transcript.
6. Highlight critical fields.
7. Confirm.
8. Show logical bundle bytes and comparison to audio baselines.
9. Transfer locally.
10. Receiver validates and synthesizes.
11. Show TTS playback.
12. Receiver acknowledges.
13. Sender UI advances to person-acknowledged state.
14. Show stage latency and device metrics.

## Strong extension

- introduce third phone relay;
- remove direct sender/receiver connectivity;
- queue message;
- physically move relay/contact;
- deliver later;
- show hop/queue metrics.

## Safety moment

Speak a phrase with a number/negation and intentionally produce low confidence. Demonstrate confirmation rather than fake certainty.

## Failure fallback

If radio fails:
- show exact error;
- switch transport if supported;
- do not use prerecorded output as if live.

A separate recorded fallback may be presented transparently.
