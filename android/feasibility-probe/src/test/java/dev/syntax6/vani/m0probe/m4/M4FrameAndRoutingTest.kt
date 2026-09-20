package dev.syntax6.vani.m0probe.m4

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class M4FrameAndRoutingTest {
    private val key = HmacKey.fromHex("00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff")
    private val auth = HmacSha256RoutingAuthenticator(key)

    private fun frame(tag: ByteArray = ByteArray(32)) = M4Frame(
        type = M4Protocol.FrameType.DATA,
        messageId = "m4-test-1",
        destinationId = "receiver",
        priority = M4Protocol.Priority.DISTRESS,
        expiresAtEpochMillis = 2_000_000L,
        routingTag = tag,
        totalBytes = 100,
        hopLimit = 4,
        copyBudget = 8,
        fragmentIndex = 0,
        fragmentCount = 2,
        payload = ByteArray(50) { it.toByte() },
    )

    @Test fun frameRoundTripPreservesBoundsAndPayload() {
        val signed = frame().copy(routingTag = auth.tag(frame()))
        val decoded = M4FrameCodec.decode(M4FrameCodec.encode(signed))
        assertEquals(signed.messageId, decoded.messageId)
        assertEquals(signed.destinationId, decoded.destinationId)
        assertEquals(signed.totalBytes, decoded.totalBytes)
        assertTrue(auth.verify(decoded))
    }

    @Test fun tamperedPayloadFailsFrameCrc() {
        val signed = frame().copy(routingTag = auth.tag(frame()))
        val bytes = M4FrameCodec.encode(signed).copyOf()
        bytes[bytes.lastIndex - 5] = (bytes[bytes.lastIndex - 5].toInt() xor 1).toByte()
        runCatching { M4FrameCodec.decode(bytes) }.also { assertTrue(it.isFailure) }
    }

    @Test fun tamperedRoutingMetadataFailsHmac() {
        val signed = frame().copy(routingTag = auth.tag(frame()))
        val tampered = signed.copy(copyBudget = 9)
        assertFalse(auth.verify(tampered))
    }

    @Test(expected = IllegalArgumentException::class)
    fun oversizedFrameIsRejected() {
        val signed = frame(ByteArray(32)).copy(payload = ByteArray(M4Protocol.MAX_FRAME_BYTES))
        M4FrameCodec.encode(signed)
    }

    @Test fun controlledFloodHonorsHopAndDuplicateBounds() {
        val bundle = M4Protocol.SemanticBundle(
            "m", "", "a", "c", "en-IN", M4Protocol.Priority.ROUTINE,
            M4Protocol.MessageType.TEXT, 1000, 10000, 2, 4,
            M4Protocol.AckPolicy.NONE, dev.syntax6.vani.m0probe.m1.SafetyAction.SEND, "hello"
        )
        val ctx = M4Protocol.RouteContext(2000, 0, 0, "b")
        assertTrue(ControlledFloodPolicy.decide(bundle, ctx, false).forward)
        assertFalse(ControlledFloodPolicy.decide(bundle, ctx, true).forward)
        assertFalse(ControlledFloodPolicy.decide(bundle.copy(hopLimit = 0), ctx, false).forward)
    }

    @Test fun sprayAndWaitSplitsCopyBudget() {
        val bundle = M4Protocol.SemanticBundle(
            "m", "", "a", "c", "en-IN", M4Protocol.Priority.ROUTINE,
            M4Protocol.MessageType.TEXT, 1000, 10000, 4, 8,
            M4Protocol.AckPolicy.NONE, dev.syntax6.vani.m0probe.m1.SafetyAction.SEND, "hello"
        )
        val decision = BinarySprayAndWaitPolicy.decide(bundle, M4Protocol.RouteContext(2000, 0, 0, "b"), false)
        assertTrue(decision.forward)
        assertEquals(4, decision.copiesToSend)
        assertEquals(4, decision.remainingCopyBudget)
    }
}
