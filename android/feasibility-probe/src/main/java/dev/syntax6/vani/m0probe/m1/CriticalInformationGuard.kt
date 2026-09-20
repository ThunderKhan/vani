package dev.syntax6.vani.m0probe.m1

import java.util.Locale

enum class CriticalFieldType { NEGATION, NUMBER, COORDINATE, TIME, QUANTITY, LOCATION, PERSON, EMERGENCY_TERM }

enum class SafetyAction { SEND, CONFIRM, REPEAT, WARN }

data class CriticalField(
    val type: CriticalFieldType,
    val value: String,
    val start: Int,
    val end: Int,
    val confidence: Double,
)

data class SafetyAssessment(
    val rawText: String,
    val normalizedText: String,
    val fields: List<CriticalField>,
    val action: SafetyAction,
    val reasons: List<String>,
) {
    val hasCriticalContent: Boolean get() = fields.isNotEmpty()
    val criticalFieldCount: Int get() = fields.size
}

object CriticalInformationGuard {
    private val negation = Regex("""(?i)\b(no|not|don't|dont|do not|never|without|avoid|cannot|can't|cannot|नहीं|मत|न)\b""")
    private val number = Regex("""(?<![\p{L}\d])(?:\d+(?:[.,]\d+)?|[०-९]+(?:[.,][०-९]+)?)(?![\p{L}\d])""")
    private val coordinate = Regex("""(?i)(?<!\w)[+-]?\d+(?:\.\d+)?\s*[,°]\s*[+-]?\d+(?:\.\d+)?(?!\w)""")
    private val time = Regex("""(?i)\b(?:[01]?\d|2[0-3])[:.]\d{2}\s*(?:am|pm)?\b|\b(?:\d{1,2})\s*(?:am|pm)\b""")
    private val emergency = Regex("""(?i)\b(sos|mayday|emergency|urgent|distress|help|evacuate|evacuation|danger|fire|flood|rescue|hospital|ambulance|बचाओ|आपातकाल|खतरा|आग|बाढ़|मदद|निकासी)\b""")
    private val quantity = Regex("""(?i)\b\d+(?:\.\d+)?\s*(?:kg|g|mg|l|ml|km|m|cm|mm|units?|people|persons?|litres?|meters?|metres?)\b""")
    private val location = Regex("""(?i)\b(?:sector|zone|checkpoint|camp|base|village|town|road|street|station|gate|room|block)\s+[\p{L}\d-]+\b""")

    fun assess(text: String, localeTag: String): SafetyAssessment {
        require(text.isNotBlank()) { "text must not be blank" }
        val normalized = normalize(text)
        val fields = buildList {
            addMatches(normalized, negation, CriticalFieldType.NEGATION, 0.92)
            addMatches(normalized, hindiNegation, CriticalFieldType.NEGATION, 0.92)
            addMatches(normalized, coordinate, CriticalFieldType.COORDINATE, 0.96)
            addMatches(normalized, quantity, CriticalFieldType.QUANTITY, 0.95)
            addMatches(normalized, time, CriticalFieldType.TIME, 0.94)
            addMatches(normalized, number, CriticalFieldType.NUMBER, 0.88)
            addMatches(normalized, location, CriticalFieldType.LOCATION, 0.82)
            addMatches(normalized, emergency, CriticalFieldType.EMERGENCY_TERM, 0.90)
            addMatches(normalized, hindiEmergency, CriticalFieldType.EMERGENCY_TERM, 0.90)
        }.distinctBy { Triple(it.type, it.start, it.end) }.sortedBy { it.start }

        val reasons = mutableListOf<String>()
        if (fields.any { it.type == CriticalFieldType.NEGATION }) reasons += "negation detected"
        if (fields.any { it.type == CriticalFieldType.COORDINATE }) reasons += "coordinate detected"
        if (fields.any { it.type == CriticalFieldType.TIME }) reasons += "time detected"
        if (fields.any { it.type == CriticalFieldType.QUANTITY || it.type == CriticalFieldType.NUMBER }) reasons += "numeric content detected"
        if (fields.any { it.type == CriticalFieldType.LOCATION }) reasons += "location-like content detected"
        if (fields.any { it.type == CriticalFieldType.EMERGENCY_TERM }) reasons += "emergency term detected"

        val action = when {
            fields.any { it.type == CriticalFieldType.COORDINATE || it.type == CriticalFieldType.QUANTITY } -> SafetyAction.CONFIRM
            fields.any { it.type == CriticalFieldType.NEGATION || it.type == CriticalFieldType.TIME || it.type == CriticalFieldType.NUMBER } -> SafetyAction.CONFIRM
            fields.any { it.type == CriticalFieldType.EMERGENCY_TERM } -> SafetyAction.CONFIRM
            fields.isNotEmpty() -> SafetyAction.WARN
            else -> SafetyAction.SEND
        }

        return SafetyAssessment(text, normalized, fields, action, reasons)
    }

    private fun normalize(text: String): String =
        text.trim().replace(Regex("\\s+"), " ").normalizeForUnicode()

    private fun String.normalizeForUnicode(): String =
        java.text.Normalizer.normalize(this, java.text.Normalizer.Form.NFC)

    private fun MutableList<CriticalField>.addMatches(
        text: String,
        regex: Regex,
        type: CriticalFieldType,
        confidence: Double,
    ) {
        regex.findAll(text).forEach { match ->
            add(CriticalField(type, match.value, match.range.first, match.range.last + 1, confidence))
        }
    }
}
