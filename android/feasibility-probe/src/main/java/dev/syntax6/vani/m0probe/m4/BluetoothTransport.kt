package dev.syntax6.vani.m4

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Paired-device Bluetooth Classic RFCOMM transport for encrypted M4 bundles.
 *
 * This is not BLE or Bluetooth SIG Mesh. Android system pairing occurs outside
 * the app. The transport receipt means *adjacent RFCOMM peer acceptance only*.
 * The M4 end-to-end AES-GCM validation happens in the destination callback.
 */
@Suppress("DEPRECATION")
class M4BluetoothTransport(context: Context) : M4Transport {
    private val adapter: BluetoothAdapter =
        BluetoothAdapter.getDefaultAdapter() ?: throw IllegalStateException("Bluetooth unsupported")

    override val id: String = "bluetooth-rfcomm-m4"
    override val capabilities = TransportCapabilities(
        maxFrameBytes = M4FrameCodec.MAX_FRAME_BYTES,
        connectionOriented = true,
        localOnly = true,
    )

    override fun observePeers(): Sequence<PeerId> =
        adapter.bondedDevices.orEmpty().asSequence()
            .map { PeerId(it.address) }

    fun bondedPeers(): List<Pair<String, String>> =
        adapter.bondedDevices.orEmpty()
            .map { (it.name ?: "Paired device") to it.address }
            .sortedBy { it.first }

    override fun send(peer: PeerId, frames: List<ByteArray>): TransferReceipt {
        require(adapter.isEnabled) { "Enable Bluetooth on this device" }
        require(BluetoothAdapter.checkBluetoothAddress(peer.value)) {
            "Select a valid paired Bluetooth hardware address"
        }
        requireValidFrames(frames)
        val device = adapter.getRemoteDevice(peer.value)
        require(device.bondState == android.bluetooth.BluetoothDevice.BOND_BONDED) {
            "Device must first be paired in Android Bluetooth settings"
        }
        device.createRfcommSocketToServiceRecord(SERVICE_UUID).use { socket ->
            socket.connect()
            val out = DataOutputStream(socket.outputStream.buffered())
            val input = DataInputStream(socket.inputStream.buffered())
            out.writeInt(MAGIC)
            out.writeInt(frames.size)
            frames.forEach {
                out.writeInt(it.size)
                out.write(it)
            }
            out.flush()
            require(input.readInt() == ACK_MAGIC) { "invalid Bluetooth peer receipt" }
            val accepted = input.readBoolean()
            return TransferReceipt(
                peer, frames.size, accepted,
                if (accepted) "paired peer accepted bundle (not human ACK)"
                else "paired peer rejected bundle",
            )
        }
    }

    class Server(
        private val onCompleteBundle: (ByteArray) -> Boolean,
        private val onStatus: (String) -> Unit = {},
    ) {
        private val running = AtomicBoolean(false)
        @Volatile private var listener: BluetoothServerSocket? = null
        private val peers = mutableSetOf<BluetoothSocket>()

        fun start(executor: ExecutorService) {
            if (!running.compareAndSet(false, true)) return
            executor.execute {
                try {
                    val adapter = BluetoothAdapter.getDefaultAdapter()
                        ?: throw IllegalStateException("Bluetooth not supported")
                    check(adapter.isEnabled) { "Bluetooth is switched off" }
                    adapter.listenUsingRfcommWithServiceRecord(SERVICE_NAME, SERVICE_UUID).use { server ->
                        listener = server
                        onStatus("Paired-device Bluetooth receiver is listening")
                        while (running.get()) {
                            val socket = server.accept()
                            synchronized(peers) { peers.add(socket) }
                            executor.execute {
                                try { handle(socket) }
                                finally { synchronized(peers) { peers.remove(socket) } }
                            }
                        }
                    }
                } catch (error: Exception) {
                    if (running.get()) onStatus("Bluetooth listener unavailable: ${error.message}")
                } finally {
                    running.set(false)
                    listener = null
                }
            }
        }

        fun stop() {
            running.set(false)
            runCatching { listener?.close() }
            synchronized(peers) {
                peers.forEach { runCatching { it.close() } }
                peers.clear()
            }
            listener = null
        }

        fun isRunning(): Boolean = running.get()

        private fun handle(socket: BluetoothSocket) {
            socket.use { session ->
                val out = DataOutputStream(session.outputStream.buffered())
                val accepted = try {
                    val input = DataInputStream(session.inputStream.buffered())
                    require(input.readInt() == MAGIC) { "invalid Bluetooth M4 frame magic" }
                    val count = input.readInt()
                    require(count in 1..M4_MAX_FRAGMENTS) { "invalid number of Bluetooth frames" }
                    val reassembler = FragmentReassembler(maxPendingMessages = 1)
                    var messageId: UUID? = null
                    var complete: ByteArray? = null
                    var totalBytes = 0
                    repeat(count) { index ->
                        val length = input.readInt()
                        require(length in 1..M4FrameCodec.MAX_FRAME_BYTES) { "invalid Bluetooth frame length" }
                        val frameBytes = ByteArray(length)
                        input.readFully(frameBytes)
                        val frame = M4FrameCodec.decode(frameBytes)
                        require(messageId == null || messageId == frame.messageId) { "mixed message IDs" }
                        require(frame.fragmentCount == count && frame.fragmentIndex == index) {
                            "invalid fragment sequence"
                        }
                        messageId = frame.messageId
                        totalBytes += frame.payload.size
                        require(totalBytes <= M4_MAX_BUNDLE_BYTES) { "Bluetooth transfer limit exceeded" }
                        complete = reassembler.accept(frameBytes) ?: complete
                    }
                    onCompleteBundle(complete ?: error("incomplete Bluetooth message"))
                } catch (_: Exception) { false }
                runCatching {
                    out.writeInt(ACK_MAGIC)
                    out.writeBoolean(accepted)
                    out.flush()
                }
            }
        }
    }

    companion object {
        private const val MAGIC = 0x56414e42 // VANB
        private const val ACK_MAGIC = 0x414e4231 // ANB1
        private const val SERVICE_NAME = "VANI semantic bundles"
        val SERVICE_UUID: UUID = UUID.fromString("8175a028-980a-48aa-bad9-75e2b6a6d46e")

        private fun requireValidFrames(frames: List<ByteArray>) {
            require(frames.size in 1..M4_MAX_FRAGMENTS) { "invalid frame count" }
            val decoded = frames.map(M4FrameCodec::decode)
            require(decoded.map { it.messageId }.distinct().size == 1)
            require(decoded.all { it.fragmentCount == frames.size })
            require(decoded.map { it.fragmentIndex }.sorted() == frames.indices.toList())
            require(decoded.sumOf { it.payload.size } == decoded.first().totalLength)
        }
    }
}
