package dev.syntax6.vani.m0probe

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.security.MessageDigest
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicBoolean

class ProbeServer(
    private val port: Int,
    private val responderInfo: String,
    private val onReceived: (ReceiveResult) -> Unit,
    private val onError: (Throwable) -> Unit,
) {
    data class ReceiveResult(
        val receivedAtNanos: Long,
        val senderInfo: String,
        val payload: ByteArray,
        val sha256Hex: String,
        val remoteAddress: String,
    )

    private val running = AtomicBoolean(false)
    @Volatile private var serverSocket: ServerSocket? = null

    fun start(executor: ExecutorService) {
        if (!running.compareAndSet(false, true)) return
        executor.execute {
            try {
                ServerSocket(port).use { server ->
                    server.reuseAddress = true
                    serverSocket = server
                    while (running.get()) {
                        val socket = try {
                            server.accept()
                        } catch (e: IOException) {
                            if (running.get()) throw e else break
                        }
                        handleClient(socket)
                    }
                }
            } catch (t: Throwable) {
                if (running.get()) onError(t)
            } finally {
                serverSocket = null
                running.set(false)
            }
        }
    }

    fun stop() {
        running.set(false)
        try {
            serverSocket?.close()
        } catch (_: IOException) {
        }
    }

    fun isRunning(): Boolean = running.get()

    private fun handleClient(socket: Socket) {
        socket.use { client ->
            client.soTimeout = SOCKET_TIMEOUT_MS
            val input = DataInputStream(BufferedInputStream(client.getInputStream()))
            val output = DataOutputStream(BufferedOutputStream(client.getOutputStream()))
            val frame = LinkProtocol.readDataFrame(input)
            val receivedAtNanos = System.nanoTime()

            LinkProtocol.writeAckFrame(
                output = output,
                accepted = true,
                receiverElapsedNanos = receivedAtNanos,
                receiverInfo = responderInfo,
                payloadLength = frame.payload.size,
                sha256 = frame.sha256,
            )

            onReceived(
                ReceiveResult(
                    receivedAtNanos = receivedAtNanos,
                    senderInfo = frame.senderInfo,
                    payload = frame.payload,
                    sha256Hex = LinkProtocol.bytesToHex(frame.sha256),
                    remoteAddress = client.inetAddress?.hostAddress ?: "unknown",
                )
            )
        }
    }

    companion object {
        const val SOCKET_TIMEOUT_MS = 8_000
    }
}

object ProbeClient {
    data class SendResult(
        val startedAtNanos: Long,
        val finishedAtNanos: Long,
        val payloadBytes: Int,
        val payloadSha256Hex: String,
        val receiverElapsedNanos: Long,
        val receiverInfo: String,
        val receiverAccepted: Boolean,
        val integrityMatched: Boolean,
    ) {
        val roundTripMillis: Double
            get() = (finishedAtNanos - startedAtNanos) / 1_000_000.0
    }

    @Throws(IOException::class)
    fun send(
        host: String,
        port: Int,
        senderInfo: String,
        payload: ByteArray,
    ): SendResult {
        require(payload.size <= LinkProtocol.MAX_PAYLOAD_BYTES)
        val expectedHash = LinkProtocol.sha256(payload)
        val startedAtNanos = System.nanoTime()

        Socket().use { socket ->
            socket.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT_MS)
            socket.soTimeout = ProbeServer.SOCKET_TIMEOUT_MS
            val output = DataOutputStream(BufferedOutputStream(socket.getOutputStream()))
            val input = DataInputStream(BufferedInputStream(socket.getInputStream()))

            LinkProtocol.writeDataFrame(
                output = output,
                senderElapsedNanos = startedAtNanos,
                senderInfo = senderInfo,
                payload = payload,
            )
            val ack = LinkProtocol.readAckFrame(input)
            val finishedAtNanos = System.nanoTime()
            val integrityMatched = ack.payloadLength == payload.size &&
                MessageDigest.isEqual(expectedHash, ack.sha256)

            return SendResult(
                startedAtNanos = startedAtNanos,
                finishedAtNanos = finishedAtNanos,
                payloadBytes = payload.size,
                payloadSha256Hex = LinkProtocol.bytesToHex(expectedHash),
                receiverElapsedNanos = ack.receiverElapsedNanos,
                receiverInfo = ack.receiverInfo,
                receiverAccepted = ack.accepted,
                integrityMatched = integrityMatched,
            )
        }
    }

    private const val CONNECT_TIMEOUT_MS = 5_000
}
