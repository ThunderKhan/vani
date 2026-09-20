package dev.syntax6.vani.m0probe.m1

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class M3BundleSafetyTest {
    @Test fun safetyMetadataRoundTrips() {
        val assessment = CriticalInformationGuard.assess("Do not enter Sector 13 before 18:30.", "en-IN")
        val message = M1Message(
            languageTag = "en-IN",
            text = assessment.rawText,
            createdElapsedNanos = 1L,
            safetyAction = assessment.action,
            criticalFields = assessment.fields,
        )
        val decoded = M1Bundle.decode(M1Bundle.encode(message)).message
        assertEquals(message.text, decoded.text)
        assertEquals(message.safetyAction, decoded.safetyAction)
        assertEquals(message.criticalFields.map { it.type }, decoded.criticalFields.map { it.type })
        assertTrue(decoded.criticalFields.isNotEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidCriticalSpanIsRejected() {
        M1Bundle.encode(M1Message(
            languageTag = "en-IN",
            text = "hello",
            createdElapsedNanos = 1L,
            criticalFields = listOf(CriticalField(CriticalFieldType.NUMBER, "999", 0, 99, 0.9)),
        ))
    }
}
