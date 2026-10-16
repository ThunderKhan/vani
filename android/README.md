# Android workspace

The Android tree begins with an intentionally disposable M0 probe rather than the production VĀṆI application.

## `feasibility-probe`

Purpose: execute **M0-UNICODE-LINK-001** on two physical Android phones.

It proves only this bounded statement:

> Two Android devices can exchange an arbitrary UTF-8 payload over a local Wi-Fi TCP path without a validated Internet route, verify the received bytes with SHA-256, return an acknowledgement, and record monotonic transfer evidence.

It does **not** claim:
- STT or TTS feasibility;
- BLE support;
- mesh or store-carry-forward behavior;
- encryption/authentication suitable for production;
- final semantic-bundle compatibility;
- M0 completion by itself.

## Toolchain

The project is configured for:
- Android Gradle Plugin 9.4.0;
- Gradle 9.6.0 in CI;
- JDK 17;
- compile/target SDK 36;
- minimum SDK 26;
- AGP built-in Kotlin support (no separate Kotlin Android plugin).

Android Studio Quail 4 supports AGP 9.4. If your local Android Studio asks for a Gradle distribution because this repository intentionally does not vendor the binary wrapper JAR, select/download Gradle 9.6.0 through the IDE.

## Physical two-phone run

1. Pull the exact commit you intend to test and open the repository root in Android Studio.
2. Let Gradle sync and install Android SDK Platform 36 if prompted.
3. Build/install `android:feasibility-probe` on two physical phones.
4. Disable mobile data/SIM Internet. Prefer airplane mode, then manually re-enable Wi-Fi.
5. Put both phones on the same **local-only** Wi-Fi network or hotspot. The app must show `Validated Internet available: false` at the beginning and end of the sender run.
6. On the receiver, tap **Start host on port 42424** and note a candidate IPv4 address.
7. On the sender, enter that IPv4 address. Leave the default multilingual payload or type another non-sensitive test string.
8. Tap **Send UTF-8 payload**.
9. A usable sender result requires:
   - receiver ACK = true;
   - SHA-256 integrity = true;
   - no validated Internet capability at start/end;
   - a real Git commit embedded by the Gradle build.
10. Copy the sender evidence JSON and store it under `experiments/m0/results/` in a follow-up evidence commit. Do not edit measured values except to add clearly separated operator notes.

## Why `INTERNET` permission exists

Android requires `android.permission.INTERNET` for raw TCP sockets. The probe contains no HTTP client, cloud endpoint, analytics SDK, account service, or remote API dependency. M0 offline evidence is based on the recorded network state and the physical test setup, not on the permission name.

## Protocol safety bounds

The probe frame is deliberately tiny and temporary:
- protocol version: 1;
- maximum payload: 65,536 bytes;
- device-info field: maximum 512 UTF-8 bytes;
- SHA-256 payload integrity check before acceptance;
- bounded connect/read timeouts;
- no allocation from an unbounded remote length field.

Production cryptographic authentication/encryption belongs to later milestones and must use audited primitives. Do not present this probe framing as the final VĀṆI wire protocol.

## Field UI alpha (October 2026, integration branch)

The main launcher is now `dev.syntax6.vani.m0probe.ui.VaniFieldActivity`.
It is a dependency-light native Android screen, not the disposable M0 probe.
It exposes **Inbox**, **Speak/Send**, **Links** and **Tools**, while keeping M0,
M1, M2 and M4 diagnostics accessible for final testing.

Two local peer transports can be selected:
- M4 local Wi-Fi/TCP (manually supplied private IPv4, port `42426`);
- M4 paired Bluetooth Classic RFCOMM (UUID defined in `BluetoothTransport.kt`;
  manual OS pairing, Android 12+ `BLUETOOTH_CONNECT` permission).

For either mode the receiver must be explicitly started, the recipient
identities must match, and an identical 256-bit AES-GCM key must be supplied to
the two endpoints **without saving that key**. The current UI exposes a manual
key exchange workflow only; it cannot authenticate the identity of the person
who provided the key. The receiver is foreground/process-bound.

`ui/M5DiagnosticsActivity` shows actual local message-store metadata and can
export redacted JSON through Android's document picker. It explicitly labels
missing speech/energy/relay measurements.

**This remains a field ALPHA**; it has not passed full-device validation.
Open-source offline model integration and BLE multi-hop routing are still
required for final SIH compliance. See `docs/FIELD_UI_DESIGN.md` and
`docs/FIELD_DEVICE_RUNBOOK.md`.
