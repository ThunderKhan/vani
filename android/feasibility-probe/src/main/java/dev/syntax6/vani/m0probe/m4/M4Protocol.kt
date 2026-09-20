package dev.syntax6.vani.m0probe.m4

import dev.syntax6.vani.m0probe.m1.CriticalField
import dev.syntax6.vani.m0probe.m1.SafetyAction

object M4Protocol {
    const val MAJOR_VERSION = 1
    const val MINOR_VERSION = 0
    const val MAX_BUNDLE_BYTES = 8 * 1024
    const val MAX_FRAME_BYTES = 4 * 1024
    const val MAX_FRAGMENT_COUNT = 128
    const val MAX_CRITICAL_FIELDS = 32
    const val MAX_EXTENSIONS = 32
    const val MAX_HOP_LIMIT = 16
    const val MAX_COPY_BUDGET = 32
    const val MAX_RETRY_COUNT = 3
    const val MAX_QUEUE_ITEMS = 256
    const val MAX_QUEUE_BYTES = 512 * 1024
    const val MAX_REASSEMBLY_BYTES = MAX_BUNDLE_BYTES

    enum class Priority(val wire: Int) { ROUTINE(0), URGENT(1), DISTRESS(2) }
    enum class AckPolicy(val wire: Int) { NONE(0), DEVICE(1), PLAYBACK(2), PERSON(3) }
    enum class MessageType(val wire: Int) { TEXT(0), ACK(1), CONTROL(2) }
    enum class FrameType(val wire: Int) { DATA(0), ACK(1) }
    enum class DeliveryState {
        CREATED, VALIDATED, QUEUED, TRANSFERRED, RELAYED, DELIVERED_DEVICE,
        PLAYBACK_STARTED, ACKNOWLEDGED_PERSON, EXPIRED, FAILED
    }

    data class Extension(val id: Int, val critical: Boolean, val payload: ByteArray)

    data class SemanticBundle(
        val messageId: String,
        val conversationId: String,
        val sourceId: String,
        val destinationId: String,
        val languageTag: String,
        val priority: Priority,
        val messageType: MessageType,
        val createdAtEpochMillis: Long,
        val expiresAfterMillis: Long,
        val hopLimit: Int,
        val copyBudget: Int,
        val ackPolicy: AckPolicy,
        val safetyAction: SafetyAction,
        val transcript: String,
        val criticalFields: List<CriticalField> = emptyList(),
        val extensions: List<Extension> = emptyList(),
    ) {
        init {
            require(messageId.isNotBlank() && messageId.length <= 128)
            require(conversationId.length <= 128)
            require(sourceId.isNotBlank() && sourceId.length <= 128)
            require(destinationId.isNotBlank() && destinationId.length <= 128)
            require(languageTag.matches(Regex("^[a-z]{2}(-[A-Z]{2})?$")))
            require(createdAtEpochMillis >= 0)
            require(expiresAfterMillis in 1..86_400_000L)
            require(hopLimit in 0..MAX_HOP_LIMIT)
            require(copyBudget in 0..MAX_COPY_BUDGET)
            require(transcript.isNotBlank())
            require(criticalFields.size <= MAX_CRITICAL_FIELDS)
            require(extensions.size <= MAX_EXTENSIONS)
        }
    }

    data class Fragment(
        val messageId: String,
        val fragmentIndex: Int,
        val fragmentCount: Int,
        val totalBytes: Int,
        val payload: ByteArray,
    )

    data class RouteContext(
        val nowEpochMillis: Long,
        val queueBytes: Int,
        val queueItems: Int,
        val peerId: String,
        val peerEncounterScore: Double = 0.0,
        val batteryPercent: Int = 100,
    )

    enum class DecisionReason {
        DESTINATION, EXPIRED, HOP_LIMIT, COPY_BUDGET, DUPLICATE,
        QUEUE_FULL, PRIORITY_REJECTED, RETRY_EXHAUSTED, POLICY
    }

    data class ForwardDecision(
        val forward: Boolean,
        val reason: DecisionReason,
        val copiesToSend: Int = 0,
        val nextHopLimit: Int = 0,
        val remainingCopyBudget: Int = 0,
    )
}
