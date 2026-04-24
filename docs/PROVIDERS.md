# LLM Providers - Open Jarvis

## Overview

Open Jarvis uses a universal adapter pattern to support multiple LLM providers through a single interface. Any provider that speaks OpenAI-compatible JSON can be added easily.

## Architecture

```
┌─────────────────────────────────────────────┐
│              AgentCore                        │
│    (parses commands → builds action plans)    │
└─────────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────┐
│            UniversalAdapter                 │
│    (routes to selected provider)              │
└─────────────────────────────────────────────┘
                      │
          ┌───────────┼───────────┐
          ▼           ▼           ▼
   ┌──────────┐ ┌──────────┐ ┌──────────┐
   │ Groq    │ │ Gemini  │ │ Custom  │
   │Provider │ │Provider │ │Provider │
   └──────────┘ └──────────┘ └──────────┘
```

## Adding a Custom Provider

### Step 1: Implement the Interface

```kotlin
package com.openjarvis.llm.providers

import com.openjarvis.llm.LLMProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MyCustomProvider(
    override val baseUrl: String,
    private val apiKey: String,
    private val model: String
) : LLMProvider {
    
    override val name: String = "My Custom"
    
    override suspend fun complete(
        systemPrompt: String,
        userMessage: String,
        context: String
    ): String = withContext(Dispatchers.IO) {
        // Implement API call here
        val requestBody = buildRequestBody(systemPrompt, userMessage, context)
        val response = httpClient.post(baseUrl) {
            header("Authorization", "Bearer $apiKey")
            header("Content-Type", "application/json")
            setBody(requestBody)
        }
        parseResponse(response)
    }
    
    private fun buildRequestBody(system: String, user: String, context: String): String {
        // Build provider-specific JSON
    }
    
    private fun parseResponse(response: Response): String {
        // Parse provider-specific response
    }
}
```

### Step 2: Register in UniversalAdapter

```kotlin
// In UniversalAdapter.kt
fun setProviderByName(name: String, baseUrl: String = ""): LLMProvider {
    currentProvider = when (name.lowercase()) {
        "mycustom" -> MyCustomProvider(baseUrl, apiKey, model)
        // ... other providers
    }
    return currentProvider
}
```

### Step 3: Add to Settings UI

```kotlin
// In SettingsActivity.kt
val availableProviders = listOf(
    "Groq",
    "Google Gemini",
    "OpenRouter",
    "Anthropic Claude",
    "OpenAI",
    "Ollama (Local)",
    "My Custom"  // Add here
)
```

## Provider Implementation Examples

### OpenAI-Compatible API

For servers that follow OpenAI's `/v1/chat/completions` format:

```kotlin
class OpenAICompatibleProvider(
    override val baseUrl: String,
    private val apiKey: String,
    private val model: String
) : LLMProvider {
    
    override val name: String = "Custom (OpenAI Compatible)"
    
    override suspend fun complete(
        systemPrompt: String,
        userMessage: String,
        context: String
    ): String = withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val requestBody = """
        {
            "model": "$model",
            "messages": [
                {"role": "system", "content": "$systemPrompt"},
                {"role": "user", "content": "$userMessage\n\nContext: $context"}
            ],
            "temperature": 0.7,
            "max_tokens": 1024
        }
        """.trimIndent()
        
        val request = Request.Builder()
            .url("$baseUrl/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create(requestBody, MediaType.get("application/json")))
            .build()
        
        val response = client.newCall(request).execute()
        val json = JSONObject(response.body()?.string())
        json.getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
    }
}
```

### Groq API (Direct)

```kotlin
class GroqProvider(
    private val apiKey: String,
    private val model: String = "llama-3.1-70b-versatile"
) : LLMProvider {
    
    override val name: String = "Groq"
    override val baseUrl: String = "https://api.groq.com/openai/v1"
    
    override suspend fun complete(
        systemPrompt: String,
        userMessage: String,
        context: String
    ): String = withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val requestBody = """
        {
            "model": "$model",
            "messages": [
                {"role": "system", "content": "$systemPrompt"},
                {"role": "user", "content": "$userMessage\n\nContext: $context"}
            ],
            "temperature": 0.7,
            "max_tokens": 1024
        }
        """.trimIndent()
        
        val request = Request.Builder()
            .url("$baseUrl/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create(requestBody, MediaType.get("application/json")))
            .build()
        
        val response = client.newCall(request).execute()
        val json = JSONObject(response.body()?.string())
        json.getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
    }
}
```

### Gemini API

```kotlin
class GeminiProvider(
    private val apiKey: String,
    private val model: String = "gemini-1.5-flash"
) : LLMProvider {
    
    override val name: String = "Google Gemini"
    override val baseUrl: String = "https://generativelanguage.googleapis.com/v1"
    
    override suspend fun complete(
        systemPrompt: String,
        userMessage: String,
        context: String
    ): String = withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val fullPrompt = "$systemPrompt\n\nUser: $userMessage\n\nContext: $context"
        
        val requestBody = """
        {
            "contents": [{"parts": [{"text": "$fullPrompt"}]}],
            "generationConfig": {
                "temperature": 0.7,
                "maxOutputTokens": 1024
            }
        }
        """.trimIndent()
        
        val request = Request.Builder()
            .url("$baseUrl/models/$model:generateContent?key=$apiKey")
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create(requestBody, MediaType.get("application/json")))
            .build()
        
        val response = client.newCall(request).execute()
        val json = JSONObject(response.body()?.string())
        json.getJSONArray("candidates")
            .getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
            .getJSONObject(0)
            .getString("text")
    }
}
```

## Hydra.js Compatibility

Open Jarvis works with [Hydra.js](https://github.com/hydra-ai/hydra) or any local proxy that translates to OpenAI format:

```
┌──────────────┐      OpenAI Format       ┌──────────────┐
│ Open Jarvis  │ ───────────────────────► │ Hydra.js     │
│ (Universal  │      HTTP/JSON          │ (local proxy)│
│  Adapter)   │ ◄─────────────────────── │              │
└──────────────┘      HTTP/JSON          └──────────────┘
                                                │
                                                ▼
                                         ┌──────────────┐
                                         │ llama.cpp    │
                                         │ or other     │
                                         │ local model │
                                         └──────────────┘
```

## Settings Storage

Provider settings are stored in EncryptedSharedPreferences:

| Key | Description |
|-----|-------------|
| `provider_name` | Selected provider (Groq, Gemini, etc.) |
| `provider_base_url` | Custom Base URL (if Custom provider) |
| `provider_api_key` | API key (encrypted) |
| `provider_model` | Model name |

## Environment Variables

For development, you can also use environment variables:

```bash
# In Termux 或 local development
export JARVIS_PROVIDER=groq
export JARVIS_API_KEY=your_api_key
export JARVIS_MODEL=llama-3.1-70b-versatile
```

## Error Handling

Each provider should handle:

| Error | Handling |
|-------|----------|
| Invalid API key | Show error in UI, prompt to check settings |
| Rate limit | Implement exponential backoff |
| Network error | Show offline message, use cache if available |
| Model not found | Fallback to default model |