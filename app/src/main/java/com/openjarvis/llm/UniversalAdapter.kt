package com.openjarvis.llm

import com.openjarvis.llm.providers.*

class UniversalAdapter(
    private var currentProvider: LLMProvider = GroqProvider()
) {
    
    fun setProvider(provider: LLMProvider) {
        currentProvider = provider
    }
    
    fun setProviderByName(name: String, baseUrl: String = ""): LLMProvider {
        currentProvider = when (name.lowercase()) {
            "groq" -> GroqProvider()
            "gemini", "google gemini" -> GeminiProvider()
            "openrouter" -> OpenRouterProvider()
            "anthropic", "anthropic claude" -> AnthropicProvider()
            "openai" -> OpenAIProvider()
            "ollama" -> OllamaProvider()
            else -> CustomProvider(baseUrl)
        }
        return currentProvider
    }
    
    suspend fun complete(
        systemPrompt: String,
        userMessage: String,
        context: String
    ): String {
        return currentProvider.complete(systemPrompt, userMessage, context)
    }
    
    fun getCurrentProvider(): LLMProvider = currentProvider
    
    companion object {
        val AVAILABLE_PROVIDERS = listOf(
            "Groq",
            "Google Gemini", 
            "OpenRouter",
            "Anthropic Claude",
            "OpenAI",
            "Ollama (Local)",
            "Custom"
        )
    }
}