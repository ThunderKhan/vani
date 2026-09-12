package dev.syntax6.vani.m0probe.m2

/**
 * Deterministic evaluation primitives for M2 evidence.
 *
 * WER uses whitespace-delimited tokens. CER uses Unicode code points after
 * language-aware normalization. Neither metric changes the stored transcript.
 */
object M2Evaluation {
    data class AsrScore(
        val language: M2Language,
        val fixtureId: String,
        val reference: String,
        val hypothesis: String,
        val wer: Double,
        val cer: Double,
    )

    data class TtsEvaluation(
        val language: M2Language,
        val fixtureId: String,
        val method: String,
        val intelligible: Boolean,
        val notes: String,
    )

    data class ResourceObservation(
        val language: M2Language,
        val deviceClass: String,
        val component: String,
        val runtimeVersion: String,
        val assetBytes: Long?,
        val peakRamBytes: Long?,
        val elapsedMillis: Long?,
        val realTimeFactor: Double?,
    )

    fun scoreAsr(
        language: M2Language,
        fixtureId: String,
        reference: String,
        hypothesis: String,
    ): AsrScore {
        val normalizedReference = M2Normalizer.scoringText(reference)
        val normalizedHypothesis = M2Normalizer.scoringText(hypothesis)
        val referenceTokens = normalizedReference.split(' ').filter(String::isNotEmpty)
        val hypothesisTokens = normalizedHypothesis.split(' ').filter(String::isNotEmpty)
        val referenceChars = normalizedReference.codePoints().toArray().filterNot(::isWhitespace).toIntArray()
        val hypothesisChars = normalizedHypothesis.codePoints().toArray().filterNot(::isWhitespace).toIntArray()
        return AsrScore(
            language = language,
            fixtureId = fixtureId,
            reference = reference,
            hypothesis = hypothesis,
            wer = errorRate(referenceTokens, hypothesisTokens),
            cer = errorRate(referenceChars, hypothesisChars),
        )
    }

    private fun isWhitespace(codePoint: Int): Boolean = Character.isWhitespace(codePoint)

    private fun <T> errorRate(reference: List<T>, hypothesis: List<T>): Double =
        if (reference.isEmpty()) {
            if (hypothesis.isEmpty()) 0.0 else 1.0
        } else {
            editDistance(reference, hypothesis).toDouble() / reference.size
        }

    private fun errorRate(reference: IntArray, hypothesis: IntArray): Double =
        if (reference.isEmpty()) {
            if (hypothesis.isEmpty()) 0.0 else 1.0
        } else {
            editDistance(reference, hypothesis).toDouble() / reference.size
        }

    private fun <T> editDistance(a: List<T>, b: List<T>): Int {
        if (a.isEmpty()) return b.size
        if (b.isEmpty()) return a.size
        var previous = IntArray(b.size + 1) { it }
        var current = IntArray(b.size + 1)
        for (i in a.indices) {
            current[0] = i + 1
            for (j in b.indices) {
                val substitution = previous[j] + if (a[i] == b[j]) 0 else 1
                current[j + 1] = minOf(
                    current[j] + 1,
                    previous[j + 1] + 1,
                    substitution,
                )
            }
            val swap = previous
            previous = current
            current = swap
        }
        return previous[b.size]
    }

    private fun editDistance(a: IntArray, b: IntArray): Int {
        if (a.isEmpty()) return b.size
        if (b.isEmpty()) return a.size
        var previous = IntArray(b.size + 1) { it }
        var current = IntArray(b.size + 1)
        for (i in a.indices) {
            current[0] = i + 1
            for (j in b.indices) {
                val substitution = previous[j] + if (a[i] == b[j]) 0 else 1
                current[j + 1] = minOf(
                    current[j] + 1,
                    previous[j + 1] + 1,
                    substitution,
                )
            }
            val swap = previous
            previous = current
            current = swap
        }
        return previous[b.size]
    }
}
