package dev.syntax6.vani.m0probe.m4

interface RoutingPolicy {
    fun decide(bundle: M4Protocol.SemanticBundle, context: M4Protocol.RouteContext, alreadySeen: Boolean): M4Protocol.ForwardDecision
}

object ControlledFloodPolicy : RoutingPolicy {
    override fun decide(bundle: M4Protocol.SemanticBundle, context: M4Protocol.RouteContext, alreadySeen: Boolean): M4Protocol.ForwardDecision {
        if (alreadySeen) return M4Protocol.ForwardDecision(false, M4Protocol.DecisionReason.DUPLICATE)
        if (context.nowEpochMillis >= bundle.createdAtEpochMillis + bundle.expiresAfterMillis)
            return M4Protocol.ForwardDecision(false, M4Protocol.DecisionReason.EXPIRED)
        if (bundle.hopLimit <= 0) return M4Protocol.ForwardDecision(false, M4Protocol.DecisionReason.HOP_LIMIT)
        if (bundle.destinationId == context.peerId)
            return M4Protocol.ForwardDecision(true, M4Protocol.DecisionReason.DESTINATION, 1, 0, 0)
        if (context.queueItems >= M4Protocol.MAX_QUEUE_ITEMS || context.queueBytes >= M4Protocol.MAX_QUEUE_BYTES)
            return M4Protocol.ForwardDecision(false, M4Protocol.DecisionReason.QUEUE_FULL)
        return M4Protocol.ForwardDecision(true, M4Protocol.DecisionReason.POLICY, 1, bundle.hopLimit - 1, bundle.copyBudget)
    }
}

object BinarySprayAndWaitPolicy : RoutingPolicy {
    override fun decide(bundle: M4Protocol.SemanticBundle, context: M4Protocol.RouteContext, alreadySeen: Boolean): M4Protocol.ForwardDecision {
        if (alreadySeen) return M4Protocol.ForwardDecision(false, M4Protocol.DecisionReason.DUPLICATE)
        if (context.nowEpochMillis >= bundle.createdAtEpochMillis + bundle.expiresAfterMillis)
            return M4Protocol.ForwardDecision(false, M4Protocol.DecisionReason.EXPIRED)
        if (bundle.hopLimit <= 0) return M4Protocol.ForwardDecision(false, M4Protocol.DecisionReason.HOP_LIMIT)
        if (bundle.destinationId == context.peerId)
            return M4Protocol.ForwardDecision(true, M4Protocol.DecisionReason.DESTINATION, 1, 0, 0)
        if (bundle.copyBudget <= 1)
            return M4Protocol.ForwardDecision(false, M4Protocol.DecisionReason.COPY_BUDGET)
        if (context.queueItems >= M4Protocol.MAX_QUEUE_ITEMS || context.queueBytes >= M4Protocol.MAX_QUEUE_BYTES)
            return M4Protocol.ForwardDecision(false, M4Protocol.DecisionReason.QUEUE_FULL)
        val copies = maxOf(1, bundle.copyBudget / 2)
        val remaining = bundle.copyBudget - copies
        return M4Protocol.ForwardDecision(true, M4Protocol.DecisionReason.POLICY, copies, bundle.hopLimit - 1, remaining)
    }
}

class Syntax6PriorityRoutingPolicy : RoutingPolicy {
    override fun decide(bundle: M4Protocol.SemanticBundle, context: M4Protocol.RouteContext, alreadySeen: Boolean): M4Protocol.ForwardDecision {
        if (alreadySeen) return M4Protocol.ForwardDecision(false, M4Protocol.DecisionReason.DUPLICATE)
        if (context.nowEpochMillis >= bundle.createdAtEpochMillis + bundle.expiresAfterMillis)
            return M4Protocol.ForwardDecision(false, M4Protocol.DecisionReason.EXPIRED)
        if (bundle.hopLimit <= 0) return M4Protocol.ForwardDecision(false, M4Protocol.DecisionReason.HOP_LIMIT)
        if (bundle.destinationId == context.peerId)
            return M4Protocol.ForwardDecision(true, M4Protocol.DecisionReason.DESTINATION, 1, 0, 0)
        if (context.queueItems >= M4Protocol.MAX_QUEUE_ITEMS || context.queueBytes >= M4Protocol.MAX_QUEUE_BYTES)
            return M4Protocol.ForwardDecision(false, M4Protocol.DecisionReason.QUEUE_FULL)
        if (bundle.priority == M4Protocol.Priority.DISTRESS && context.batteryPercent < 10)
            return M4Protocol.ForwardDecision(false, M4Protocol.DecisionReason.PRIORITY_REJECTED)
        val encounterAllows = context.peerEncounterScore >= if (bundle.priority == M4Protocol.Priority.ROUTINE) 0.25 else 0.05
        if (!encounterAllows) return M4Protocol.ForwardDecision(false, M4Protocol.DecisionReason.POLICY)
        val copies = when (bundle.priority) {
            M4Protocol.Priority.DISTRESS -> minOf(4, maxOf(1, bundle.copyBudget / 2))
            M4Protocol.Priority.URGENT -> minOf(2, maxOf(1, bundle.copyBudget / 2))
            M4Protocol.Priority.ROUTINE -> 1
        }
        return M4Protocol.ForwardDecision(true, M4Protocol.DecisionReason.POLICY, copies, bundle.hopLimit - 1, maxOf(0, bundle.copyBudget - copies))
    }
}
