package dev.syntax6.vani.m0probe.m1

import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

object M1Bundle {
    const val VERSION = 1
    const val MAX_BYTES = 64 * 1024

    data class Decoded(val message: M1Message, val sha256: String)

    fun encode(message: M1Message): ByteArray {
        validate(message)
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
        val json = JSONObject(decodeUtf8(bytes))
        require(json.optInt("v", -1) == VERSION) { "unsupported bundle version" }

        val message = M1Message(
            id = json.optString("id", ""),
            languageTag = json.optString("lang", ""),
            text = json.optString("text", ""),
            createdElapsedNanos = json.optLong("created_elapsed_nanos", -1L),
            source = json.optString("source", ""),
            destination = json.optString("destination", ""),
            priority = json.optInt("priority", -1),
            expiresAfterMillis = json.optLong("expires_after_ms", -1L),
            ackPolicy = json.optString("ack_policy", ""),
        )
        validate(message)
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return Decoded(message, digest.toHex())
    }

    private fun validate(message: M1Message) {
        require(message.id.isNotBlank() && message.id.length <= 128) { "invalid message id" }
        require(message.source.length <= 128 && message.destination.length <= 128) { "invalid endpoint" }
        require(message.languageTag.matches(Regex("^[a-z]{2}(-[A-Z]{2})?$"))) { "invalid language tag" }
        require(message.text.isNotBlank()) { "message text is empty" }
        require(message.priority in 0..3) { "invalid priority" }
        require(message.createdElapsedNanos >= 0L) { "invalid creation timestamp" }
        require(message.expiresAfterMillis in 1..86_400_000L) { "invalid expiry" }
        require(message.ackPolicy.length in 1..32) { "invalid ACK policy" }
    }

    private fun decodeUtf8(bytes: ByteArray): String = try {
        val decoder = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        decoder.decode(ByteBuffer.wrap(bytes)).toString()
    } catch (error: CharacterCodingException) {
        throw IllegalArgumentException("bundle is not valid UTF-8", error)
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}
