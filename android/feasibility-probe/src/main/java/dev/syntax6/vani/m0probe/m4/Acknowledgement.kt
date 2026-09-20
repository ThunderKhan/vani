package dev.syntax6.vani.m0probe.m4

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.charset.StandardCharsets

data class M4Acknowledgement(
    val messageId: String,
    val state: M4Protocol.DeliveryState,
    val destinationId: String,
    val observedAtEpochMillis: Long,
)

object M4AcknowledgementCodec {
    private val magic = byteArrayOf('A'.code.toByte(), 'C'.code.toByte(), 'K'.code.toByte(), '4'.code.toByte())

    fun encode(ack: M4Acknowledgement): ByteArray {
        require(ack.messageId.isNotBlank() && ack.messageId.length <= 128)
        require(ack.destinationId.isNotBlank() && ack.destinationId.length <= 128)
        val out = ByteArrayOutputStream()
        DataOutputStream(out).use { d ->
            d.write(magic)
            writeString(d, ack.messageId)
            d.writeByte(ack.state.ordinal)
            writeString(d, ack.destinationId)
            d.writeLong(ack.observedAtEpochMillis)
        }
        return out.toByteArray()
    }

    fun decode(bytes: ByteArray): M4Acknowledgement {
        require(bytes.size <= M4Protocol.MAX_FRAME_BYTES)
        DataInputStream(ByteArrayInputStream(bytes)).use { d ->
            val actual = ByteArray(4).also(d::readFully)
            require(actual.contentEquals(magic))
            val id = readString(d)
            val state = M4Protocol.DeliveryState.values().getOrNull(d.readUnsignedByte()) ?: error("unknown delivery state")
            val destination = readString(d)
            val observed = d.readLong()
            require(d.available() == 0)
            return M4Acknowledgement(id, state, destination, observed)
        }
    }

    private fun writeString(d: DataOutputStream, value: String) {
        val b = value.toByteArray(StandardCharsets.UTF_8)
        require(b.size <= 255)
        d.writeByte(b.size)
        d.write(b)
    }

    private fun readString(d: DataInputStream): String {
        val n = d.readUnsignedByte()
        require(n <= d.available())
        return ByteArray(n).also(d::readFully).toString(StandardCharsets.UTF_8)
    }
}
