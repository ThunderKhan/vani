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

## 17 October field UI + Bluetooth increment (separate integration branch)

Added an original native Android Views product shell, inspired by Google Jetchat
(Apache-2.0 **design interaction reference**, no source copied) and off-grid
device-status conventions. There are now Inbox, Speak/Send, Links and Tools pages,
and the app launcher opens this shell. Other research activities remain accessible.

Source additions:
- `ui/VaniFieldActivity.kt`: high-contrast responsive Android layout with
  persisted non-secret peer preferences, runtime-only AES key, PTT input,
  language selector, M3 confirmation, M4 encrypted send, user-controlled TTS
  replay, actual ciphertext inbox ledger, and manual recipient-scoped retry;
- `m4/BluetoothTransport.kt`: bounded paired Bluetooth Classic RFCOMM frame
  exchange with adjacent-peer receipts, **not BLE or Bluetooth Mesh**;
- `ui/M5DiagnosticsActivity.kt`: actual local delivery counts and redacted
  user-initiated JSON export; no invented model/network timings;
- adaptive Vāṇī launcher icon; Android manifest permissions/activities;
- `docs/FIELD_UI_DESIGN.md` and `docs/FIELD_DEVICE_RUNBOOK.md`.

Remaining competition blockers are unchanged in principle: verified and
licensable open-source speech weights/runtimes for all ten languages; dedicated
VAD/endpointing and accessible conversation mode; BLE/GATT and automatic
multi-hop/DTN relaying; authenticated device/person receipts and trusted key
onboarding; lifecycle-safe background receiving; real performance and physical
human-comprehension evidence; safety/priority authorization and final UX
validation. A working Bluetooth Classic direct carrier does not satisfy BLE
mesh/multi-hop evidence. No new physical testing was performed.

Urgent receipt opt-in added with Android channel/runtime permission, redacted
notification body, user setting and process-local alert rate limiting. This
is not guaranteed presentation under OS policies, not forced-volume speech,
and not privileged distress broadcast authorization.
