package dev.syntax6.vani.m0probe.m4

import dev.syntax6.vani.m0probe.m1.SafetyAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class M4DeliveryEngineTest {
    private class FakeStore : M4Store {
        val out = linkedMapOf<String, StoredBundle>()
        val seen = mutableSetOf<String>()
        val fragments = linkedMapOf<Pair<String, Int>, M4Protocol.Fragment>()
        val inbox = linkedMapOf<String, M4Protocol.DeliveryState>()

        override fun enqueue(bundle: StoredBundle) { out[bundle.messageId] = bundle }
        override fun find(messageId: String) = out[messageId]
        override fun nextEligible(destinationId: String?, now: Long): StoredBundle? =
            out.values.firstOrNull { (destinationId == null || it.destinationId == destinationId) && (it.state == M4Protocol.DeliveryState.QUEUED || it.state == M4Protocol.DeliveryState.RELAYED) && it.expiresAt > now }
        override fun state(messageId: String) = out[messageId]?.state
        override fun updateState(messageId: String, state: M4Protocol.DeliveryState, attempts: Int?) {
            out[messageId]?.let { out[messageId] = it.copy(state = state, attempts = attempts ?: it.attempts) }
        }
        override fun remove(messageId: String) { out.remove(messageId) }
        override fun markSeen(messageId: String, seenAt: Long): Boolean = seen.add(messageId)
        override fun hasSeen(messageId: String) = messageId in seen
        override fun recoverAfterRestart() {}
        override fun purgeExpired(now: Long) = 0
        override fun saveFragment(fragment: M4Protocol.Fragment, receivedAt: Long) { fragments[fragment.messageId to fragment.fragmentIndex] = fragment }
        override fun hasFragment(messageId: String, index: Int) = fragments.containsKey(messageId to index)
        override fun loadFragments(messageId: String) = fragments.values.filter { it.messageId == messageId }.sortedBy { it.fragmentIndex }
        override fun clearFragments(messageId: String) { fragments.keys.removeAll { it.first == messageId } }
        override fun persistInbox(messageId: String, payload: ByteArray, state: M4Protocol.DeliveryState) { inbox[messageId] = state }
        override fun inboxState(messageId: String) = inbox[messageId]
        override fun updateInboxState(messageId: String, state: M4Protocol.DeliveryState) { inbox[messageId] = state }
    }

    private val key = TestKey.fromHex("00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff")
    private val routeKey = HmacKey.fromHex("ffeeddccbbaa99887766554433221100ffeeddccbbaa99887766554433221100")

    private fun engine(node: String, store: FakeStore) = M4DeliveryEngine(
        node, store, CanonicalBinarySemanticCodec, AesGcmBundleProtector(key),
        HmacSha256RoutingAuthenticator(routeKey), ControlledFloodPolicy
    )

    private fun bundle(destination: String = "receiver") = M4Protocol.SemanticBundle(
        "m-engine-1", "incident", "sender", destination, "en-IN",
        M4Protocol.Priority.URGENT, M4Protocol.MessageType.TEXT,
        1_000, 60_000, 6, 8, M4Protocol.AckPolicy.PLAYBACK,
        SafetyAction.CONFIRM, "Do not enter Sector 13."
    )

    @Test fun directDeliveryReassemblesAndDeliversExactlyOnce() {
        val senderStore = FakeStore()
        val receiverStore = FakeStore()
        val sender = engine("sender", senderStore)
        val receiver = engine("receiver", receiverStore)
        sender.enqueue(bundle(), 2_000)
        val frames = sender.framesFor("m-engine-1", 17)
        frames.asReversed().forEach { receiver.receiveFrame(it, "sender", 2_000) }
        assertEquals(M4Protocol.DeliveryState.DELIVERED_DEVICE, receiverStore.inboxState("m-engine-1"))
        assertTrue(receiver.receiveFrame(frames.first(), "sender", 2_000) is M4DeliveryEngine.ReceiveResult.Duplicate)
    }

    @Test fun tamperedFrameIsRejectedBeforeDelivery() {
        val senderStore = FakeStore()
        val receiverStore = FakeStore()
        val sender = engine("sender", senderStore)
        val receiver = engine("receiver", receiverStore)
        sender.enqueue(bundle(), 2_000)
        val frame = sender.framesFor("m-engine-1", 100).single().copyOf()
        frame[frame.lastIndex - 1] = (frame[frame.lastIndex - 1].toInt() xor 1).toByte()
        assertTrue(runCatching { receiver.receiveFrame(frame, "sender", 2_000) }.isFailure)
        assertTrue(receiverStore.inbox.isEmpty())
    }

    @Test fun relayQueuesOpaqueProtectedContentAndNeverClaimsDestinationDelivery() {
        val senderStore = FakeStore()
        val relayStore = FakeStore()
        val sender = engine("sender", senderStore)
        val relay = engine("relay", relayStore)
        sender.enqueue(bundle(), 2_000)
        val result = sender.framesFor("m-engine-1", 100).map { relay.receiveFrame(it, "sender", 2_000) }.last()
        assertTrue(result is M4DeliveryEngine.ReceiveResult.Relayed)
        assertEquals(M4Protocol.DeliveryState.RELAYED, relayStore.state("m-engine-1"))
        assertTrue(relayStore.inbox.isEmpty())
    }
}
