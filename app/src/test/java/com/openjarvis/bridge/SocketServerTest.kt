package com.openjarvis.bridge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotNull
import org.junit.Test

/** JVM-only protocol tests; no Android Context or running socket is required. */
class SocketServerTest {

    @Test
    fun parseRequestWithValidJsonReturnsCommandAndRequestId() {
        val result = SocketProtocol.parseRequest("""{"cmd":"open chrome","requestId":"abc123"}""")
        assertNotNull(result)
        assertEquals("abc123", result?.first)
        assertEquals("open chrome", result?.second)
    }

    @Test
    fun parseRequestWithMissingCommandReturnsNull() {
        assertNull(SocketProtocol.parseRequest("""{"requestId":"abc123"}"""))
    }

    @Test
    fun parseRequestWithInvalidJsonReturnsNull() {
        assertNull(SocketProtocol.parseRequest("not json"))
    }

    @Test
    fun parseRequestWithoutRequestIdUsesEmptyString() {
        val result = SocketProtocol.parseRequest("""{"cmd":"status"}""")
        assertNotNull(result)
        assertEquals("", result?.first)
        assertEquals("status", result?.second)
    }

    @Test
    fun escapeJsonEscapesSpecialCharacters() {
        val escaped = SocketProtocol.escapeJson("hello\nworld\"test\tvalue")
        assertTrue(escaped.contains("\\n"))
        assertTrue(escaped.contains("\\\""))
        assertTrue(escaped.contains("\\t"))
    }

    @Test
    fun createErrorResponseIncludesRequestAndMessage() {
        val response = SocketProtocol.createErrorResponse("req123", "test error")
        assertTrue(response.contains("req123"))
        assertTrue(response.contains("error"))
        assertTrue(response.contains("test error"))
    }

    @Test
    fun malformedJsonInputsAreRejected() {
        listOf("", "{", """{"cmd":""", "null", "[][]").forEach { input ->
            assertNull("Should reject: $input", SocketProtocol.parseRequest(input))
        }
    }
}
