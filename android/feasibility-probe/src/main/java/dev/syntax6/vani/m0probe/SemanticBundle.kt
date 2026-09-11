package dev.syntax6.vani.m0probe

import org.json.JSONObject
import java.util.UUID

data class SemanticBundle(
    val protocolVersion: Int = 1,
    val messageId: String = UUID.randomUUID().toString(),
    val sourceId: String,
    val destination: String,
    val languageCode: String,
    val priority: String = "normal",
    val messageType: String = "speech",
    val transcript: String,
    val expiryEpochMs: Long,
    val hopLimit: Int = 0,
    val criticalFields: List<String> = emptyList(),
    val confidenceSummary: JSONObject? = null,
    val ackPolicy: String = "required",
    val copyBudget: Int = 1,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("protocol_version", protocolVersion)
        .put("message_id", messageId)
        .put("source_id", sourceId)
        .put("destination", destination)
        .put("language_code", languageCode)
        .put("priority", priority)
        .put("message_type", messageType)
        .put("transcript", transcript)
        .put("expiry", expiryEpochMs)
        .put("hop_limit", hopLimit)
        .put("critical_fields", criticalFields)
        .put("confidence_summary", confidenceSummary ?: JSONObject.NULL)
        .put("ack_policy", ackPolicy)
        .put("copy_budget", copyBudget)
        .put("security_data", JSONObject().put("integrity", "sha256"))

    fun encodeUtf8(): ByteArray = toJson().toString().toByteArray(Charsets.UTF_8)

    fun validate(maxBytes: Int = LinkProtocol.MAX_PAYLOAD_BYTES) {
        require(protocolVersion == 1)
        require(messageId.isNotBlank() && messageId.length <= 128)
        require(sourceId.isNotBlank() && sourceId.length <= 128)
        require(destination.isNotBlank() && destination.length <= 128)
        require(languageCode.matches(Regex("^[a-z]{2}(-[A-Z]{2})?$")))
        require(priority in setOf("low", "normal", "high", "critical"))
        require(messageType in setOf("speech", "text"))
        require(transcript.isNotBlank())
        require(expiryEpochMs > 0)
        require(hopLimit in 0..16)
        require(ackPolicy in setOf("none", "required"))
        require(copyBudget in 0..16)
        require(encodeUtf8().size <= maxBytes)
    }

    companion object {
        fun decodeUtf8(bytes: ByteArray): SemanticBundle {
            require(bytes.size <= LinkProtocol.MAX_PAYLOAD_BYTES)
            val json = JSONObject(bytes.toString(Charsets.UTF_8))
            val values = json.optJSONArray("critical_fields")
            val bundle = SemanticBundle(
                protocolVersion = json.getInt("protocol_version"),
                messageId = json.getString("message_id"),
                sourceId = json.getString("source_id"),
                destination = json.getString("destination"),
                languageCode = json.getString("language_code"),
                priority = json.getString("priority"),
                messageType = json.getString("message_type"),
                transcript = json.getString("transcript"),
                expiryEpochMs = json.getLong("expiry"),
                hopLimit = json.getInt("hop_limit"),
                criticalFields = if (values == null) emptyList() else (0 until values.length()).map { values.getString(it) },
                confidenceSummary = json.optJSONObject("confidence_summary"),
                ackPolicy = json.getString("ack_policy"),
                copyBudget = json.optInt("copy_budget", 1),
            )
            bundle.validate()
            return bundle
        }
    }
}
