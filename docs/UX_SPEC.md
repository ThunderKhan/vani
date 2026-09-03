# UX Specification

## Primary screen

The PTT interface should resemble a field communications instrument.

Must show:
- offline/local connectivity status;
- selected language;
- language-pack health;
- destination/group;
- priority;
- large PTT target;
- state text;
- transcript;
- critical-span emphasis;
- send/cancel/confirm.

## State vocabulary

Use explicit labels:
- Ready
- Listening
- Processing
- Check transcript
- Queued — no route
- Transferring
- Relayed
- Delivered to device
- Playing
- Acknowledged
- Expired
- Failed

Avoid generic “Sent”.

## Accessibility

- large touch target;
- haptics;
- screen-reader labels;
- text scaling;
- sufficient contrast;
- no color-only state;
- icon + text + haptic/audio redundancy;
- glove/stress-friendly workflow.

## Critical-content confirmation

Example:

`DO [NOT] ENTER [SECTOR 13] BEFORE [18:30]`

Allow:
- tap field to edit;
- replay recognized text locally;
- repeat speech;
- confirm all.

## Diagnostics

Separate engineering screen:
- model ID;
- WER fixture result;
- bundle bytes;
- stage latency;
- RTF;
- RAM;
- hops;
- retries;
- queue state.
