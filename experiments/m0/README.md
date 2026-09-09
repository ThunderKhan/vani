# M0 Experiment Records

M0 records prove feasibility; they do not exist to make the project look complete.

## Required first records

1. **M0-ASR-ANDROID-001** — one offline ASR candidate on a real low/mid-range Android phone.
2. **M0-TTS-ANDROID-001** — one offline TTS candidate on a real low/mid-range Android phone.
3. **M0-UNICODE-LINK-001** — two physical Android phones exchange a Unicode payload using an intended local transport with internet infrastructure unavailable.

For ASR/TTS, capture at minimum:
- exact model artifact and SHA-256;
- runtime/version and thread count;
- cold and warm latency;
- model size;
- peak/observed memory method and result;
- device/SoC/RAM/Android version;
- proof that remote APIs were not used.

For the Unicode link, capture at minimum:
- sender/receiver device profiles;
- transport and local-only topology;
- exact UTF-8 payload and byte count (do not store sensitive real messages);
- transfer start/end monotonic timing;
- receive integrity/hash check;
- failure/retry notes;
- explicit internet-blocked condition.

Use `first-device-run.template.json` as a starting point and validate completed records against `../schema/experiment-record.schema.json` where tooling permits.
