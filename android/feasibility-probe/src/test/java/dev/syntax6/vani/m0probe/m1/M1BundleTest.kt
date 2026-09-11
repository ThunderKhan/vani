package dev.syntax6.vani.m0probe.m1

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class M1BundleTest {
    @Test
    fun roundTripPreservesUnicodeAndMetadata() {
        val original = M1Message(
            id = "m1-test-001",
            languageTag = "hi-IN",
            text = "नमस्ते, दुनिया — यह VĀṆI है।",
            createdElapsedNanos = 1234L,
            source = "sender",
            destination = "receiver",
            priority = 2,
            expiresAfterMillis = 30_000L,
            ackPolicy = "playback",
        )
        val decoded = M1Bundle.decode(M1Bundle.encode(original)).message
        assertEquals(original, decoded)
    }

    @Test
    fun bundleIsBounded() {
        val message = M1Message(languageTag = "en-IN", text = "x".repeat(60_000), createdElapsedNanos = 1L)
        assertTrue(M1Bundle.encode(message).size <= M1Bundle.MAX_BYTES)
    }

    @Test(expected = IllegalArgumentException::class)
    fun oversizedBundleIsRejected() {
        M1Bundle.encode(M1Message(languageTag = "en-IN", text = "x".repeat(70_000), createdElapsedNanos = 1L))
    }
}
