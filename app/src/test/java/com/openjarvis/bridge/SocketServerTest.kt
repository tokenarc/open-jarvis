package com.openjarvis.bridge

import org.junit.Test
import org.junit.Assert.*
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.Dispatchers

class SocketServerTest {

    @Test
    fun `parseRequest with valid JSON returns command and requestId`() {
        val server = createTestServer()
        val result = server.parseRequest("""{"cmd":"open chrome","requestId":"abc123"}""")
        
        assertNotNull(result)
        assertEquals("abc123", result?.first)
        assertEquals("open chrome", result?.second)
    }

    @Test
    fun `parseRequest with missing cmd returns null`() {
        val server = createTestServer()
        val result = server.parseRequest("""{"requestId":"abc123"}""")
        
        assertNull(result)
    }

    @Test
    fun `parseRequest with invalid JSON returns null`() {
        val server = createTestServer()
        val result = server.parseRequest("not json")
        
        assertNull(result)
    }

    @Test
    fun `parseRequest with empty request uses empty string`() {
        val server = createTestServer()
        val result = server.parseRequest("""{"cmd":"status"}""")
        
        assertNotNull(result)
        assertEquals("", result?.first)
        assertEquals("status", result?.second)
    }

    @Test
    fun `escapeJson escapes special characters`() {
        val server = createTestServer()
        
        val input = "hello\nworld\"test\tvalue"
        val escaped = server.escapeJson(input)
        
        assertTrue(escaped.contains("\\n"))
        assertTrue(escaped.contains("\\\""))
        assertTrue(escaped.contains("\\t"))
    }

    @Test
    fun `createErrorResponse formats correctly`() {
        val server = createTestServer()
        val response = server.createErrorResponse("req123", "test error")
        
        assertTrue(response.contains("req123"))
        assertTrue(response.contains("error"))
        assertTrue(response.contains("test error"))
    }

    @Test
    fun `special commands handled correctly`() = runTest(Dispatchers.IO) {
        val server = createTestServer()
        val context = testContext()
        
        // Verify status command structure
        val statusRequest = server.parseRequest("""{"cmd":"status","requestId":"s1"}""")
        assertNotNull(statusRequest)
        assertEquals("status", statusRequest?.second)
        
        // Verify history command
        val historyRequest = server.parseRequest("""{"cmd":"history","requestId":"h1"}""")
        assertNotNull(historyRequest)
        assertEquals("history", historyRequest?.second)
        
        // Verify memory command
        val memoryRequest = server.parseRequest("""{"cmd":"memory","requestId":"m1"}""")
        assertNotNull(memoryRequest)
        assertEquals("memory", memoryRequest?.second)
        
        // Verify providers command
        val providersRequest = server.parseRequest("""{"cmd":"providers","requestId":"p1"}""")
        assertNotNull(providersRequest)
        assertEquals("providers", providersRequest?.second)
    }

    @Test
    fun `requestId roundtrip preserved`() {
        val server = createTestServer()
        
        val testIds = listOf("abc123", "xyz789", "test-id-001", "")
        
        for (id in testIds) {
            val json = if (id.isEmpty()) {
                """{"cmd":"test"}"""
            } else {
                """{"cmd":"test","requestId":"$id"}"""
            }
            
            val result = server.parseRequest(json)
            assertNotNull("Failed for id: $id", result)
            assertEquals(id, result?.first)
        }
    }

    @Test
    fun `malformed JSON handled gracefully`() {
        val server = createTestServer()
        
        val malformedInputs = listOf(
            "",
            "{",
            "{\"cmd\":",
            "null",
            "[][]"
        )
        
        for (input in malformedInputs) {
            val result = server.parseRequest(input)
            assertNull("Should return null for: $input", result)
        }
    }

    private fun createTestServer(): SocketServer {
        return SocketServer(
            testContext(),
            TestAgentCore(),
            TestGraphifyRepo()
        )
    }

    private fun testContext(): android.content.Context {
        return androidx.compose.ui.platform.LocalContext.current.applicationContext
    }
}

class TestAgentCore : com.openjarvis.agent.AgentCore(android.content.ContextWrapper(android.content.ContextWrapper::class.java.class.asContextStub())) {
    override fun getCurrentProviderName(): String = "test-provider"
}

class TestGraphifyRepo : com.openjarvis.graphify.GraphifyRepository(android.content.ContextWrapper(android.content.ContextWrapper::class.java.asContextStub())) {
    override suspend fun getRecentTasks(limit: Int): List<com.openjarvis.graphify.nodes.TaskNode> = emptyList()
    override suspend fun getProviderStats(): List<ProviderStats> = emptyList()
    override fun buildMemoryContext(command: String): String = "test context"
}