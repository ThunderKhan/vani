# M0 Requirement Ledger — SIH26173

> **Status:** working frozen ledger for engineering as of 2026-09-09.  
> **Important:** this is not a substitute for preserving the organizer's original statement artifact/URL. The repository still needs that canonical external reference before M0 can close.

## Required product facts currently treated as non-negotiable

- Primary deliverable is an Android application.
- STT and TTS operate fully offline in the judged communication path.
- Required languages are Hindi, Gujarati, Marathi, Kannada, Malayalam, Tamil, Telugu, Odia, Bengali, and English.
- Same-language reconstruction is the required core path; translation is not required for the MVP.
- Speech pauses/stoppages must be detected so stable utterance/sentence segments can be formed.
- Local communication uses Wi-Fi/Bluetooth-connected devices or another organizer-permitted local device path.
- Push-to-talk must support a two-phone walkie-talkie-style demonstration.
- A non-PTT/conversational mode is a later product requirement, not an excuse to weaken PTT reliability.
- Speech/TinyML dependencies used for the judged solution must satisfy the organizer's open-source constraint; proprietary cloud speech services are excluded.
- Low- and mid-range Android practicality must be demonstrated with measured latency/resource evidence, not inferred from desktop performance.
- Urgent playback must use Android-supported notification/audio behavior and must not bypass platform safety controls.

## Published scoring information currently available in project sources

- Accuracy: 40%
- Efficiency: 20%
- Latency: 20%

These values sum to 80%. The remaining scoring component is **OPEN** until the complete official statement/organizer clarification is preserved and checked. Do not invent the missing 20%.

## M0 open questions

1. What is the exact canonical organizer URL/PDF revision to archive in the repository evidence index?
2. What is the remaining evaluation category/weight beyond the currently captured 80%?
3. Does the organizer impose any exact interpretation of "open source" for model weights and training data beyond code availability?
4. Which Android versions/device classes will Syntax6 declare as its low/mid-range target envelope?
5. Which local transport will be used for the first physical Unicode-transfer gate: BLE or local Wi-Fi?

## Change-control rule

If an official source contradicts this ledger, the official source wins. Update this file, the roadmap/risk register if affected, and record the decision rather than silently preserving the old assumption.
