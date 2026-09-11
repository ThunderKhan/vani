package dev.syntax6.vani.m0probe.m1

import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicBoolean

object M1Transport {
    const val PORT = 42425
    private const val MAGIC = 0x56314E49 // V1NI
    private const val ACK = 0x41434B31 // ACK1
    private const val CONNECT_TIMEOUT_MS = 4_000
    private const val READ_TIMEOUT_MS = 8_000

    data class SendResult(
        val messageId: String,
        val transferred: Boolean,
        val delivered: Boolean,
        val acknowledged: Boolean,
        val duplicate: Boolean,
        val bundleBytes: Int,
        val transportMillis: Long,
        val ttsFirstAudioMillis: Long?,
        val endToEndMillis: Long,
        val error: String? = null,
    )

    data class ReceiveResult(
        val message: M1Message,
        val bundleBytes: Int,
        val duplicate: Boolean,
        val receiverReceivedNanos: Long,
    )

    fun send(host: String, bundle: ByteArray, messageId: String): SendResult {
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
            out.flush()

            val ackMagic = input.readInt()
            require(ackMagic == ACK) { "invalid ACK magic" }
            val accepted = input.readBoolean()
            val duplicate = input.readBoolean()
            val id = input.readUTF()
            require(id == messageId) { "ACK message id mismatch" }
            val ttsFirstAudioMillis = input.readLong().let { if (it < 0) null else it }
            val end = System.nanoTime()
            return SendResult(
                messageId = messageId,
                transferred = true,
                delivered = accepted,
                acknowledged = true,
                duplicate = duplicate,
                bundleBytes = bundle.size,
                transportMillis = (end - start) / 1_000_000,
                ttsFirstAudioMillis = ttsFirstAudioMillis,
                endToEndMillis = (end - start) / 1_000_000,
            )
        }
    }

    class Server(private val handler: (M1Message, Int) -> ReceiveAck) {
        private val running = AtomicBoolean(false)
        private var socket: ServerSocket? = null

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
                    // Stop/close is an expected path. The UI reports failure through lifecycle state.
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
                    require(id.length <= 128) { "message id too long" }
                    val bytes = ByteArray(size)
                    input.readFully(bytes)
                    val decoded = M1Bundle.decode(bytes)
                    require(decoded.message.id == id) { "frame/bundle id mismatch" }
                    val ack = handler(decoded.message, bytes.size)
                    output.writeInt(ACK)
                    output.writeBoolean(ack.accepted)
                    output.writeBoolean(ack.duplicate)
                    output.writeUTF(decoded.message.id)
                    output.writeLong(ack.ttsFirstAudioMillis ?: -1L)
                    output.flush()
                } catch (_: Exception) {
                    runCatching {
                        output.writeInt(ACK)
                        output.writeBoolean(false)
                        output.writeBoolean(false)
                        output.writeUTF("")
                        output.writeLong(-1L)
                        output.flush()
                    }
                }
            }
        }
    }
}
