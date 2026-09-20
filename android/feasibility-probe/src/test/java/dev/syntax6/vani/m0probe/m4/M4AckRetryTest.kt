package dev.syntax6.vani.m0probe.m4

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class M4AckRetryTest {
    @Test fun acknowledgementCorrelatesExactMessageAndState() {
        val ack = M4Acknowledgement("m-42", M4Protocol.DeliveryState.PLAYBACK_STARTED, "receiver", 1000)
        val decoded = M4AcknowledgementCodec.decode(M4AcknowledgementCodec.encode(ack))
        assertEquals(ack, decoded)
        assertEquals("m-42", decoded.messageId)
    }

    @Test fun retryStopsAtBoundAndExpiry() {
        assertTrue(M4RetryPolicy.decide(0, 100, 10_000).retry)
        assertTrue(M4RetryPolicy.decide(2, 100, 10_000).retry)
        assertFalse(M4RetryPolicy.decide(M4Protocol.MAX_RETRY_COUNT, 100, 10_000).retry)
        assertFalse(M4RetryPolicy.decide(0, 20_000, 10_000).retry)
    }

    @Test fun unknownAckStateCannotDecode() {
        val bytes = M4AcknowledgementCodec.encode(
            M4Acknowledgement("m", M4Protocol.DeliveryState.DELIVERED_DEVICE, "d", 1)
        ).copyOf()
        bytes[bytes.indexOfLast { it == M4Protocol.DeliveryState.DELIVERED_DEVICE.ordinal.toByte() }] = 99
        runCatching { M4AcknowledgementCodec.decode(bytes) }.also { assertTrue(it.isFailure) }
    }
}
