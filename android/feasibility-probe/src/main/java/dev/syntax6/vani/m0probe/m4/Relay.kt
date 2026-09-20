package dev.syntax6.vani.m4

import java.nio.ByteBuffer
import java.util.UUID

data class RelayEnvelope(
    val messageId: UUID, val expiresAtEpochMillis: Long, val hopLimitRemaining: Int,
    val copyBudgetRemaining: Int, val priority: M4Priority, val protectedBundle: ByteArray,
) {
    fun validate(now: Long) {
        require(protectedBundle.isNotEmpty() && protectedBundle.size <= M4_MAX_BUNDLE_BYTES)
        require(hopLimitRemaining in 0..M4_MAX_HOPS)
        require(copyBudgetRemaining in 0..M4_MAX_COPY_BUDGET)
        require(now < expiresAtEpochMillis) { "expired relay envelope" }
    }
}

object RelayEnvelopeCodec {
    private const val MAGIC = 0x56414E52
    private const val VERSION = 1
    private const val HEADER = 4 + 1 + 16 + 8 + 1 + 1 + 1 + 4

    fun encode(envelope: RelayEnvelope): ByteArray {
        envelope.validate(System.currentTimeMillis())
        val b = ByteBuffer.allocate(HEADER + envelope.protectedBundle.size)
        b.putInt(MAGIC).put(VERSION.toByte())
        b.putLong(envelope.messageId.mostSignificantBits).putLong(envelope.messageId.leastSignificantBits)
        b.putLong(envelope.expiresAtEpochMillis).put(envelope.hopLimitRemaining.toByte()).put(envelope.copyBudgetRemaining.toByte()).put(envelope.priority.wire.toByte()).putInt(envelope.protectedBundle.size).put(envelope.protectedBundle)
        return b.array()
    }

    fun decode(bytes: ByteArray, now: Long = System.currentTimeMillis()): RelayEnvelope {
        require(bytes.size in HEADER + 1..HEADER + M4_MAX_BUNDLE_BYTES) { "invalid relay envelope size" }
        val b = ByteBuffer.wrap(bytes)
        require(b.int == MAGIC && (b.get().toInt() and 0xff) == VERSION) { "unsupported relay envelope" }
        val id = UUID(b.long, b.long); val expires = b.long
        val hops = b.get().toInt() and 0xff; val copies = b.get().toInt() and 0xff
        val priority = M4Priority.entries.firstOrNull { it.wire == (b.get().toInt() and 0xff) } ?: error("invalid relay priority")
        val length = b.int
        require(length in 1..M4_MAX_BUNDLE_BYTES && length == b.remaining()) { "invalid relay payload length" }
        val payload = ByteArray(length); b.get(payload)
        return RelayEnvelope(id, expires, hops, copies, priority, payload).also { it.validate(now) }
    }
}

class RelayEngine(private val policy: RoutingPolicy) {
    fun decide(envelope: RelayEnvelope, context: RoutingContext): ForwardingDecision {
        envelope.validate(context.nowEpochMillis)
        require(context.messageId == envelope.messageId)
        require(context.copyBudget == envelope.copyBudgetRemaining)
        require(context.hopLimit == envelope.hopLimitRemaining)
        return policy.decide(context)
    }

    fun forwardedEnvelope(envelope: RelayEnvelope, decision: ForwardingDecision, now: Long): RelayEnvelope? {
        if (!decision.forward) return null
        require(envelope.hopLimitRemaining > 0)
        return envelope.copy(
            hopLimitRemaining = envelope.hopLimitRemaining - 1,
            copyBudgetRemaining = decision.remainingCopies,
        ).also { it.validate(now) }
    }
}
