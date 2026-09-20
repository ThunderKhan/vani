package dev.syntax6.vani.m0probe.m4

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

interface BundleProtector {
    fun protect(messageId: String, plaintext: ByteArray): ByteArray
    fun unprotect(messageId: String, protected: ByteArray): ByteArray
}

class AesGcmBundleProtector(private val key: SecretKey) : BundleProtector {
    override fun protect(messageId: String, plaintext: ByteArray): ByteArray {
        require(plaintext.size <= M4Protocol.MAX_BUNDLE_BYTES)
        val nonce = ByteArray(12).also(SecureRandom()::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, nonce))
        cipher.updateAAD(messageId.toByteArray(Charsets.UTF_8))
        val ciphertext = cipher.doFinal(plaintext)
        return nonce + ciphertext
    }

    override fun unprotect(messageId: String, protected: ByteArray): ByteArray {
        require(protected.size >= 12 + 16)
        val nonce = protected.copyOfRange(0, 12)
        val ciphertext = protected.copyOfRange(12, protected.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, nonce))
        cipher.updateAAD(messageId.toByteArray(Charsets.UTF_8))
        return cipher.doFinal(ciphertext).also {
            require(it.size <= M4Protocol.MAX_BUNDLE_BYTES)
        }
    }
}

object AndroidKeystoreKeyProvider {
    private const val STORE = "AndroidKeyStore"
    private const val ALIAS = "vani.m4.bundle"

    fun getOrCreate(): SecretKey {
        val store = KeyStore.getInstance(STORE).apply { load(null) }
        if (store.containsAlias(ALIAS)) {
            return (store.getEntry(ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
        }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, STORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setKeySize(256)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return generator.generateKey()
    }
}

object TestKey {
    fun fromHex(hex: String): SecretKey {
        require(hex.length == 64)
        val bytes = hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        return SecretKeySpec(bytes, "AES")
    }
}
