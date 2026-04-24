package com.openjarvis.graphify

import org.junit.Test
import org.junit.Assert.*
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.Dispatchers

class AnalysisEngineTest {

    @Test
    fun `3 identical sequences should create PatternNode with confidence > 0`() = runTest(Dispatchers.IO) {
        val context = testContext()
        val engine = AnalysisEngine(context)
        val repo = GraphifyRepository(context)

        val now = System.currentTimeMillis()
        
        repo.logTask("open whatsapp", "success", "groq", 500)
        repo.logTask("call john", "success", "groq", 500)
        repo.logTask("open whatsapp", "success", "groq", 500)
        repo.logTask("call john", "success", "groq", 500)
        repo.logTask("open whatsapp", "success", "groq", 500)
        repo.logTask("call john", "success", "groq", 500)

        engine.analyzeLastTask()

        val patterns = repo.getActivePatterns(now - 7 * 24 * 60 * 60 * 1000, 0.3f)
        assertTrue("Should have detected at least one pattern", patterns.isNotEmpty())
        
        val totalConfidence = patterns.sumOf { (it.confidence * 100).toInt() }
        assertTrue("Pattern confidence should be > 0", totalConfidence > 0)
    }

    @Test
    fun `confidence decays after 7 days stale`() = runTest(Dispatchers.IO) {
        val context = testContext()
        val engine = AnalysisEngine(context)
        val repo = GraphifyRepository(context)

        val staleTimestamp = System.currentTimeMillis() - 10 * 24 * 60 * 60 * 1000L
        
        repo.insertPattern(
            com.openjarvis.graphify.nodes.PatternNode(
                sequenceHash = "abc123",
                sequenceString = "test seq",
                occurrenceCount = 3,
                lastSeen = staleTimestamp,
                confidence = 0.9f,
                timeOfDayMask = 0,
                dayOfWeekMask = 0
            )
        )

        engine.decayAllPatterns()

        val patterns = repo.getActivePatterns(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000, 0.1f)
        val decayedPattern = patterns.find { it.sequenceHash == "abc123" }
        
        assertNotNull("Pattern should still exist after decay", decayedPattern)
        assertTrue("Confidence should decay after 7 days", decayedPattern!!.confidence < 0.9f)
    }

    @Test
    fun `contact fuzzy match within distance 2`() = runTest(Dispatchers.IO) {
        val context = testContext()
        val repo = GraphifyRepository(context)

        repo.insertContact(
            com.openjarvis.graphify.nodes.ContactNode(
                name = "John Doe",
                phoneNumber = "+1234567890",
                lastContacted = System.currentTimeMillis(),
                contactCount = 5
            )
        )

        val exactMatch = repo.findContactByName("John Doe")
        assertNotNull("Exact name match should work", exactMatch)

        val fuzzyMatch1 = repo.findContactByName("Jhon Doe")
        val fuzzyMatch2 = repo.findContactByName("Jon Doe")
        
        assertTrue("Fuzzy match within distance 2 should find contact", 
            fuzzyMatch1 != null || fuzzyMatch2 != null)
    }

    private fun testContext(): android.content.Context {
        return androidx.compose.ui.platform.LocalContext.current.applicationContext
    }
}