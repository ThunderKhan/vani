package dev.syntax6.vani.m0probe.m1

import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

object M1Bundle {
    const val VERSION = 1
    const val MAX_BYTES = 64 * 1024

    data class Decoded(val message: M1Message, val sha256: String)

    fun encode(message: M1Message): ByteArray {
        require(message.id.length <= 128) { "message id too long" }
        require(message.languageTag.length <= 32) { "language tag too long" }
        require(message.text.isNotBlank()) { "message text is empty" }
        require(message.priority in 0..3) { "invalid priority" }
        require(message.expiresAfterMillis in 1..86_400_000L) { "invalid expiry" }
        val json = JSONObject()
            .put("v", VERSION)
            .put("id", message.id)
            .put("source", message.source)
            .put("destination", message.destination)
            .put("lang", message.languageTag)
            .put("priority", message.priority)
            .put("text", message.text)
            .put("created_elapsed_nanos", message.createdElapsedNanos)
            .put("expires_after_ms", message.expiresAfterMillis)
            .put("ack_policy", message.ackPolicy)
            .toString()
        val bytes = json.toByteArray(StandardCharsets.UTF_8)
        require(bytes.size <= MAX_BYTES) { "semantic bundle exceeds $MAX_BYTES bytes" }
        return bytes
    }

    fun decode(bytes: ByteArray): Decoded {
        require(bytes.size in 1..MAX_BYTES) { "invalid bundle size" }
        val json = JSONObject(String(bytes, StandardCharsets.UTF_8))
        require(json.optInt("v", -1) == VERSION) { "unsupported bundle version" }
        val id = json.optString("id", "")
        val source = json.optString("source", "")
        val destination = json.optString("destination", "")
        val lang = json.optString("lang", "")
        val text = json.optString("text", "")
        val priority = json.optInt("priority", -1)
        val created = json.optLong("created_elapsed_nanos", -1L)
        val expiry = json.optLong("expires_after_ms", -1L)
        val ackPolicy = json.optString("ack_policy", "")
        require(id.isNotBlank() && id.length <= 128) { "invalid message id" }
        require(source.length <= 128 && destination.length <= 128) { "invalid endpoint" }
        require(lang.isNotBlank() && lang.length <= 32) { "invalid language tag" }
        require(text.isNotBlank()) { "invalid message text" }
        require(priority in 0..3) { "invalid priority" }
        require(created >= 0L && expiry in 1..86_400_000L) { "invalid timing metadata" }
        require(ackPolicy.length <= 32) { "invalid ACK policy" }
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return Decoded(M1Message(id, lang, text, created, source, destination, priority, expiry, ackPolicy), digest.toHex())
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}
