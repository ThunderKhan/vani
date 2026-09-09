package dev.syntax6.vani.m0probe

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

class LinkProtocolTest {
    @Test
    fun dataFrameRoundTripsMultilingualUtf8() {
        val payload = "नमस्ते | નમસ્તે | ನಮಸ್ಕಾರ | வணக்கம் | ନମସ୍କାର | নমস্কার | Hello"
            .toByteArray(Charsets.UTF_8)
        val bytes = ByteArrayOutputStream()

        LinkProtocol.writeDataFrame(
            output = DataOutputStream(bytes),
            senderElapsedNanos = 1234L,
            senderInfo = "test-device",
            payload = payload,
        )

        val decoded = LinkProtocol.readDataFrame(
            DataInputStream(ByteArrayInputStream(bytes.toByteArray()))
        )

        assertEquals(1234L, decoded.senderElapsedNanos)
        assertEquals("test-device", decoded.senderInfo)
        assertArrayEquals(payload, decoded.payload)
        assertEquals(LinkProtocol.sha256Hex(payload), LinkProtocol.bytesToHex(decoded.sha256))
    }

    @Test(expected = ProtocolException::class)
    fun corruptedPayloadIsRejectedBeforeUse() {
        val payload = "critical 18:30".toByteArray(Charsets.UTF_8)
        val bytes = ByteArrayOutputStream()
        LinkProtocol.writeDataFrame(
            output = DataOutputStream(bytes),
            senderElapsedNanos = 1L,
            senderInfo = "sender",
            payload = payload,
        )
        val corrupted = bytes.toByteArray()
        corrupted[corrupted.lastIndex] = (corrupted.last().toInt() xor 0x01).toByte()

        LinkProtocol.readDataFrame(DataInputStream(ByteArrayInputStream(corrupted)))
    }

    @Test
    fun acknowledgementPreservesHashAndReceiverIdentity() {
        val payload = "hello".toByteArray()
        val hash = LinkProtocol.sha256(payload)
        val bytes = ByteArrayOutputStream()

        LinkProtocol.writeAckFrame(
            output = DataOutputStream(bytes),
            accepted = true,
            receiverElapsedNanos = 9988L,
            receiverInfo = "receiver",
            payloadLength = payload.size,
            sha256 = hash,
        )

        val ack = LinkProtocol.readAckFrame(DataInputStream(ByteArrayInputStream(bytes.toByteArray())))
        assertTrue(ack.accepted)
        assertEquals(9988L, ack.receiverElapsedNanos)
        assertEquals("receiver", ack.receiverInfo)
        assertEquals(payload.size, ack.payloadLength)
        assertArrayEquals(hash, ack.sha256)
    }

    @Test(expected = IllegalArgumentException::class)
    fun oversizedPayloadIsRejectedBeforeSerialization() {
        val bytes = ByteArrayOutputStream()
        LinkProtocol.writeDataFrame(
            output = DataOutputStream(bytes),
            senderElapsedNanos = 1L,
            senderInfo = "sender",
            payload = ByteArray(LinkProtocol.MAX_PAYLOAD_BYTES + 1),
        )
    }
}
