package dev.syntax6.vani.m4

import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class M4PureTest {
    private val id = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
    private val bundle = SemanticBundle(
        id, "incident-01", "sender", "receiver", 1700000000000, 1700000060000, 4, "hi-IN",
        M4Priority.URGENT, M4AckPolicy.PERSON_ACKNOWLEDGED, M4SafetyAction.CONFIRM,
        "Do not enter Sector 13 before 18:30.",
        listOf(
            M4CriticalField(M4CriticalType.NEGATION, "Do not", 0, 7, .92),
            M4CriticalField(M4CriticalType.LOCATION, "Sector 13", 8, 17, .82),
            M4CriticalField(M4CriticalType.TIME, "18:30", 25, 30, .94),
        ),
        4,
    )

    @Test fun canonicalVectorRoundTrip() {
        val bytes = CanonicalBinaryBundleCodec().encode(bundle)
        assertEquals(
            "56414e3401000000123e4567e89b12d3a456426614174000000b696e636964656e742d3031000673656e646572000872656365697665720000018bcfe568000000018bcfe652600401030104000568692d494e0024446f206e6f7420656e74657220536563746f72203133206265666f72652031383a33302e0300000000075c0006446f206e6f740500080011520009536563746f72203133030019001e5e000531383a3330",
            bytes.toHex(),
        )
        assertEquals(bundle, CanonicalBinaryBundleCodec().decode(bytes))
    }

    @Test fun fragmentationHandlesOutOfOrderAndDuplicate() {
        val bytes = CanonicalBinaryBundleCodec().encode(bundle)
        val frames = M4FrameCodec.fragment(id, bytes, 80)
        val reassembler = FragmentReassembler()
        assertNull(reassembler.accept(frames[0]))
        assertNull(reassembler.accept(frames[0]))
        frames.drop(1).reversed().drop(1).forEach { assertNull(reassembler.accept(it)) }
        assertArrayEquals(bytes, reassembler.accept(frames.last()))
    }

    @Test fun missingFragmentDoesNotComplete() {
        val frames = M4FrameCodec.fragment(id, ByteArray(200) { it.toByte() }, 80)
        val reassembler = FragmentReassembler()
        frames.dropLast(1).forEach { reassembler.accept(it) }
        assertEquals(1, reassembler.pendingCount())
    }

    @Test(expected = IllegalArgumentException::class)
    fun oversizedBundleRejected() {
        bundle.copy(transcript = "x".repeat(M4_MAX_TRANSCRIPT_BYTES + 1)).validate()
    }

    @Test(expected = IllegalArgumentException::class)
    fun malformedFrameRejected() {
        M4FrameCodec.decode(byteArrayOf(1, 2, 3))
    }

    @Test(expected = IllegalArgumentException::class)
    fun unknownProtocolVersionRejected() {
        val bytes = CanonicalBinaryBundleCodec().encode(bundle).copyOf()
        bytes[4] = 99
        CanonicalBinaryBundleCodec().decode(bytes)
    }

    @Test fun tamperingFailsAuthentication() {
        val protector = AesGcmMessageProtector(AesGcmMessageProtector.testKey())
        val envelope = protector.protect(CanonicalBinaryBundleCodec().encode(bundle))
        envelope[envelope.lastIndex] = (envelope.last().toInt() xor 1).toByte()
        assertThrows(Exception::class.java) { protector.unprotect(envelope) }
    }

    @Test fun routingIsBounded() {
        val context = RoutingContext(id, "dest", "peer", 3, 8, M4Priority.DISTRESS, 1000, 100, 100, 1000, false, false)
        val flood = ControlledFloodPolicy().decide(context)
        assertTrue(flood.forward); assertEquals(7, flood.remainingCopies)
        val spray = BinarySprayAndWaitPolicy().decide(context)
        assertEquals(4, spray.copies); assertEquals(4, spray.remainingCopies)
        assertFalse(ControlledFloodPolicy().decide(context.copy(hopLimit = 0)).forward)
        assertFalse(ControlledFloodPolicy().decide(context.copy(seen = true)).forward)
        assertFalse(BinarySprayAndWaitPolicy().decide(context.copy(copyBudget = 1)).forward)
    }

    @Test fun ackCorrelationRejectsWrongIdAndUnauthenticatedAck() {
        val good = DeliveryAcknowledgement(id, DeliveryState.PLAYBACK_STARTED, "receiver", true)
        assertTrue(AckCorrelator.accept(id, good))
        assertFalse(AckCorrelator.accept(UUID.randomUUID(), good))
        assertThrows(IllegalArgumentException::class.java) { AckCorrelator.accept(id, good.copy(authenticated = false)) }
    }

    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }
}
