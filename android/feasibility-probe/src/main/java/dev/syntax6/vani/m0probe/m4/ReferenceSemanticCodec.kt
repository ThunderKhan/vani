package dev.syntax6.vani.m0probe.m4

import dev.syntax6.vani.m0probe.m1.CriticalField
import dev.syntax6.vani.m0probe.m1.CriticalFieldType
import dev.syntax6.vani.m0probe.m1.SafetyAction
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

/**
 * Independent reference codec used only to cross-check canonical byte stability.
 * It is intentionally implemented separately from CanonicalBinarySemanticCodec.
 */
object ReferenceSemanticCodec : SemanticCodec {
    private val magic = byteArrayOf(86, 65, 78, 52)

    override fun encode(bundle: M4Protocol.SemanticBundle): ByteArray {
        val b = ByteBuffer.allocate(M4Protocol.MAX_BUNDLE_BYTES).order(ByteOrder.BIG_ENDIAN)
        b.put(magic).put(M4Protocol.MAJOR_VERSION.toByte()).put(M4Protocol.MINOR_VERSION.toByte())
        b.put(bundle.priority.wire.toByte()).put(bundle.messageType.wire.toByte())
        put8(b, bundle.messageId); put8(b, bundle.conversationId); put8(b, bundle.sourceId)
        put8(b, bundle.destinationId); put8(b, bundle.languageTag)
        b.putLong(bundle.createdAtEpochMillis).putLong(bundle.expiresAfterMillis)
        b.put(bundle.hopLimit.toByte()).put(bundle.copyBudget.toByte())
        b.put(bundle.ackPolicy.wire.toByte()).put(bundle.safetyAction.ordinal.toByte())
        put16(b, bundle.transcript)
        b.put(bundle.criticalFields.size.toByte())
        bundle.criticalFields.forEach {
            b.put(it.type.ordinal.toByte()).putShort(it.start.toShort()).putShort(it.end.toShort())
            b.put((it.confidence.coerceIn(0.0, 1.0) * 100.0).toInt().toByte())
            put16(b, it.value)
        }
        b.put(bundle.extensions.size.toByte())
        bundle.extensions.sortedBy { it.id }.forEach {
            b.putShort(it.id.toShort()).put(if (it.critical) 1 else 0)
            b.putShort(it.payload.size.toShort()).put(it.payload)
        }
        require(b.position() <= M4Protocol.MAX_BUNDLE_BYTES)
        return b.array().copyOf(b.position())
    }

    override fun decode(bytes: ByteArray): M4Protocol.SemanticBundle {
        require(bytes.size in 1..M4Protocol.MAX_BUNDLE_BYTES)
        val b = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
        val m = ByteArray(4).also(b::get)
        require(m.contentEquals(magic))
        val major = b.get().toInt() and 0xff
        val minor = b.get().toInt() and 0xff
        require(major == M4Protocol.MAJOR_VERSION && minor <= M4Protocol.MINOR_VERSION)
        val priority = M4Protocol.Priority.values().firstOrNull { it.wire == b.get().toInt() and 0xff } ?: error("priority")
        val type = M4Protocol.MessageType.values().firstOrNull { it.wire == b.get().toInt() and 0xff } ?: error("type")
        val id = get8(b); val conversation = get8(b); val source = get8(b); val destination = get8(b); val language = get8(b)
        val created = b.long; val expiry = b.long
        val hop = b.get().toInt() and 0xff; val copies = b.get().toInt() and 0xff
        val ack = M4Protocol.AckPolicy.values().firstOrNull { it.wire == b.get().toInt() and 0xff } ?: error("ack")
        val safety = SafetyAction.values().getOrNull(b.get().toInt() and 0xff) ?: error("safety")
        val transcript = get16(b)
        val count = b.get().toInt() and 0xff
        require(count <= M4Protocol.MAX_CRITICAL_FIELDS)
        val fields = ArrayList<CriticalField>(count)
        repeat(count) {
            val typeId = b.get().toInt() and 0xff
            val start = b.short.toInt() and 0xffff
            val end = b.short.toInt() and 0xffff
            val confidence = (b.get().toInt() and 0xff) / 100.0
            fields += CriticalField(CriticalFieldType.values().getOrNull(typeId) ?: error("critical type"), get16(b), start, end, confidence)
        }
        val extCount = b.get().toInt() and 0xff
        require(extCount <= M4Protocol.MAX_EXTENSIONS)
        val extensions = ArrayList<M4Protocol.Extension>(extCount)
        repeat(extCount) {
            val idExt = b.short.toInt() and 0xffff
            val critical = b.get().toInt() != 0
            val n = b.short.toInt() and 0xffff
            require(n <= b.remaining())
            val payload = ByteArray(n).also(b::get)
            if (critical) error("unknown critical extension: $idExt")
            extensions += M4Protocol.Extension(idExt, false, payload)
        }
        require(!b.hasRemaining())
        return M4Protocol.SemanticBundle(id, conversation, source, destination, language, priority, type, created, expiry, hop, copies, ack, safety, transcript, fields, extensions)
    }

    private fun put8(b: ByteBuffer, value: String) {
        val bytes = value.toByteArray(StandardCharsets.UTF_8); require(bytes.size <= 255)
        b.put(bytes.size.toByte()).put(bytes)
    }
    private fun put16(b: ByteBuffer, value: String) {
        val bytes = value.toByteArray(StandardCharsets.UTF_8); require(bytes.size <= 0xffff)
        b.putShort(bytes.size.toShort()).put(bytes)
    }
    private fun get8(b: ByteBuffer) = getString(b, b.get().toInt() and 0xff)
    private fun get16(b: ByteBuffer) = getString(b, b.short.toInt() and 0xffff)
    private fun getString(b: ByteBuffer, length: Int): String {
        require(length <= b.remaining())
        val bytes = ByteArray(length).also(b::get)
        return StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes)).toString()
    }
}
