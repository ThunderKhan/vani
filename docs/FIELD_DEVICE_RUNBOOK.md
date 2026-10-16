# Field implementation runbook (for deferred testing stage)

**Do not mark this guide as completed evidence.** It records the intended operator flow so final device trials are reproducible.

## Prepare

1. Build `android:feasibility-probe` from the exact tested git commit with JDK 17, SDK 36 and configured Gradle.
2. Install Vāṇī on two Android phones, preferably one constrained device. Do not assume emulator radio behavior.
3. Launch the new Inbox. It should not show any fabricated messages.
4. Open **Links** on both devices. Note the local node identity on each and enter the other as recipient identity.
5. Generate a random AES session key on one phone and transfer it to the other **out of band, securely**; enter it manually on the receiver. No in-app trusted pairing exists.
6. For Wi-Fi, connect both devices to the same reachable local-only network and enter the receiver's listed candidate private IPv4 address. For Bluetooth, pair in Android settings, grant Nearby Devices if requested, and select the bonded peer by address.
7. Start receiving on the intended destination, keeping the application in the foreground. Background radio operation is not yet claimed.

## Send

1. Switch to **Speak / send**.
2. Choose a requested language, capture an experimental Android speech transcript or type the exact message manually.
3. Review numbers, negations, coordinates and names. Confirm safety prompts where shown.
4. Send a routine or urgent message and observe that a local queue record was created.
5. A successful adjacent-peer receipt advances only to `TRANSFERRED`; this is **not** authenticated person acknowledgement. Receiver-side acceptance must be inspected separately.
6. In **Inbox**, select the actual received item and load the correct key to view/replay locally. Auto playback is opt-in.
7. If transfer is not accepted, use **Retry queued messages** with the *same destination*, preserving the message ID and bundle bytes.

## Evidence phase (not yet executed)

- With internet blocked, verify physical STT/TTS availability for every language individually.
- Ensure the intended device speech engine's exact weights/runtime and licences meet official open-source requirements.
- Record two-phone Wi-Fi and Bluetooth success, queue expiry, retry, duplicates, speaker privacy, audio playback and no-internet operation.
- Confirm that byte counts represent real transmitted payloads and that timed metrics use named start/end markers.
- Evaluate M4 malformed-frame, replay, key-rotation and blocked-contact behavior.
- Run three-phone line / partition-and-merge topology **only after real relay code has been implemented**.
- Record model/device/transport versions, power use, held-out ASR WER/CER, TTS listener transcription and all negative outcomes.
- Export redacted metadata using **Tools → Delivery evidence**. It does not replace the full experimental result schema.

## Security warnings

- Copying a shared key to Android clipboard can expose it: the user must clear it.
- Existing queued ciphertext is not automatically re-encrypted when a key changes.
- The current receiver is only active while the app process/listener is alive.
- Neither ordinary TCP nor RFCOMM receipt is cryptographically signed by a human recipient.
- Bluetooth Classic is not BLE Mesh, and manual peer selection is not opportunistic routing.
