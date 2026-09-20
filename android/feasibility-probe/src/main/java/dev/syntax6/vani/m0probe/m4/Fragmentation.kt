package dev.syntax6.vani.m4

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.UUID
import java.util.zip.CRC32

data class M4Frame(val messageId: UUID, val fragmentIndex: Int, val fragmentCount: Int, val totalLength: Int, val payload: ByteArray)

object M4FrameCodec {
    const val MAX_FRAME_BYTES = 1024
    const val HEADER_BYTES = 4 + 1 + 1 + 2 + 16 + 2 + 2 + 4 + 4
    const val CRC_BYTES = 4
    private const val MAGIC = 0x56414E46
    private const val VERSION = 1

    fun encode(frame: M4Frame): ByteArray {
        validate(frame)
        val body = ByteBuffer.allocate(HEADER_BYTES + frame.payload.size + CRC_BYTES)
        body.putInt(MAGIC).put(VERSION.toByte()).put(0).putShort(0)
        body.putLong(frame.messageId.mostSignificantBits).putLong(frame.messageId.leastSignificantBits)
        body.putShort(frame.fragmentIndex.toShort()).putShort(frame.fragmentCount.toShort())
        body.putInt(frame.totalLength).putInt(frame.payload.size).put(frame.payload)
        val crc = CRC32().apply { update(body.array(), 0, body.position()) }.value
        body.putInt(crc.toInt())
        return body.array()
    }

    fun decode(bytes: ByteArray): M4Frame {
        require(bytes.size in HEADER_BYTES + CRC_BYTES..MAX_FRAME_BYTES) { "invalid frame size" }
        val expected = ByteBuffer.wrap(bytes).apply { position(bytes.size - CRC_BYTES) }.int.toLong() and 0xffffffffL
        val crc = CRC32().apply { update(bytes, 0, bytes.size - CRC_BYTES) }.value
        require(expected == crc) { "frame CRC mismatch" }
        val b = ByteBuffer.wrap(bytes)
        require(b.int == MAGIC) { "invalid frame magic" }
        require((b.get().toInt() and 0xff) == VERSION) { "unsupported frame version" }
        b.get(); b.short
        val id = UUID(b.long, b.long)
        val index = b.short.toInt() and 0xffff; val count = b.short.toInt() and 0xffff
        val total = b.int; val length = b.int
        require(length == bytes.size - HEADER_BYTES - CRC_BYTES) { "frame payload length mismatch" }
        require(count in 1..M4_MAX_FRAGMENTS && index in 0 until count) { "invalid fragment bounds" }
        require(total in 1..M4_MAX_BUNDLE_BYTES) { "invalid total length" }
        val payload = ByteArray(length); b.get(payload)
        return M4Frame(id, index, count, total, payload)
    }

    private fun validate(frame: M4Frame) {
        require(frame.fragmentCount in 1..M4_MAX_FRAGMENTS)
        require(frame.fragmentIndex in 0 until frame.fragmentCount)
        require(frame.totalLength in 1..M4_MAX_BUNDLE_BYTES)
        require(frame.payload.isNotEmpty() && frame.payload.size <= MAX_FRAME_BYTES - HEADER_BYTES - CRC_BYTES)
        require(frame.totalLength >= frame.payload.size)
    }

    fun fragment(messageId: UUID, bundle: ByteArray, maxFrameBytes: Int = MAX_FRAME_BYTES): List<ByteArray> {
        require(bundle.size in 1..M4_MAX_BUNDLE_BYTES)
        require(maxFrameBytes in HEADER_BYTES + CRC_BYTES + 1..MAX_FRAME_BYTES)
        val chunk = maxFrameBytes - HEADER_BYTES - CRC_BYTES
        val count = (bundle.size + chunk - 1) / chunk
        require(count in 1..M4_MAX_FRAGMENTS)
        return (0 until count).map { index ->
            val from = index * chunk; val to = minOf(bundle.size, from + chunk)
            encode(M4Frame(messageId, index, count, bundle.size, bundle.copyOfRange(from, to)))
        }
    }
}

class FragmentReassembler(private val now: () -> Long = { System.currentTimeMillis() }) {
    private data class State(val total: Int, val count: Int, val parts: MutableMap<Int, ByteArray>, var lastSeen: Long)
    private val states = linkedMapOf<UUID, State>()

    fun accept(frameBytes: ByteArray): ByteArray? {
        val frame = M4FrameCodec.decode(frameBytes)
        val state = states.getOrPut(frame.messageId) { State(frame.totalLength, frame.fragmentCount, linkedMapOf(), now()) }
        require(state.total == frame.totalLength && state.count == frame.fragmentCount) { "conflicting fragment metadata" }
        state.lastSeen = now()
        if (state.parts.putIfAbsent(frame.fragmentIndex, frame.payload) != null) return null
        if (state.parts.size != state.count) return null
        val out = ByteArrayOutputStream(state.total)
        for (i in 0 until state.count) out.write(state.parts[i] ?: return null)
        val result = out.toByteArray()
        require(result.size == state.total) { "reassembled length mismatch" }
        states.remove(frame.messageId)
        return result
    }

    fun expireOlderThan(ageMillis: Long): Int {
        require(ageMillis >= 0)
        val cutoff = now() - ageMillis
        val before = states.size
        states.entries.removeIf { it.value.lastSeen < cutoff }
        return before - states.size
    }

    fun pendingCount(): Int = states.size
}
