# Component Catalogue

## Audio Capture
Responsibilities:
- microphone session;
- sample-format conversion;
- ring buffer;
- PTT lifecycle;
- timestamping.

Failure cases:
permission denied, audio focus lost, call interruption, microphone unavailable, buffer overrun.

## VAD + Endpointing
Responsibilities:
- low-cost speech activity;
- stable utterance finalization;
- silence thresholds;
- token stability inputs.

Metrics:
false activation, missed speech, clipping, endpoint delay.

## ASR
Interface:
```kotlin
interface SpeechRecognizer {
    suspend fun recognize(segment: AudioSegment, language: LanguageCode): RecognitionResult
}
```

Result conceptually contains transcript, normalized confidence metadata, timing, model ID, model version, and warnings.

## Critical Information Guard
Responsibilities:
- extract critical candidates;
- assign risk class;
- trigger confirmation policy;
- generate structured fields;
- preserve user corrections.

## Protocol Codec
Responsibilities:
- encode/decode;
- canonical field ordering;
- version checks;
- deterministic test vectors;
- maximum size enforcement.

## Security
Responsibilities:
- key storage;
- authentication;
- integrity;
- replay rejection;
- encryption;
- sender identity assertions.

## Delivery Store
Responsibilities:
- outbox/inbox;
- atomic state transitions;
- expiry;
- ACK correlation;
- duplicate suppression.

## Routing
Input: destination, bundle priority, expiry, copy budget, peer/contact state.  
Output: forwarding decision.

## Transport Adapter
```kotlin
interface Transport {
    val capabilities: TransportCapabilities
    fun observePeers(): Flow<PeerEvent>
    suspend fun send(peer: PeerId, frame: ByteArray): TransferResult
}
```

## TTS
Responsibilities:
- correct language voice/model;
- synthesis;
- time-to-first-audio measurement;
- pronunciation fallback;
- cancellation.

## Diagnostics
Produces:
- pipeline traces;
- bundle size;
- hop path;
- stage latency;
- model/runtime metadata;
- device resource metrics.
