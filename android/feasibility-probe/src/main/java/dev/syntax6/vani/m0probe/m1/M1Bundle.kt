package dev.syntax6.vani.m0probe.m1

import android.util.Base64
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

object M1Bundle {
    const val VERSION = 1
    const val MAX_BYTES = 64 * 1024

    data class Decoded(
        val message: M1Message,
        val sha256: String,
    )

    fun encode(message: M1Message): ByteArray {
        require(message.id.length <= 128) { "message id too long" }
        require(message.languageTag.length <= 32) { "language tag too long" }
        require(message.text.isNotBlank()) { "message text is empty" }
        val json = JSONObject()
            .put("v", VERSION)
            .put("id", message.id)
            .put("lang", message.languageTag)
            .put("text", message.text)
            .put("created_elapsed_nanos", message.createdElapsedNanos)
            .toString()
        val bytes = json.toByteArray(StandardCharsets.UTF_8)
        require(bytes.size <= MAX_BYTES) { "semantic bundle exceeds $MAX_BYTES bytes" }
        return bytes
    }

    fun decode(bytes: ByteArray): Decoded {
        require(bytes.size <= MAX_BYTES) { "bundle exceeds $MAX_BYTES bytes" }
        val json = JSONObject(String(bytes, StandardCharsets.UTF_8))
        require(json.optInt("v", -1) == VERSION) { "unsupported bundle version" }
        val id = json.optString("id", "")
        val lang = json.optString("lang", "")
        val text = json.optString("text", "")
        require(id.isNotBlank() && id.length <= 128) { "invalid message id" }
        require(lang.isNotBlank() && lang.length <= 32) { "invalid language tag" }
        require(text.isNotBlank()) { "invalid message text" }
        val created = json.optLong("created_elapsed_nanos", -1L)
        require(created >= 0L) { "invalid creation timestamp" }
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return Decoded(M1Message(id, lang, text, created), digest.toHex())
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}
