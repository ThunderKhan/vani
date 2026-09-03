# Critical Information Safety

## Why this exists

WER weights all words similarly. Field communication does not.

“Enter Sector 13” and “Do not enter Sector 13” differ by one token but may produce opposite actions.

## Critical classes

- negation;
- number;
- coordinate;
- time/date;
- quantity;
- location;
- person/call sign;
- emergency keyword;
- domain-sensitive term.

## Safety pipeline

1. ASR emits transcript and confidence metadata.
2. deterministic recognizers mark candidate critical spans.
3. risk policy estimates whether confirmation is needed.
4. sender sees emphasized span/read-back.
5. sender accepts, edits, or repeats.
6. structured field metadata is included in bundle.
7. receiver displays uncertainty.
8. TTS may spell/read digits separately for difficult fields.

## Rules

- never silently LLM-correct critical content;
- confidence is not assumed calibrated;
- unknown/low-confidence distress messages may be transmitted with uncertainty markers rather than blocked if policy permits;
- critical fields receive stricter testing.

## Metrics

Critical Field Accuracy:

`CFA = correctly preserved critical fields / total critical fields`

Message Success:
all required critical fields + intended action must survive sender ASR, transport, receiver TTS, and human interpretation.

## Confirmation policies

Possible:
- no confirmation for high-confidence routine text;
- selective span confirmation;
- full transcript confirmation;
- mandatory read-back for critical numeric fields.

Measure added latency versus safety benefit.
