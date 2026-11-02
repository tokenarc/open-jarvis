package com.openjarvis.graphify

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** JVM-only regression tests for deterministic graph analysis helpers. */
class AnalysisEngineTest {

    @Test
    fun sequenceHashIsStableAndBounded() {
        val first = AnalysisAlgorithms.hashSequence("open whatsapp → call john")
        val second = AnalysisAlgorithms.hashSequence("open whatsapp → call john")
        assertEquals(first, second)
        assertEquals(16, first.length)
    }

    @Test
    fun differentSequencesProduceDifferentHashes() {
        val first = AnalysisAlgorithms.hashSequence("open whatsapp → call john")
        val second = AnalysisAlgorithms.hashSequence("open chrome → search weather")
        assertNotEquals(first, second)
    }

    @Test
    fun levenshteinDistanceHandlesExactAndFuzzyNames() {
        assertEquals(0, AnalysisAlgorithms.levenshteinDistance("john", "john"))
        assertEquals(2, AnalysisAlgorithms.levenshteinDistance("john", "jhon"))
        assertEquals(1, AnalysisAlgorithms.levenshteinDistance("john", "jon"))
    }

    @Test
    fun levenshteinDistanceHandlesEmptyStrings() {
        assertEquals(4, AnalysisAlgorithms.levenshteinDistance("", "john"))
        assertEquals(0, AnalysisAlgorithms.levenshteinDistance("", ""))
    }
}
