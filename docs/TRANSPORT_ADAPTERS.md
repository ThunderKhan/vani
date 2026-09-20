# Transport Adapter Contract

## Interface semantics

A transport moves opaque frames between peers. It does not decide message safety or final delivery.

Conceptual API:

```kotlin
interface MeshTransport {
    val id: TransportId
    val capabilities: TransportCapabilities
    fun peerEvents(): Flow<PeerEvent>
    suspend fun open(peer: PeerId): TransportSession
}

interface TransportSession {
    suspend fun send(frame: ByteArray): SendReceipt
    val incoming: Flow<ByteArray>
    suspend fun close()
}
```

## Capabilities

Record:
- max practical frame size;
- connection-oriented vs datagram-like;
- expected throughput;
- discovery mechanism;
- background restrictions;
- encryption at link layer;
- peer addressing;
- broadcast support;
- energy characteristics.

## BLE adapter

Must handle:
- permissions by Android version;
- advertising/scanning lifecycle;
- MTU negotiation;
- GATT serialization;
- reconnect;
- duplicate service discovery;
- manufacturer-specific behavior.

## Wi-Fi adapter

Must handle:
- local addressing;
- peer discovery;
- socket lifecycle;
- network changes;
- hotspot/local-network variations;
- no-internet network validation behavior.

## Hardware bridge adapter

Treat bridge as another transport. The semantic bundle does not change.


## M4 concrete interface

The Android implementation now defines a transport boundary equivalent to:

    interface MeshTransport {
        val id: String
        val capabilities: TransportCapabilities
        fun addListener(listener: (peerId: String, frame: ByteArray) -> Unit)
        fun removeListener(listener: (String, ByteArray) -> Unit)
        fun send(peerId: String, frame: ByteArray): SendReceipt
    }

M4DeliveryEngine depends on this abstraction and the persistence/routing interfaces rather than directly invoking a radio API.

The current repository does **not** claim that a BLE or Wi-Fi adapter is complete merely because this contract exists. Adapter-specific discovery, lifecycle, permissions, MTU/socket behavior, reconnection and background restrictions remain transport implementation and physical-validation work.

The semantic payload remains opaque to transports and relays. Transport adapters must not invoke STT/TTS.
