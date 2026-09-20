package dev.syntax6.vani.m4

import java.util.UUID

data class RoutingContext(
    val messageId: UUID, val destinationPeer: String?, val peerId: String, val hopLimit: Int, val copyBudget: Int,
    val priority: M4Priority, val expiresAtEpochMillis: Long, val nowEpochMillis: Long,
    val queueBytes: Long, val queueLimitBytes: Long, val seen: Boolean, val peerIsDestination: Boolean,
)
data class ForwardingDecision(val forward: Boolean, val copies: Int, val remainingCopies: Int, val reason: String)
interface RoutingPolicy { fun decide(context: RoutingContext): ForwardingDecision }

abstract class BoundedRoutingPolicy : RoutingPolicy {
    protected fun reject(c: RoutingContext, reason: String) = ForwardingDecision(false, 0, c.copyBudget, reason)
    protected fun common(c: RoutingContext): ForwardingDecision? = when {
        c.seen -> reject(c, "duplicate-suppressed")
        c.hopLimit <= 0 -> reject(c, "hop-limit-exhausted")
        c.copyBudget <= 0 -> reject(c, "copy-budget-exhausted")
        c.nowEpochMillis >= c.expiresAtEpochMillis -> reject(c, "expired")
        c.queueBytes >= c.queueLimitBytes -> reject(c, "queue-limit")
        else -> null
    }
}
class ControlledFloodPolicy : BoundedRoutingPolicy() {
    override fun decide(context: RoutingContext): ForwardingDecision {
        common(context)?.let { return it }
        return ForwardingDecision(true, 1, context.copyBudget - 1, if (context.peerIsDestination) "destination" else "bounded-flood")
    }
}
class BinarySprayAndWaitPolicy : BoundedRoutingPolicy() {
    override fun decide(context: RoutingContext): ForwardingDecision {
        common(context)?.let { return it }
        if (context.peerIsDestination) return ForwardingDecision(true, 1, 0, "destination")
        if (context.copyBudget <= 1) return reject(context, "waiting-for-destination")
        val transfer = context.copyBudget / 2
        return ForwardingDecision(true, transfer, context.copyBudget - transfer, "binary-spray")
    }
}
