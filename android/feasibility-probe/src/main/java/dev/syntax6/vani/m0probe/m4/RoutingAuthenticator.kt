package dev.syntax6.vani.m0probe.m4

import java.nio.ByteBuffer
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

interface RoutingAuthenticator {
    fun tag(frame: M4Frame): ByteArray
    fun verify(frame: M4Frame): Boolean
}

class HmacSha256RoutingAuthenticator(private val key: SecretKey) : RoutingAuthenticator {
    override fun tag(frame: M4Frame): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(key)
        mac.update(canonicalRoutingBytes(frame))
        return mac.doFinal()
    }

    override fun verify(frame: M4Frame): Boolean =
        MessageDigest.isEqual(tag(frame.copy(routingTag = ByteArray(32))), frame.routingTag)

    private fun canonicalRoutingBytes(frame: M4Frame): ByteArray {
        val destination = frame.destinationId.toByteArray(Charsets.UTF_8)
        val id = frame.messageId.toByteArray(Charsets.UTF_8)
        return ByteBuffer.allocate(1 + 1 + 8 + 1 + id.size + 1 + destination.size + 2 + 2)
            .put(frame.type.wire.toByte())
            .put(frame.priority.wire.toByte())
            .putLong(frame.expiresAtEpochMillis)
            .put(id.size.toByte()).put(id)
            .put(destination.size.toByte()).put(destination)
            .putShort(frame.hopLimit.toShort())
            .putShort(frame.copyBudget.toShort())
            .array()
    }
}

object HmacKey {
    fun fromHex(hex: String): SecretKey {
        require(hex.length == 64)
        return SecretKeySpec(hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray(), "HmacSHA256")
    }
}
