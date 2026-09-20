package dev.syntax6.vani.m0probe.m4

interface MeshTransport {
    val id: String
    val capabilities: TransportCapabilities
    fun addListener(listener: (peerId: String, frame: ByteArray) -> Unit)
    fun removeListener(listener: (String, ByteArray) -> Unit)
    fun send(peerId: String, frame: ByteArray): SendReceipt
}

data class TransportCapabilities(
    val maxFrameBytes: Int,
    val connectionOriented: Boolean,
    val broadcast: Boolean,
    val name: String,
)

data class SendReceipt(val accepted: Boolean, val bytes: Int, val error: String? = null)

class M4DeliveryEngine(
    private val localNodeId: String,
    private val store: M4DeliveryStore,
    private val codec: SemanticCodec,
    private val protector: BundleProtector,
    private val routeAuthenticator: RoutingAuthenticator,
    private val routingPolicy: RoutingPolicy,
) {
    fun enqueue(bundle: M4Protocol.SemanticBundle, now: Long = System.currentTimeMillis()) {
        require(bundle.sourceId == localNodeId || bundle.sourceId.isNotBlank())
        require(now < bundle.createdAtEpochMillis + bundle.expiresAfterMillis) { "bundle already expired" }
        val canonical = codec.encode(bundle)
        val protected = protector.protect(bundle.messageId, canonical)
        store.enqueue(
            StoredBundle(
                messageId = bundle.messageId,
                destinationId = bundle.destinationId,
                priority = bundle.priority,
                createdAt = bundle.createdAtEpochMillis,
                expiresAt = bundle.createdAtEpochMillis + bundle.expiresAfterMillis,
                hopLimit = bundle.hopLimit,
                copyBudget = bundle.copyBudget,
                attempts = 0,
                state = M4Protocol.DeliveryState.QUEUED,
                payload = protected,
            )
        )
    }

    fun framesFor(messageId: String, maxFragmentPayload: Int): List<ByteArray> {
        val stored = store.nextEligible(null)?.takeIf { it.messageId == messageId }
            ?: error("message is not currently eligible")
        val fragments = M4Fragmenter.split(messageId, stored.payload, maxFragmentPayload)
        return fragments.map { fragment ->
            val provisional = M4Frame(
                type = M4Protocol.FrameType.DATA,
                messageId = messageId,
                destinationId = stored.destinationId,
                priority = stored.priority,
                expiresAtEpochMillis = stored.expiresAt,
                routingTag = ByteArray(32),
                hopLimit = stored.hopLimit,
                copyBudget = stored.copyBudget,
                fragmentIndex = fragment.fragmentIndex,
                fragmentCount = fragment.fragmentCount,
                payload = fragment.payload,
            )
            val authenticated = provisional.copy(routingTag = routeAuthenticator.tag(provisional))
            M4FrameCodec.encode(authenticated)
        }
    }

    fun receiveFrame(frameBytes: ByteArray, peerId: String, now: Long = System.currentTimeMillis()): ReceiveResult {
        val frame = M4FrameCodec.decode(frameBytes)
        require(routeAuthenticator.verify(frame)) { "routing authentication failed" }
        if (now >= frame.expiresAtEpochMillis) {
            return ReceiveResult.Rejected(M4Protocol.DecisionReason.EXPIRED)
        }
        require(frame.destinationId.isNotBlank())
        if (store.hasFragment(frame.messageId, frame.fragmentIndex)) {
            return ReceiveResult.Duplicate
        }
        store.saveFragment(
            M4Protocol.Fragment(frame.messageId, frame.fragmentIndex, frame.fragmentCount, inferTotalBytes(frame), frame.payload)
        )
        val fragments = store.loadFragments(frame.messageId)
        val reassembler = FragmentReassembler()
        var result: FragmentReassembler.Result = FragmentReassembler.Result.Incomplete
        fragments.forEach { result = reassembler.accept(it, now) }
        if (result !is FragmentReassembler.Result.Complete) return ReceiveResult.FragmentStored

        val protected = result.bytes
        if (frame.destinationId == localNodeId) {
            val plaintext = try {
                protector.unprotect(frame.messageId, protected)
            } catch (_: Exception) {
                store.clearFragments(frame.messageId)
                return ReceiveResult.Rejected(M4Protocol.DecisionReason.POLICY)
            }
            val bundle = codec.decode(plaintext)
            require(bundle.messageId == frame.messageId)
            require(bundle.destinationId == localNodeId)
            require(bundle.createdAtEpochMillis + bundle.expiresAfterMillis > now)
            if (!store.markSeen(bundle.messageId, now)) {
                store.clearFragments(bundle.messageId)
                return ReceiveResult.Duplicate
            }
            store.persistInbox(bundle.messageId, plaintext, M4Protocol.DeliveryState.DELIVERED_DEVICE)
            store.clearFragments(bundle.messageId)
            return ReceiveResult.Delivered(bundle)
        }

        val decision = routingPolicy.decide(
            M4Protocol.SemanticBundle(
                messageId = frame.messageId,
                conversationId = "",
                sourceId = "opaque",
                destinationId = frame.destinationId,
                languageTag = "en-IN",
                priority = frame.priority,
                messageType = M4Protocol.MessageType.TEXT,
                createdAtEpochMillis = now.coerceAtMost(frame.expiresAtEpochMillis),
                expiresAfterMillis = (frame.expiresAtEpochMillis - now).coerceAtLeast(1),
                hopLimit = frame.hopLimit,
                copyBudget = frame.copyBudget,
                ackPolicy = M4Protocol.AckPolicy.NONE,
                safetyAction = dev.syntax6.vani.m0probe.m1.SafetyAction.SEND,
                transcript = "opaque",
            ),
            M4Protocol.RouteContext(now, 0, 0, peerId),
            store.hasSeen(frame.messageId)
        )
        if (!decision.forward) return ReceiveResult.Rejected(decision.reason)
        if (!store.markSeen(frame.messageId, now)) return ReceiveResult.Duplicate
        store.enqueue(
            StoredBundle(
                messageId = frame.messageId,
                destinationId = frame.destinationId,
                priority = frame.priority,
                createdAt = now,
                expiresAt = frame.expiresAtEpochMillis,
                hopLimit = decision.nextHopLimit,
                copyBudget = decision.remainingCopyBudget,
                attempts = 0,
                state = M4Protocol.DeliveryState.RELAYED,
                payload = protected,
            )
        )
        store.clearFragments(frame.messageId)
        return ReceiveResult.Relayed(decision)
    }

    private fun inferTotalBytes(frame: M4Frame): Int {
        // Total protected size is recovered from all fragments. The fragment table retains each payload.
        // The bound is enforced by FragmentReassembler; this value is the per-message declared bound used by the durable table.
        return minOf(M4Protocol.MAX_REASSEMBLY_BYTES, frame.fragmentCount * frame.payload.size)
    }

    sealed interface ReceiveResult {
        data object FragmentStored : ReceiveResult
        data object Duplicate : ReceiveResult
        data class Delivered(val bundle: M4Protocol.SemanticBundle) : ReceiveResult
        data class Relayed(val decision: M4Protocol.ForwardDecision) : ReceiveResult
        data class Rejected(val reason: M4Protocol.DecisionReason) : ReceiveResult
    }
}
