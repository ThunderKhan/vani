package dev.syntax6.vani.m0probe.m2

import java.text.Normalizer

/**
 * Conservative normalization for transport/evaluation. It intentionally does
 * not transliterate Indic scripts or rewrite words: those operations can change
 * meaning and belong to a language-specific evaluation policy, not transport.
 */
object M2Normalizer {
    data class NormalizedText(
        val original: String,
        val normalized: String,
        val changed: Boolean,
    )

    fun normalize(language: M2Language, text: String): NormalizedText {
        require(text.isNotBlank()) { "text must not be blank" }
        val original = text
        val unicode = Normalizer.normalize(text, Normalizer.Form.NFC)
        val normalized = unicode
            .replace(WHITESPACE, " ")
            .trim()
        require(normalized.isNotEmpty()) { "normalized text is empty" }
        return NormalizedText(original, normalized, original != normalized)
    }

    /**
     * Returns a stable representation for scoring. The language is part of the
     * signature so future language-specific tokenization can be introduced
     * without changing callers.
     */
    fun scoringText(language: M2Language, text: String): String =
        normalize(language, text).normalized

    private val WHITESPACE = Regex("\\s+")
}
