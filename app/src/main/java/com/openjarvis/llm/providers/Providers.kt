package com.openjarvis.llm.providers

import com.openjarvis.llm.LLMProvider

class GroqProvider(override val baseUrl: String = "https://api.groq.com/openai/v1") : LLMProvider {
    override val name: String = "Groq"
}

class GeminiProvider(override val baseUrl: String = "https://generativelanguage.googleapis.com/v1") : LLMProvider {
    override val name: String = "Google Gemini"
}

class OpenRouterProvider(override val baseUrl: String = "https://openrouter.ai/api/v1") : LLMProvider {
    override val name: String = "OpenRouter"
}

class AnthropicProvider(override val baseUrl: String = "https://api.anthropic.com/v1") : LLMProvider {
    override val name: String = "Anthropic Claude"
}

class OpenAIProvider(override val baseUrl: String = "https://api.openai.com/v1") : LLMProvider {
    override val name: String = "OpenAI"
}

class OllamaProvider(override val baseUrl: String = "http://localhost:11434/api") : LLMProvider {
    override val name: String = "Ollama"
}

class CustomProvider(override val baseUrl: String) : LLMProvider {
    override val name: String = "Custom"
}