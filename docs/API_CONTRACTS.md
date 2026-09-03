# Internal API Contracts

## Speech recognizer

Inputs:
- audio segment;
- language;
- model profile;
- cancellation token.

Outputs:
- transcript;
- token/segment confidence if available;
- timing;
- model identity;
- error state.

## Safety validator

Inputs:
- recognition result;
- priority;
- domain profile.

Outputs:
- normalized transcript;
- critical fields;
- confirmation requirement;
- warnings.

## Protocol encoder

Input:
validated application message.

Output:
immutable canonical logical bundle bytes.

## Delivery engine

Input:
bundle + destination policy.

Output:
observable state stream.

## Router

Input:
bundle metadata + peer/contact state + resource policy.

Output:
forwarding decisions.

## TTS

Input:
verified text + language + playback policy.

Output:
audio stream / playback events + timing.
