# M6 — Competition product implementation boundary

**State: not competition-ready.** This file is a truthful gap register, not
a declaration that the SIH 2026 product is complete.

Integrated in the current branch:
- M1 direct Wi-Fi screen with corrected launcher entry and scrollable layout;
- explicit selection among the ten SIH language locales;
- manual editable transcript input as well as experimental Android speech capture;
- M2 per-language capability research screen;
- M3 critical-content confirmation in the M1 path;
- M4 core protocol, store, safety metadata, routing primitives and local-Wi-Fi
  binary frame carrier (separate from the M1 screen);
- M5 evidence scoring and input schema.

Required before a truthful competition-ready declaration:
- independently verified **open-source** STT and TTS model/runtime stack for
  all ten required languages; audit weights, code, data and redistributability;
- wire up M4 authenticated delivery, key provisioning and signed/validated ACK
  semantics to the user-facing voice loop; M1 still uses unauthenticated JSON/TCP;
- real BLE connectivity and radio state/permissions, reconnection and relay;
- three-device store-carry-forward wiring and recovery;
- reliable hands-free pause/endpointer conversation mode;
- priority alert playback subject to Android safety controls;
- user onboarding, consent, pair/identity management, inbox, replay,
  incident/group UI, accessibility and lifecycle/background service;
- measured performance and complete documented final validation;
- release artifacts, scripted demo, failure recordings, pitch and judge Q&A.

Do not call platform-recognizer availability an open-source model license.
Do not call an M4 adjacent-peer receipt destination delivery. 
All physical, speech and security claims await the final verification stage.
