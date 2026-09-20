package dev.syntax6.vani.m0probe.m4

import org.junit.Assert.assertArrayEquals
import org.junit.Test

class M4CryptoTest {
    private val key = TestKey.fromHex("00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff")

    @Test fun aesGcmRoundTrip() {
        val protector = AesGcmBundleProtector(key)
        val plain = "private semantic payload".toByteArray()
        val protected = protector.protect("message-1", plain)
        assertArrayEquals(plain, protector.unprotect("message-1", protected))
    }

    @Test(expected = Exception::class)
    fun tamperingFailsAuthentication() {
        val protector = AesGcmBundleProtector(key)
        val protected = protector.protect("message-1", "private".toByteArray()).copyOf()
        protected[protected.lastIndex] = (protected[protected.lastIndex].toInt() xor 1).toByte()
        protector.unprotect("message-1", protected)
    }

    @Test(expected = Exception::class)
    fun wrongMessageIdFailsAeadAad() {
        val protector = AesGcmBundleProtector(key)
        val protected = protector.protect("message-1", "private".toByteArray())
        protector.unprotect("message-2", protected)
    }
}
