package dev.syntax6.vani.m0probe.m4

import dev.syntax6.vani.m0probe.m1.CriticalField
import dev.syntax6.vani.m0probe.m1.CriticalFieldType
import dev.syntax6.vani.m0probe.m1.SafetyAction
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.charset.StandardCharsets

interface SemanticCodec {
    fun encode(bundle: M4Protocol.SemanticBundle): ByteArray
    fun decode(bytes: ByteArray): M4Protocol.SemanticBundle
}

object CanonicalBinarySemanticCodec : SemanticCodec {
    private val magic = byteArrayOf('V'.code.toByte(), 'A'.code.toByte(), 'N'.code.toByte(), '4'.code.toByte())

    override fun encode(bundle: M4Protocol.SemanticBundle): ByteArray {
        validateBundle(bundle)
        val out = ByteArrayOutputStream()
        DataOutputStream(out).use { d ->
            d.write(magic)
            d.writeByte(M4Protocol.MAJOR_VERSION)
            d.writeByte(M4Protocol.MINOR_VERSION)
            d.writeByte(bundle.priority.wire)
            d.writeByte(bundle.messageType.wire)
            writeString8(d, bundle.messageId)
            writeString8(d, bundle.conversationId)
            writeString8(d, bundle.sourceId)
            writeString8(d, bundle.destinationId)
            writeString8(d, bundle.languageTag)
            d.writeLong(bundle.createdAtEpochMillis)
            d.writeLong(bundle.expiresAfterMillis)
            d.writeByte(bundle.hopLimit)
            d.writeByte(bundle.copyBudget)
            d.writeByte(bundle.ackPolicy.wire)
            d.writeByte(bundle.safetyAction.ordinal)
            writeString16(d, bundle.transcript)
            d.writeByte(bundle.criticalFields.size)
            bundle.criticalFields.forEach { field ->
                d.writeByte(field.type.ordinal)
                d.writeShort(field.start)
                d.writeShort(field.end)
                d.writeByte((field.confidence.coerceIn(0.0, 1.0) * 100.0).toInt())
                writeString16(d, field.value)
            }
            d.writeByte(bundle.extensions.size)
            bundle.extensions.sortedBy { it.id }.forEach { extension ->
                d.writeShort(extension.id)
                d.writeByte(if (extension.critical) 1 else 0)
                d.writeShort(extension.payload.size)
                d.write(extension.payload)
            }
        }
        return out.toByteArray().also {
            require(it.size <= M4Protocol.MAX_BUNDLE_BYTES) { "bundle exceeds limit" }
        }
    }

    override fun decode(bytes: ByteArray): M4Protocol.SemanticBundle {
        require(bytes.size in 1..M4Protocol.MAX_BUNDLE_BYTES) { "invalid bundle size" }
        DataInputStream(ByteArrayInputStream(bytes)).use { d ->
            val actualMagic = ByteArray(4).also(d::readFully)
            require(actualMagic.contentEquals(magic)) { "invalid semantic bundle magic" }
            val major = d.readUnsignedByte()
            val minor = d.readUnsignedByte()
            require(major == M4Protocol.MAJOR_VERSION) { "unsupported protocol major version" }
            require(minor <= M4Protocol.MINOR_VERSION) { "unsupported protocol minor version" }
            val priority = M4Protocol.Priority.values().firstOrNull { it.wire == d.readUnsignedByte() } ?: error("unknown priority")
            val messageType = M4Protocol.MessageType.values().firstOrNull { it.wire == d.readUnsignedByte() } ?: error("unknown message type")
            val messageId = readString8(d)
            val conversationId = readString8(d)
            val sourceId = readString8(d)
            val destinationId = readString8(d)
            val language = readString8(d)
            val created = d.readLong()
            val expiry = d.readLong()
            val hop = d.readUnsignedByte()
            val copies = d.readUnsignedByte()
            val ack = M4Protocol.AckPolicy.values().firstOrNull { it.wire == d.readUnsignedByte() } ?: error("unknown ACK policy")
            val safety = SafetyAction.values().getOrNull(d.readUnsignedByte()) ?: error("unknown safety action")
            val transcript = readString16(d)
            val criticalCount = d.readUnsignedByte()
            require(criticalCount <= M4Protocol.MAX_CRITICAL_FIELDS) { "too many critical fields" }
            val fields = ArrayList<CriticalField>(criticalCount)
            repeat(criticalCount) {
                val type = CriticalFieldType.values().getOrNull(d.readUnsignedByte()) ?: error("unknown critical field type")
                val start = d.readUnsignedShort()
                val end = d.readUnsignedShort()
                val confidence = d.readUnsignedByte() / 100.0
                val value = readString16(d)
                fields += CriticalField(type, value, start, end, confidence)
            }
            val extensionCount = d.readUnsignedByte()
            require(extensionCount <= M4Protocol.MAX_EXTENSIONS) { "too many extensions" }
            val extensions = ArrayList<M4Protocol.Extension>(extensionCount)
            repeat(extensionCount) {
                val id = d.readUnsignedShort()
                val critical = d.readUnsignedByte() != 0
                val length = d.readUnsignedShort()
                require(length <= d.available()) { "extension length exceeds remaining bytes" }
                val payload = ByteArray(length).also(d::readFully)
                if (critical) error("unknown critical extension: $id")
                extensions += M4Protocol.Extension(id, false, payload)
            }
            require(d.available() == 0) { "trailing bytes in canonical bundle" }
            return M4Protocol.SemanticBundle(
                messageId, conversationId, sourceId, destinationId, language,
                priority, messageType, created, expiry, hop, copies, ack, safety,
                transcript, fields, extensions
            ).also(::validateBundle)
        }
    }

    private fun validateBundle(bundle: M4Protocol.SemanticBundle) {
        require(bundle.hopLimit <= M4Protocol.MAX_HOP_LIMIT)
        require(bundle.copyBudget <= M4Protocol.MAX_COPY_BUDGET)
        bundle.criticalFields.forEach {
            require(it.start >= 0 && it.end > it.start && it.end <= bundle.transcript.length) { "invalid critical span" }
            require(it.confidence in 0.0..1.0) { "invalid critical confidence" }
        }
    }

    private fun writeString8(d: DataOutputStream, value: String) {
        val bytes = value.toByteArray(StandardCharsets.UTF_8)
        require(bytes.size <= 255) { "field exceeds u8 string limit" }
        d.writeByte(bytes.size)
        d.write(bytes)
    }

    private fun writeString16(d: DataOutputStream, value: String) {
        val bytes = value.toByteArray(StandardCharsets.UTF_8)
        require(bytes.size <= 0xFFFF) { "field exceeds u16 string limit" }
        d.writeShort(bytes.size)
        d.write(bytes)
    }

    private fun readString8(d: DataInputStream): String = readString(d, d.readUnsignedByte())
    private fun readString16(d: DataInputStream): String = readString(d, d.readUnsignedShort())

    private fun readString(d: DataInputStream, length: Int): String {
        require(length <= d.available()) { "string length exceeds remaining bytes" }
        val bytes = ByteArray(length).also(d::readFully)
        return StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
            .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
            .decode(java.nio.ByteBuffer.wrap(bytes)).toString()
    }
}
