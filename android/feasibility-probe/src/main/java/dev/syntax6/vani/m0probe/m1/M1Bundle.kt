package dev.syntax6.vani.m0probe.m1

import org.json.JSONArray
import org.json.JSONObject
import java.nio.ByteBuffer
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
        val fields = JSONArray()
        message.criticalFields.forEach {
            fields.put(JSONObject()
                .put("type", it.type.name)
                .put("value", it.value)
                .put("start", it.start)
                .put("end", it.end)
                .put("confidence", it.confidence))
        }
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
            .put("safety_action", message.safetyAction.name)
            .put("critical_fields", fields)
            .toString()
        val bytes = json.toByteArray(StandardCharsets.UTF_8)
        require(bytes.size <= MAX_BYTES) { "semantic bundle exceeds $MAX_BYTES bytes" }
        return bytes
    }

    fun decode(bytes: ByteArray): Decoded {
        require(bytes.size in 1..MAX_BYTES) { "invalid bundle size" }
        val json = JSONObject(decodeUtf8(bytes))
        require(json.optInt("v", -1) == VERSION) { "unsupported bundle version" }
        val fields = mutableListOf<CriticalField>()
        val array = json.optJSONArray("critical_fields") ?: JSONArray()
        require(array.length() <= 32) { "too many critical fields" }
        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            val type = runCatching { CriticalFieldType.valueOf(item.getString("type")) }
                .getOrElse { throw IllegalArgumentException("unknown critical field type") }
            fields += CriticalField(
                type = type,
                value = item.getString("value"),
                start = item.getInt("start"),
                end = item.getInt("end"),
                confidence = item.getDouble("confidence"),
            )
        }
        val safetyAction = runCatching { SafetyAction.valueOf(json.optString("safety_action", SafetyAction.SEND.name)) }
            .getOrElse { throw IllegalArgumentException("invalid safety action") }
        val message = M1Message(
            id = json.optString("id", ""), languageTag = json.optString("lang", ""),
            text = json.optString("text", ""), createdElapsedNanos = json.optLong("created_elapsed_nanos", -1L),
            source = json.optString("source", ""), destination = json.optString("destination", ""),
            priority = json.optInt("priority", -1), expiresAfterMillis = json.optLong("expires_after_ms", -1L),
            ackPolicy = json.optString("ack_policy", ""), safetyAction = safetyAction, criticalFields = fields,
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
        require(message.criticalFields.size <= 32) { "too many critical fields" }
        message.criticalFields.forEach { field ->
            require(field.start >= 0 && field.end > field.start && field.end <= message.text.length) { "invalid critical span" }
            require(field.confidence in 0.0..1.0) { "invalid critical confidence" }
        }
    }

    private fun decodeUtf8(bytes: ByteArray): String = try {
        StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes)).toString()
    } catch (error: CharacterCodingException) {
        throw IllegalArgumentException("bundle is not valid UTF-8", error)
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}
