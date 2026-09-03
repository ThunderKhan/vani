# Product Requirements Document

## 1. Product

**Working name:** Syntax6 MeshVoice  
**Problem statement:** SIH26173  
**Primary platform:** Android  
**Primary operating mode:** Fully offline, local device-to-device semantic voice communication.

## 2. Problem

Audio is expensive to transmit compared with compact text. In damaged or absent infrastructure, users may still have short-range BLE or local Wi-Fi contact, but continuous voice streams can be unreliable, power-hungry, or infeasible.

Typing is not always appropriate either: users may be stressed, moving, gloved, visually constrained, unfamiliar with the script, or unable to read comfortably.

MeshVoice converts speech to text on the sender, sends a compact semantic representation, and synthesizes speech at the destination.

## 3. Target users

### Primary
- disaster response teams;
- rescue volunteers;
- expedition teams;
- rural/off-grid workers;
- field engineers;
- forest/mining teams;
- temporary response camps;
- communities with weak or damaged connectivity.

### Secondary
- event operations;
- local industrial communication;
- educational field deployments;
- humanitarian simulations and research.

## 4. Jobs to be done

- “Let me speak naturally even when the internet is unavailable.”
- “Let my message survive a weak link.”
- “Tell me whether it actually reached the destination.”
- “Warn me before a possibly wrong number or negation is transmitted.”
- “Let nearby devices help relay a message without reading private content.”
- “Use my language without requiring a cloud service.”
- “Keep working on a modest Android phone.”

## 5. Required capabilities

### P0
- fully offline STT and TTS;
- ten-language same-language speech reconstruction;
- PTT flow;
- direct BLE or local Wi-Fi transfer;
- compact versioned message bundle;
- sender transcript visibility;
- receiver TTS playback;
- acknowledgement;
- failure and timeout states;
- on-device metrics.

### P1
- critical-span detection and confirmation;
- message priority;
- robust endpointing;
- fragmentation/reassembly;
- duplicate suppression;
- durable queue;
- store-carry-forward;
- three-device relay;
- resource-aware model selection;
- security and replay protection.

### P2
- uncertainty-aware redundancy;
- adaptive routing;
- optional ESP32/low-rate-radio bridge;
- incident/group messaging;
- cross-device benchmark dashboard;
- publication-grade experimental modes.

## 6. Functional requirements

### Speech input
- microphone capture with documented sample rate;
- VAD to avoid unnecessary inference;
- endpoint detection for utterance finalization;
- PTT and continuous conversational mode;
- cancel before sending.

### STT
- offline;
- required language selected explicitly;
- output visible before send when policy requires;
- confidence exposed if available;
- failures surfaced rather than silently replaced.

### Semantic safety
- Unicode normalization;
- critical span tagging;
- thresholds for confirmation;
- raw transcript retained in local processing context;
- critical content never silently rewritten by a generative model.

### Transport
- common transport adapter;
- BLE and local Wi-Fi implementations;
- bounded retries;
- timeouts;
- acknowledgements;
- duplicate detection;
- optional relay.

### Receiver
- integrity and replay verification;
- correct language pack selection;
- text display;
- local TTS;
- replay;
- acknowledgement;
- uncertainty badge/read-back behavior.

## 7. Non-functional requirements

### Accuracy
Per-language WER/CER plus critical-field accuracy and end-to-end message success.

### Latency
Stage-level timing with P50/P90/P95. No claim from “feels fast.”

### Efficiency
Track model size, APK size, peak RAM, CPU, active/idle energy proxy, RTF, and thermal throttling.

### Reliability
Test direct link, reconnection, duplication, reordering, loss, queue recovery, process death, and malformed payloads.

### Security
Authenticated message origin, integrity, replay protection, bounded resource allocation, encrypted private payloads.

### Accessibility
Large PTT control, haptics, audio cues, high contrast, clear state labels, no color-only semantics.

## 8. Success metrics

- 10/10 required language paths validated;
- 100% judged-path independence from internet APIs;
- complete two-phone loop on physical hardware;
- critical-field accuracy reported separately from WER;
- direct transport success under repeatable test conditions;
- measured end-to-end latency and resource usage;
- receiver playback and acknowledgement;
- no ambiguous “sent” state;
- reproducible build and experiment metadata.

## 9. Product constraints

- no proprietary cloud STT/TTS in judged path;
- no unbounded mesh flooding;
- no unsafe DND/volume bypass claims;
- no invented crypto;
- no unverifiable range claims;
- no hidden prerequisite that requires internet during message exchange.

## 10. Out of scope for initial competition build

- mandatory translation;
- voice cloning;
- preservation of original emotion or speaker identity;
- satellite connectivity;
- custom RF waveform;
- full BPv7 interoperability;
- massive cloud analytics;
- blockchain;
- social/chat features unrelated to field communication.
