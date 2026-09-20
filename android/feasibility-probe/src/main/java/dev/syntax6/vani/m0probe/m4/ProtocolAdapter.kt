package dev.syntax6.vani.m4

import dev.syntax6.vani.m0probe.m1.CriticalField
import dev.syntax6.vani.m0probe.m1.M1Message
import java.util.UUID

object M4ProtocolAdapter {
    fun fromM1(message: M1Message, nowEpochMillis: Long = System.currentTimeMillis()): SemanticBundle {
        val ttl = message.expiresAfterMillis.coerceIn(1L, 86_400_000L)
        return SemanticBundle(
            messageId = UUID.fromString(message.id),
            sourceId = message.source,
            destination = message.destination,
            createdAtEpochMillis = nowEpochMillis,
            expiresAtEpochMillis = nowEpochMillis + ttl,
            hopLimit = M4_MAX_HOPS,
            languageTag = message.languageTag,
            priority = when (message.priority.coerceIn(0, 2)) {
                0 -> M4Priority.ROUTINE
                1 -> M4Priority.URGENT
                else -> M4Priority.DISTRESS
            },
            ackPolicy = M4AckPolicy.PERSON_ACKNOWLEDGED,
            safetyAction = when (message.safetyAction.name) {
                "CONFIRM" -> M4SafetyAction.CONFIRM
                "WARN" -> M4SafetyAction.WARN
                "REPEAT" -> M4SafetyAction.REPEAT
                else -> M4SafetyAction.SEND
            },
            transcript = message.text,
            criticalFields = message.criticalFields.map { it.toM4() },
            copyBudget = 4,
        )
    }

    private fun CriticalField.toM4() =
        M4CriticalField(M4CriticalType.valueOf(type.name), value, start, end, confidence)
}
