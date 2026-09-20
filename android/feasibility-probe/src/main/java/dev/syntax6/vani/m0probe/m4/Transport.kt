package dev.syntax6.vani.m4

data class PeerId(val value: String)
data class TransportCapabilities(val maxFrameBytes: Int, val connectionOriented: Boolean, val localOnly: Boolean)
data class TransferReceipt(val peer: PeerId, val frameCount: Int, val accepted: Boolean, val detail: String? = null)

interface M4Transport {
    val id: String
    val capabilities: TransportCapabilities
    fun observePeers(): Sequence<PeerId>
    fun send(peer: PeerId, frames: List<ByteArray>): TransferReceipt
}

interface M4RelayLink {
    fun forward(peer: PeerId, frames: List<ByteArray>): TransferReceipt
}
