package com.openjarvis.llm

interface LLMProvider {
    val name: String
    val baseUrl: String
    
    suspend fun complete(
        systemPrompt: String,
        userMessage: String,
        context: String
    ): String
}