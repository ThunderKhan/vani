package dev.syntax6.vani.m4

import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID
import java.util.concurrent.Semaphore
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manually addressed local Wi-Fi TCP carrier for M4 frames.
 *
 * A successful receipt means this adjacent peer accepted the COMPLETE FRAME SET,
 * not that the destination authenticated, played, or acknowledged the message.
 * End-to-end authenticity is the responsibility of MessageProtector + delivery.
 * This does not perform discovery, Wi-Fi Direct pairing, or BLE communication.
 */
class M4LocalWifiTransport : M4Transport {
    override val id: String = "local-wifi-tcp-m4"
    override val capabilities = TransportCapabilities(
        maxFrameBytes = M4FrameCodec.MAX_FRAME_BYTES,
        connectionOriented = true,
        localOnly = true,
    )

    override fun observePeers(): Sequence<PeerId> = emptySequence() // Explicit IPv4 peer selection.

    override fun send(peer: PeerId, frames: List<ByteArray>): TransferReceipt {
        requireIpv4(peer.value)
        validateFrames(frames)
        Socket().use { socket ->
            socket.connect(InetSocketAddress(peer.value, PORT), CONNECT_TIMEOUT_MS)
            socket.soTimeout = READ_TIMEOUT_MS
            val output = DataOutputStream(socket.getOutputStream().buffered())
            output.writeInt(MAGIC)
            output.writeInt(frames.size)
            frames.forEach { frame ->
                output.writeInt(frame.size)
                output.write(frame)
            }
            output.flush()
            val input = DataInputStream(socket.getInputStream().buffered())
            require(input.readInt() == RECEIPT_MAGIC) { "invalid M4 peer receipt" }
            val accepted = input.readBoolean()
            return TransferReceipt(peer, frames.size, accepted,
                if (accepted) "peer accepted frames (not destination delivery)" else "peer rejected frames")
        }
    }

    class Server(private val onCompleteBundle: (ByteArray) -> Boolean) {
        private val running = AtomicBoolean(false)
        private val permits = Semaphore(4)
        @Volatile private var serverSocket: ServerSocket? = null

        fun start(executor: ExecutorService) {
            if (!running.compareAndSet(false, true)) return
            executor.execute {
                try {
                    ServerSocket(PORT).use { server ->
                        serverSocket = server
                        while (running.get()) {
                            val client = server.accept()
                            if (!permits.tryAcquire()) {
                                client.close()
                                continue
                            }
                            executor.execute {
                                try { handle(client) } finally { permits.release() }
                            }
                        }
                    }
                } catch (_: java.io.IOException) {
                    // A deliberate socket close during stop() is normal.
                } finally {
                    serverSocket = null
                    running.set(false)
                }
            }
        }

        fun stop() {
            running.set(false)
            runCatching { serverSocket?.close() }
            serverSocket = null
        }

        fun isRunning(): Boolean = running.get()

        private fun handle(socket: Socket) {
            socket.use { client ->
                client.soTimeout = READ_TIMEOUT_MS
                val output = DataOutputStream(client.getOutputStream().buffered())
                val accepted = try {
                    val input = DataInputStream(client.getInputStream().buffered())
                    require(input.readInt() == MAGIC) { "invalid M4 carrier header" }
                    val count = input.readInt()
                    require(count in 1..M4_MAX_FRAGMENTS) { "invalid M4 frame count" }
                    val reassembler = FragmentReassembler(maxPendingMessages = 1)
                    var totalPayloadBytes = 0
                    var completed: ByteArray? = null
                    var receivedId: UUID? = null
                    repeat(count) { index ->
                        val length = input.readInt()
                        require(length in 1..M4FrameCodec.MAX_FRAME_BYTES) { "invalid frame length" }
                        val bytes = ByteArray(length)
                        input.readFully(bytes)
                        val frame = M4FrameCodec.decode(bytes)
                        require(receivedId == null || frame.messageId == receivedId) { "mixed message IDs" }
                        receivedId = frame.messageId
                        require(frame.fragmentCount == count) { "fragment count mismatch" }
                        require(frame.fragmentIndex == index) { "out-of-order carrier frame" }
                        totalPayloadBytes += frame.payload.size
                        require(totalPayloadBytes <= M4_MAX_BUNDLE_BYTES) { "carrier payload budget exceeded" }
                        completed = reassembler.accept(bytes) ?: completed
                    }
                    val bundle = completed ?: error("incomplete fragment set")
                    onCompleteBundle(bundle)
                } catch (_: Exception) {
                    false
                }
                runCatching {
                    output.writeInt(RECEIPT_MAGIC)
                    output.writeBoolean(accepted)
                    output.flush()
                }
            }
        }
    }

    companion object {
        const val PORT = 42426
        private const val MAGIC = 0x56345746 // V4WF
        private const val RECEIPT_MAGIC = 0x41345746 // A4WF
        private const val CONNECT_TIMEOUT_MS = 4_000
        private const val READ_TIMEOUT_MS = 15_000

        private fun requireIpv4(host: String) {
            val parts = host.split('.')
            require(parts.size == 4 && parts.all { part ->
                part.isNotEmpty() && part.length <= 3 &&
                    part.all(Char::isDigit) && (part.toIntOrNull()?.let { it in 0..255 } == true)
            }) { "manual local peer must be a numeric IPv4 address" }
            require(host != "0.0.0.0" && host != "255.255.255.255") { "invalid peer address" }
            val partsNumeric = parts.map { it.toInt() }
            val first = partsNumeric[0]
            val second = partsNumeric[1]
            val siteLocal = first == 10 || (first == 172 && second in 16..31) ||
                (first == 192 && second == 168) || (first == 169 && second == 254)
            require(siteLocal) { "only RFC1918 or link-local IPv4 peers are supported" }
        }

        private fun validateFrames(frames: List<ByteArray>) {
            require(frames.size in 1..M4_MAX_FRAGMENTS) { "invalid frame count" }
            val decoded = frames.map(M4FrameCodec::decode)
            require(decoded.map { it.messageId }.distinct().size == 1) { "mixed message IDs" }
            require(decoded.all { it.fragmentCount == frames.size }) { "missing frame" }
            require(decoded.map { it.fragmentIndex }.sorted() == frames.indices.toList()) {
                "duplicate/missing fragment index"
            }
            require(decoded.sumOf { it.payload.size } == decoded.first().totalLength) {
                "fragment data length mismatch"
            }
        }
    }
}
