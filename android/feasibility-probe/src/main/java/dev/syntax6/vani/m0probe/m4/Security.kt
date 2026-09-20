package dev.syntax6.vani.m4

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

interface MessageProtector {
    fun protect(plaintext: ByteArray, associatedData: ByteArray = ByteArray(0)): ByteArray
    fun unprotect(envelope: ByteArray, associatedData: ByteArray = ByteArray(0)): ByteArray
}

class AesGcmMessageProtector(private val key: SecretKey, private val random: SecureRandom = SecureRandom()) : MessageProtector {
    override fun protect(plaintext: ByteArray, associatedData: ByteArray): ByteArray {
        require(plaintext.size <= M4_MAX_BUNDLE_BYTES)
        val nonce = ByteArray(12).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, nonce))
        cipher.updateAAD(associatedData)
        return nonce + cipher.doFinal(plaintext)
    }

    override fun unprotect(envelope: ByteArray, associatedData: ByteArray): ByteArray {
        require(envelope.size >= 28) { "invalid security envelope" }
        val nonce = envelope.copyOfRange(0, 12); val ciphertext = envelope.copyOfRange(12, envelope.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, nonce))
        cipher.updateAAD(associatedData)
        return cipher.doFinal(ciphertext).also { require(it.size <= M4_MAX_BUNDLE_BYTES) }
    }

    companion object { fun testKey(): SecretKey = SecretKeySpec(ByteArray(32) { (it + 1).toByte() }, "AES") }
}
