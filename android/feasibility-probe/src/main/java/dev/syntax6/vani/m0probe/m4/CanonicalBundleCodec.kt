package dev.syntax6.vani.m4

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.charset.StandardCharsets
import java.util.UUID

interface SemanticBundleCodec {
    fun encode(bundle: SemanticBundle): ByteArray
    fun decode(bytes: ByteArray): SemanticBundle
}

/** Experimental compact codec. The byte format remains unfrozen until independent codec agreement. */
class CanonicalBinaryBundleCodec : SemanticBundleCodec {
    override fun encode(bundle: SemanticBundle): ByteArray {
        bundle.validate()
        val out = ByteArrayOutputStream()
        DataOutputStream(out).use { d ->
            d.writeInt(MAGIC); d.writeByte(MAJOR); d.writeByte(MINOR); d.writeByte(0); d.writeByte(0)
            d.writeLong(bundle.messageId.mostSignificantBits); d.writeLong(bundle.messageId.leastSignificantBits)
            writeString(d, bundle.conversationId); writeString(d, bundle.sourceId); writeString(d, bundle.destination)
            d.writeLong(bundle.createdAtEpochMillis); d.writeLong(bundle.expiresAtEpochMillis)
            d.writeByte(bundle.hopLimit); d.writeByte(bundle.priority.wire); d.writeByte(bundle.ackPolicy.wire)
            d.writeByte(bundle.safetyAction.wire); d.writeByte(bundle.copyBudget)
            writeString(d, bundle.languageTag); writeString(d, bundle.transcript)
            d.writeByte(bundle.criticalFields.size)
            bundle.criticalFields.forEach {
                d.writeByte(it.type.wire); d.writeShort(it.start); d.writeShort(it.end)
                d.writeByte(kotlin.math.round(it.confidence * 100.0).toInt()); writeString(d, it.value)
            }
        }
        return out.toByteArray().also { require(it.size <= M4_MAX_BUNDLE_BYTES) { "bundle exceeds $M4_MAX_BUNDLE_BYTES bytes" } }
    }

    override fun decode(bytes: ByteArray): SemanticBundle {
        require(bytes.size in 1..M4_MAX_BUNDLE_BYTES) { "invalid bundle size" }
        val input = DataInputStream(ByteArrayInputStream(bytes))
        require(input.readInt() == MAGIC) { "invalid bundle magic" }
        require(input.readUnsignedByte() == MAJOR) { "unsupported bundle major version" }
        require(input.readUnsignedByte() <= MINOR) { "unsupported bundle minor version" }
        input.readUnsignedByte(); input.readUnsignedByte()
        val id = UUID(input.readLong(), input.readLong())
        val conversation = readString(input, true)
        val source = readString(input)!!
        val destination = readString(input)!!
        val created = input.readLong(); val expires = input.readLong()
        val hops = input.readUnsignedByte()
        val priorityWire = input.readUnsignedByte(); val ackWire = input.readUnsignedByte(); val safetyWire = input.readUnsignedByte()
        val priority = M4Priority.entries.firstOrNull { it.wire == priorityWire } ?: error("invalid priority")
        val ack = M4AckPolicy.entries.firstOrNull { it.wire == ackWire } ?: error("invalid ACK policy")
        val safety = M4SafetyAction.entries.firstOrNull { it.wire == safetyWire } ?: error("invalid safety action")
        val copies = input.readUnsignedByte()
        val language = readString(input)!!; val transcript = readString(input)!!
        val count = input.readUnsignedByte()
        require(count <= M4_MAX_CRITICAL_FIELDS) { "too many critical fields" }
        val fields = ArrayList<M4CriticalField>(count)
        repeat(count) {
            val typeWire = input.readUnsignedByte()
            val type = M4CriticalType.entries.firstOrNull { it.wire == typeWire } ?: error("invalid critical field type")
            val start = input.readUnsignedShort(); val end = input.readUnsignedShort()
            val confidence = input.readUnsignedByte() / 100.0
            fields += M4CriticalField(type, readString(input)!!, start, end, confidence)
        }
        require(input.available() == 0) { "trailing bundle bytes" }
        return SemanticBundle(id, conversation, source, destination, created, expires, hops, language, priority, ack, safety, transcript, fields, copies).also { it.validate() }
    }

    private fun writeString(d: DataOutputStream, value: String?) {
        if (value == null) { d.writeShort(0xFFFF); return }
        val bytes = value.toByteArray(StandardCharsets.UTF_8)
        require(bytes.size <= 0xFFFE) { "string too large" }
        d.writeShort(bytes.size); d.write(bytes)
    }

    private fun readString(input: DataInputStream, allowNull: Boolean = false): String? {
        val length = input.readUnsignedShort()
        if (length == 0xFFFF) { require(allowNull) { "unexpected null string" }; return null }
        require(length <= M4_MAX_BUNDLE_BYTES) { "string length exceeds bounds" }
        val bytes = ByteArray(length); input.readFully(bytes)
        return bytes.toString(StandardCharsets.UTF_8)
    }

    companion object { const val MAGIC = 0x56414E34; const val MAJOR = 1; const val MINOR = 0 }
}
