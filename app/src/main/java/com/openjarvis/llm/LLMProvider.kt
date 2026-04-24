package com.openjarvis.llm

interface LLMProvider {
    val name: String
    val baseUrl: String
    
    suspend fun complete(
        systemPrompt: String,
        userMessage: String
    ): Result<String>
    
    suspend fun testConnection(): Result<Long>
}

sealed class ConnectionResult {
    data class Success(val latencyMs: Long) : ConnectionResult()
    data class Error(val message: String) : ConnectionResult()
}