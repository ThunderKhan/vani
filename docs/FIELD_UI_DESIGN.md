# Vāṇī Field Interface — design and implementation

## Design provenance

Primary UI **reference**, not a codebase fork: [Google Android Jetchat](https://github.com/android/compose-samples/tree/main/Jetchat), from the Apache-2.0 licensed [Compose samples](https://github.com/android/compose-samples). Borrowed **interaction concepts**: readable message ledger, in-place composition, high-contrast actions, and explicit navigation/state.

Secondary **research reference**: [Meshtastic Android](https://github.com/meshtastic/Meshtastic-Android) (GPL-3.0): field-device vocabulary, peer-first connectivity model, and visible transport status. **No source code, graphics, or assets from that GPL project were copied.**

All application View code, colors, layout, typography choices and original icon in this integration were written specifically for Vāṇī. The Android Views implementation is deliberately dependency-light, preserving the existing AGP/Kotlin toolchain rather than transplanting a complete Compose sample or copying its dependencies.

## Product principles

1. Show **only real observed communication state** and locally stored ciphertext metadata.
2. Make the PTT/composition action immediately accessible.
3. Separate local transport receipt, end-to-end cryptographic acceptance, actual playback and human confirmation.
4. Never imply Internet access is required by the semantic transport.
5. Keep key material in memory only and explain manual-key trust limitations.
6. Do not put research metrics, false ASR accuracy claims or simulated nodes in the primary inbox.
7. Use large Android touch targets and readable labels on low/mid-range devices.

## Visual system

- Base: deep blue-charcoal `#0C171D`
- Surface: `#1C2D34`; panel `#15242B`; border `#384D52`
- Primary content: `#EEF7F4`; secondary: `#A3B8B7`
- Active accents: mint `#7EE2BC`
- Attention/limitations: amber `#F5C26F`
- Font: platform sans-serif / sans-serif-medium; no third-party fonts
- Touch targets: at least 48 density-independent pixels for primary actions
- Launcher: original V + radio waves adaptive icon (no third-party graphics)

## Information architecture

### Inbox
Actual inbox/outbox SQLite records. Counts for authenticated destination arrivals and queued outbound messages. Cards show source/destination, direction, priority, state, modified time, ciphertext byte count, retry count. Plaintext is never saved into this screen. Tapping a record requires the active correct key before disclosure; the user may replay using device-local TTS.

### Speak / send
Destination and current carrier, language control for ten mandated locales, editable transcript, hold-to-speak input with a tap-to-start accessibility alternative, routine/urgent selection, critical span confirmation, and explicit encrypted transmit. Distress authorization is not faked or exposed as a privileged user control.

### Links
Edit local/destination identities, choose local Wi-Fi IPv4 or already-paired Bluetooth Classic peer, configure AES-GCM shared key **in memory only**, start/stop an active listener, manually retry queued records with preserved IDs, and toggle ordinary receiver speech playback. Internet status is displayed as an observation, not as proof of a successful offline test.

### Urgent notification consent
Only explicitly enabled URGENT messages may generate a normal Android
high-importance notification. Notifications include no transcript, are rate
limited and use Android 13+ runtime `POST_NOTIFICATIONS` consent; they respect
channel settings, Do Not Disturb and user control. There is no claim of forced
maximum volume, non-interruptibility, privileged distress rights or guaranteed
attention.

### Tools
M0 Unicode feasibility, M1 prototype, M2 language research, M4 direct-link diagnostics, and M5 truthful redacted metrics/export. Tools are visibly experimental so unfinished scientific features do not masquerade as user-ready messaging.

## Interaction contracts

- No demo or dummy conversations are inserted.
- Sending a message enqueues the protected binary bundle before attempting transport.
- A receiver accepts a message into durable inbox only after authenticated AES-GCM decoding and correct destination matching.
- Adjacent-peer receipt is displayed as `TRANSFERRED`, not human acknowledgment.
- Recipients may opt into normal-volume, Android-policy-respecting TTS playback; the default is off.
- The app does not circumvent Do Not Disturb or OS safety constraints.
- Both peer configuration and current user key are required for an actual send.
- Key/identity changes require receiver restart; changing the key stops existing listeners.
- Retry only targets queued messages for the selected intended recipient.
- Receiver code is bounded for frame sizes, queue capacity and concurrent socket sessions.

## Boundaries and next engineering tasks

**Current implementation:** direct local-Wi-Fi TCP, Android-paired Bluetooth Classic RFCOMM, M4 AES-GCM payload, manual-key experimental receiver and existing M3 critical guard. The native app uses Android platform speech adapters **only as experimental capability probes**.

**NOT implemented or evidenced:** open-source ten-language STT/TTS runtimes and weights, verified phone-to-phone offline runs, BLE/GATT transport, opportunistic discovery and actual 3+ phone mesh relays, trusted pairing/key exchange, signed recipient/human ACK, long-running background service, general hands-free VAD, multi-hop queue scheduling, alert audio-policy integration, and field-model benchmarks. Those cannot be honestly inferred from the UI.

No functional/physical testing was executed during this design integration; final testing will occur in the agreed separate phase.
