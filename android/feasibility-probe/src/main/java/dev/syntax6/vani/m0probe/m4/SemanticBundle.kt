package dev.syntax6.vani.m4

import java.nio.charset.StandardCharsets
import java.util.UUID

const val M4_MAX_BUNDLE_BYTES = 8 * 1024
const val M4_MAX_TRANSCRIPT_BYTES = 4 * 1024
const val M4_MAX_CRITICAL_FIELDS = 32
const val M4_MAX_HOPS = 16
const val M4_MAX_FRAGMENTS = 128
const val M4_MAX_RETRIES = 3
const val M4_MAX_COPY_BUDGET = 16

enum class M4Priority(val wire: Int) { ROUTINE(0), URGENT(1), DISTRESS(2) }
enum class M4AckPolicy(val wire: Int) { NONE(0), DEVICE_RECEIVED(1), PLAYBACK_STARTED(2), PERSON_ACKNOWLEDGED(3) }
enum class M4SafetyAction(val wire: Int) { SEND(0), CONFIRM(1), REPEAT(2), WARN(3) }
enum class M4CriticalType(val wire: Int) { NEGATION(0), NUMBER(1), COORDINATE(2), TIME(3), QUANTITY(4), LOCATION(5), PERSON(6), EMERGENCY_TERM(7) }

data class M4CriticalField(val type: M4CriticalType, val value: String, val start: Int, val end: Int, val confidence: Double)

data class SemanticBundle(
    val messageId: UUID,
    val conversationId: String? = null,
    val sourceId: String,
    val destination: String,
    val createdAtEpochMillis: Long,
    val expiresAtEpochMillis: Long,
    val hopLimit: Int,
    val languageTag: String,
    val priority: M4Priority,
    val ackPolicy: M4AckPolicy,
    val safetyAction: M4SafetyAction,
    val transcript: String,
    val criticalFields: List<M4CriticalField> = emptyList(),
    val copyBudget: Int = 1,
) {
    fun validate(nowEpochMillis: Long? = null) {
        require(sourceId.isNotBlank() && sourceId.length <= 128) { "invalid source" }
        require(destination.isNotBlank() && destination.length <= 128) { "invalid destination" }
        require(languageTag.matches(Regex("^[a-z]{2,3}(-[A-Z]{2})?$"))) { "invalid language tag" }
        val bytes = transcript.toByteArray(StandardCharsets.UTF_8)
        require(bytes.isNotEmpty() && bytes.size <= M4_MAX_TRANSCRIPT_BYTES) { "invalid transcript size" }
        require(expiresAtEpochMillis > createdAtEpochMillis) { "expiry must be after creation" }
        require(expiresAtEpochMillis - createdAtEpochMillis <= 86_400_000L) { "lifetime exceeds 24h" }
        require(hopLimit in 0..M4_MAX_HOPS) { "invalid hop limit" }
        require(copyBudget in 0..M4_MAX_COPY_BUDGET) { "invalid copy budget" }
        require(criticalFields.size <= M4_MAX_CRITICAL_FIELDS) { "too many critical fields" }
        criticalFields.forEach {
            require(it.start >= 0 && it.end > it.start && it.end <= transcript.length) { "invalid critical span" }
            require(it.confidence in 0.0..1.0) { "invalid critical confidence" }
            require(it.value.toByteArray(StandardCharsets.UTF_8).size <= 1024) { "critical value too large" }
        }
        if (nowEpochMillis != null) require(nowEpochMillis <= expiresAtEpochMillis) { "bundle expired" }
    }
}
