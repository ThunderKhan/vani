package dev.syntax6.vani.m0probe.m1

import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.security.MessageDigest
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicBoolean

/** Direct local transport for the M1 feasibility slice. */
object M1Transport {
    const val PORT = 42425
    private const val MAGIC = 0x56314E49 // V1NI
    private const val ACK = 0x41434B31 // ACK1
    private const val HASH_BYTES = 32
    private const val CONNECT_TIMEOUT_MS = 4_000
    private const val READ_TIMEOUT_MS = 8_000

    data class SendResult(
        val messageId: String,
        val transferred: Boolean,
        val delivered: Boolean,
        val acknowledged: Boolean,
        val duplicate: Boolean,
        val bundleBytes: Int,
        val endToEndMillis: Long,
        val ttsFirstAudioMillis: Long?,
        val error: String? = null,
    )

    fun send(host: String, bundle: ByteArray, messageId: String): SendResult {
        require(bundle.size in 1..M1Bundle.MAX_BYTES) { "invalid bundle size" }
        require(messageId.isNotBlank() && messageId.length <= 128) { "invalid message id" }
        val expectedHash = sha256(bundle)
        val start = System.nanoTime()
        Socket().use { socket ->
            socket.connect(InetSocketAddress(host, PORT), CONNECT_TIMEOUT_MS)
            socket.soTimeout = READ_TIMEOUT_MS
            val out = DataOutputStream(socket.getOutputStream().buffered())
            val input = DataInputStream(socket.getInputStream().buffered())
            out.writeInt(MAGIC)
            out.writeInt(bundle.size)
            out.writeUTF(messageId)
            out.write(bundle)
            out.write(expectedHash)
            out.flush()

            require(input.readInt() == ACK) { "invalid ACK magic" }
            val accepted = input.readBoolean()
            val duplicate = input.readBoolean()
            val id = input.readUTF()
            require(id == messageId) { "ACK message id mismatch" }
            val ackHash = ByteArray(HASH_BYTES)
            input.readFully(ackHash)
            require(MessageDigest.isEqual(expectedHash, ackHash)) { "ACK SHA-256 mismatch" }
            val ttsFirstAudioMillis = input.readLong().let { if (it < 0) null else it }
            val end = System.nanoTime()
            return SendResult(
                messageId = messageId,
                transferred = true,
                delivered = accepted,
                acknowledged = true,
                duplicate = duplicate,
                bundleBytes = bundle.size,
                endToEndMillis = (end - start) / 1_000_000,
                ttsFirstAudioMillis = ttsFirstAudioMillis,
            )
        }
    }

    class Server(private val handler: (M1Message, Int) -> ReceiveAck) {
        private val running = AtomicBoolean(false)
        @Volatile private var socket: ServerSocket? = null

        data class ReceiveAck(val accepted: Boolean, val duplicate: Boolean, val ttsFirstAudioMillis: Long?)

        fun start(executor: ExecutorService) {
            if (!running.compareAndSet(false, true)) return
            executor.execute {
                try {
                    ServerSocket(PORT).use { serverSocket ->
                        socket = serverSocket
                        while (running.get()) {
                            val client = serverSocket.accept()
                            executor.execute { handle(client) }
                        }
                    }
                } catch (_: Exception) {
                    // Closing the socket is the normal stop path; the activity owns user-facing status.
                } finally {
                    running.set(false)
                }
            }
        }

        fun stop() {
            running.set(false)
            runCatching { socket?.close() }
            socket = null
        }

        fun isRunning(): Boolean = running.get()

        private fun handle(socket: Socket) {
            socket.use { client ->
                client.soTimeout = READ_TIMEOUT_MS
                val input = DataInputStream(client.getInputStream().buffered())
                val output = DataOutputStream(client.getOutputStream().buffered())
                try {
                    require(input.readInt() == MAGIC) { "invalid M1 frame" }
                    val size = input.readInt()
                    require(size in 1..M1Bundle.MAX_BYTES) { "invalid bundle size" }
                    val id = input.readUTF()
                    require(id.isNotBlank() && id.length <= 128) { "invalid message id" }
                    val bytes = ByteArray(size)
                    input.readFully(bytes)
                    val expectedHash = ByteArray(HASH_BYTES)
                    input.readFully(expectedHash)
                    val actualHash = sha256(bytes)
                    require(MessageDigest.isEqual(expectedHash, actualHash)) { "bundle SHA-256 mismatch" }
                    val decoded = M1Bundle.decode(bytes)
                    require(decoded.message.id == id) { "frame/bundle id mismatch" }
                    val ack = handler(decoded.message, bytes.size)
                    writeAck(output, ack.accepted, ack.duplicate, decoded.message.id, actualHash, ack.ttsFirstAudioMillis)
                } catch (_: Exception) {
                    runCatching { writeAck(output, false, false, "", ByteArray(HASH_BYTES), null) }
                }
            }
        }
    }

    private fun writeAck(
        output: DataOutputStream,
        accepted: Boolean,
        duplicate: Boolean,
        messageId: String,
        sha256: ByteArray,
        ttsFirstAudioMillis: Long?,
    ) {
        require(sha256.size == HASH_BYTES)
        output.writeInt(ACK)
        output.writeBoolean(accepted)
        output.writeBoolean(duplicate)
        output.writeUTF(messageId)
        output.write(sha256)
        output.writeLong(ttsFirstAudioMillis ?: -1L)
        output.flush()
    }

    private fun sha256(bytes: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(bytes)
}
