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
