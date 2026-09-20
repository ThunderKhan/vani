package dev.syntax6.vani.m0probe.m4

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.zip.CRC32

data class M4Frame(
    val type: M4Protocol.FrameType,
    val messageId: String,
    val destinationId: String,
    val priority: M4Protocol.Priority,
    val expiresAtEpochMillis: Long,
    val routingTag: ByteArray,
    val totalBytes: Int,
    val hopLimit: Int,
    val copyBudget: Int,
    val fragmentIndex: Int,
    val fragmentCount: Int,
    val payload: ByteArray,
)

object M4FrameCodec {
    private val magic = byteArrayOf('V'.code.toByte(), 'F'.code.toByte(), '4'.code.toByte(), 1)
    private const val HEADER_BYTES = 4 + 1 + 1 + 1 + 1 + 2 + 2 + 2 + 2 + 4
    private const val MAX_MESSAGE_ID_BYTES = 128

    fun encode(frame: M4Frame): ByteArray {
        require(frame.messageId.isNotBlank() && frame.messageId.toByteArray().size <= MAX_MESSAGE_ID_BYTES)
        require(frame.destinationId.isNotBlank() && frame.destinationId.toByteArray().size <= MAX_MESSAGE_ID_BYTES)
        require(frame.expiresAtEpochMillis >= 0)
        require(frame.routingTag.size == 32)
        require(frame.totalBytes in 1..M4Protocol.MAX_REASSEMBLY_BYTES)
        require(frame.hopLimit in 0..M4Protocol.MAX_HOP_LIMIT)
        require(frame.copyBudget in 0..M4Protocol.MAX_COPY_BUDGET)
        require(frame.fragmentCount in 1..M4Protocol.MAX_FRAGMENT_COUNT)
        require(frame.fragmentIndex in 0 until frame.fragmentCount)
        require(frame.payload.size <= M4Protocol.MAX_FRAME_BYTES - HEADER_BYTES - MAX_MESSAGE_ID_BYTES)
        val out = ByteArrayOutputStream()
        DataOutputStream(out).use { d ->
            d.write(magic)
            d.writeByte(frame.type.wire)
            d.writeByte(frame.hopLimit)
            d.writeByte(frame.copyBudget)
            d.writeShort(frame.fragmentIndex)
            d.writeShort(frame.fragmentCount)
            val id = frame.messageId.toByteArray(Charsets.UTF_8)
            val destination = frame.destinationId.toByteArray(Charsets.UTF_8)
            d.writeByte(id.size)
            d.write(id)
            d.writeByte(destination.size)
            d.write(destination)
            d.writeByte(frame.priority.wire)
            d.writeLong(frame.expiresAtEpochMillis)
            d.write(frame.routingTag)
            d.writeInt(frame.totalBytes)
            d.writeInt(frame.payload.size)
            d.write(frame.payload)
            val crc = CRC32().apply { update(frame.payload) }.value
            d.writeInt(crc.toInt())
        }
        return out.toByteArray().also { require(it.size <= M4Protocol.MAX_FRAME_BYTES) }
    }

    fun decode(bytes: ByteArray): M4Frame {
        require(bytes.size in HEADER_BYTES..M4Protocol.MAX_FRAME_BYTES) { "invalid frame size" }
        DataInputStream(ByteArrayInputStream(bytes)).use { d ->
            val m = ByteArray(4).also(d::readFully)
            require(m.contentEquals(magic)) { "invalid frame magic" }
            val type = M4Protocol.FrameType.values().firstOrNull { it.wire == d.readUnsignedByte() } ?: error("unknown frame type")
            val hop = d.readUnsignedByte()
            val copies = d.readUnsignedByte()
            val index = d.readUnsignedShort()
            val count = d.readUnsignedShort()
            require(count in 1..M4Protocol.MAX_FRAGMENT_COUNT && index < count)
            val idLength = d.readUnsignedByte()
            require(idLength in 1..MAX_MESSAGE_ID_BYTES)
            val idBytes = ByteArray(idLength).also(d::readFully)
            val id = idBytes.toString(Charsets.UTF_8)
            require(id.isNotBlank())
            val destinationLength = d.readUnsignedByte()
            require(destinationLength in 1..MAX_MESSAGE_ID_BYTES)
            val destinationBytes = ByteArray(destinationLength).also(d::readFully)
            val destination = destinationBytes.toString(Charsets.UTF_8)
            val priority = M4Protocol.Priority.values().firstOrNull { it.wire == d.readUnsignedByte() } ?: error("unknown frame priority")
            val expiresAt = d.readLong()
            val routingTag = ByteArray(32).also(d::readFully)
            val totalBytes = d.readInt()
            require(totalBytes in 1..M4Protocol.MAX_REASSEMBLY_BYTES)
            val length = d.readInt()
            require(length >= 0 && length <= M4Protocol.MAX_FRAME_BYTES)
            require(length <= d.available() - 4) { "declared payload exceeds frame" }
            val payload = ByteArray(length).also(d::readFully)
            val expectedCrc = d.readInt()
            require(d.available() == 0)
            val crc = CRC32().apply { update(payload) }.value.toInt()
            require(crc == expectedCrc) { "frame CRC mismatch" }
            return M4Frame(type, id, destination, priority, expiresAt, routingTag, totalBytes, hop, copies, index, count, payload)
        }
    }
}
