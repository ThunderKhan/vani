package dev.syntax6.vani.m0probe.m2

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class M2EvaluationTest {
    @Test
    fun requiredLanguageSetContainsExactlyTenExplicitPaths() {
        assertEquals(10, M2Language.entries.size)
        assertEquals(
            setOf("hi", "gu", "mr", "kn", "ml", "ta", "te", "or", "bn", "en"),
            M2Language.entries.map { it.languageCode }.toSet(),
        )
    }

    @Test
    fun localeAndLanguageCodeLookupIsStrict() {
        assertEquals(M2Language.HINDI, M2Language.fromLocaleTag("HI-in"))
        assertEquals(M2Language.TAMIL, M2Language.fromLanguageCode("ta"))
        assertEquals(null, M2Language.fromLocaleTag("xx-IN"))
        assertEquals(null, M2Language.fromLanguageCode("xx"))
    }

    @Test
    fun normalizationPreservesIndicCharactersAndCanonicalizesWhitespace() {
        val input = "  सेक्टर\u00A013   में\n18:30 बजे  "
        assertEquals("सेक्टर 13 में 18:30 बजे", M2Normalizer.normalize(input))
    }

    @Test
    fun identicalHindiFixtureScoresZeroError() {
        val fixture = M2Fixtures.forLanguage(M2Language.HINDI)
        val score = M2Evaluation.scoreAsr(
            language = fixture.language,
            fixtureId = fixture.id,
            reference = fixture.text,
            hypothesis = fixture.text,
        )
        assertEquals(0.0, score.wer, 0.0)
        assertEquals(0.0, score.cer, 0.0)
    }

    @Test
    fun substitutionAndInsertionAreCounted() {
        val score = M2Evaluation.scoreAsr(
            language = M2Language.ENGLISH,
            fixtureId = "unit",
            reference = "alpha beta",
            hypothesis = "alpha gamma extra",
        )
        assertTrue(score.wer > 0.0)
        assertTrue(score.cer > 0.0)
    }
}
