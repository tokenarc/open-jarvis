package com.openjarvis.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandDictionaryTest {
    @Test fun englishBackAliasesNormalize() {
        assertEquals("press back", CommandDictionary.normalize("Go back"))
    }

    @Test fun hindiAndMarathiBackAliasesNormalize() {
        assertEquals("press back", CommandDictionary.normalize("वापस जाओ"))
        assertEquals("press back", CommandDictionary.normalize("मागे जा"))
    }

    @Test fun multilingualAppOpenAliasesNormalize() {
        assertEquals("open whatsapp", CommandDictionary.normalize("व्हाट्सएप खोलो"))
        assertEquals("open youtube", CommandDictionary.normalize("यूट्यूब उघडा"))
    }

    @Test fun unknownCommandIsPreserved() {
        val command = "Please fill the Euronics service report"
        assertEquals(command, CommandDictionary.normalize(command))
    }
}

class ErrorDiagnosisTest {
    @Test fun timeoutHasActionableFixAndRetryFlag() {
        val result = ErrorDiagnosis.fromThrowable(java.net.SocketTimeoutException("timeout"))
        assertEquals("JARVIS-NET-001", result.code)
        assertTrue(result.retryable)
        assertTrue(result.suggestedFix.contains("Ollama"))
    }

    @Test fun invalidPlanHasRepairGuidance() {
        val result = ErrorDiagnosis.invalidPlan("item 0 is not an object")
        assertEquals("JARVIS-JSON-001", result.code)
        assertTrue(result.suggestedFix.contains("repair"))
    }
}
