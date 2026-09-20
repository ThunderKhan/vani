package dev.syntax6.vani.m0probe.m1

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CriticalInformationGuardTest {
    @Test fun detectsNegationAndNumber() {
        val result = CriticalInformationGuard.assess("Do not enter Sector 13.", "en-IN")
        assertTrue(result.fields.any { it.type == CriticalFieldType.NEGATION })
        assertTrue(result.fields.any { it.type == CriticalFieldType.NUMBER })
        assertTrue(result.action == SafetyAction.CONFIRM)
    }

    @Test fun detectsCoordinateAndQuantity() {
        val result = CriticalInformationGuard.assess("Move to 26.7606, 83.3732 with 5 kg.", "en-IN")
        assertTrue(result.fields.any { it.type == CriticalFieldType.COORDINATE })
        assertTrue(result.fields.any { it.type == CriticalFieldType.QUANTITY })
    }

    @Test fun preservesTextInsteadOfRepairingIt() {
        val text = "Do not enter Sector 13 before 18:30."
        val result = CriticalInformationGuard.assess(text, "en-IN")
        assertEquals(text, result.rawText)
        assertEquals(text, result.normalizedText)
    }

    @Test fun detectsHindiSafetyTerms() {
        val result = CriticalInformationGuard.assess("खतरा है, यहाँ मत जाओ।", "hi-IN")
        assertTrue(result.fields.any { it.type == CriticalFieldType.EMERGENCY_TERM })
        assertTrue(result.fields.any { it.type == CriticalFieldType.NEGATION })
    }
}
