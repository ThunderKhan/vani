package dev.syntax6.vani.m0probe.m1

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets

class M1BundleTest {
    private fun message(text: String = "नमस्ते, दुनिया — यह VĀṆI है।") = M1Message(
        id = "m1-test-001", languageTag = "hi-IN", text = text,
        createdElapsedNanos = 1234L, source = "sender", destination = "receiver",
        priority = 2, expiresAfterMillis = 30_000L, ackPolicy = "playback",
    )

    @Test
    fun roundTripPreservesUnicodeAndMetadata() {
        val original = message()
        assertEquals(original, M1Bundle.decode(M1Bundle.encode(original)).message)
    }

    @Test
    fun bundleIsBounded() {
        val encoded = M1Bundle.encode(message("x".repeat(60_000)))
        assertTrue(encoded.size <= M1Bundle.MAX_BYTES)
    }

    @Test
    fun digestChangesWhenSerializedContentChanges() {
        val first = M1Bundle.decode(M1Bundle.encode(message("alpha"))).sha256
        val second = M1Bundle.decode(M1Bundle.encode(message("beta"))).sha256
        assertTrue(first != second)
    }

    @Test(expected = IllegalArgumentException::class)
    fun oversizedBundleIsRejected() {
        M1Bundle.encode(message("x".repeat(70_000)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidPriorityIsRejected() {
        M1Bundle.encode(message().copy(priority = 9))
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidVersionIsRejected() {
        val json = String(M1Bundle.encode(message()), StandardCharsets.UTF_8).replace("\"v\":1", "\"v\":99")
        M1Bundle.decode(json.toByteArray(StandardCharsets.UTF_8))
    }

    @Test(expected = IllegalArgumentException::class)
    fun malformedUtf8IsRejected() {
        val bytes = M1Bundle.encode(message()).copyOf()
        bytes[bytes.lastIndex] = 0xFF.toByte()
        M1Bundle.decode(bytes)
    }
}
