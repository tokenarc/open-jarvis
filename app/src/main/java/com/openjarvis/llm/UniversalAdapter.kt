package com.openjarvis.llm

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.openjarvis.llm.providers.*
import com.openjarvis.local.ModelManager

class UniversalAdapter(private val context: Context) {
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "jarvis_encrypted_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    fun getActiveProvider(): LLMProvider {
        val wasOllama = prefs.getString("provider_name", "") == "Ollama (Local)"
        val baseUrl = if (wasOllama) {
            (prefs.getString("provider_base_url", DEFAULT_OLLAMA_URL) ?: DEFAULT_OLLAMA_URL)
                .trim().trimEnd('/').ifBlank { DEFAULT_OLLAMA_URL }
        } else DEFAULT_OLLAMA_URL
        val model = if (wasOllama) {
            (prefs.getString("provider_model", DEFAULT_OLLAMA_MODEL) ?: DEFAULT_OLLAMA_MODEL)
                .trim().ifBlank { DEFAULT_OLLAMA_MODEL }
        } else DEFAULT_OLLAMA_MODEL
        // Deliberately route every request to local Ollama. No cloud or llama.cpp fallback.
        if (!wasOllama || prefs.getString("provider_model", "").isNullOrBlank() ||
            prefs.getString("provider_base_url", "").isNullOrBlank()) {
            prefs.edit()
                .putString("provider_name", "Ollama (Local)")
                .putString("provider_base_url", baseUrl)
                .putString("provider_api_key", "")
                .putString("provider_model", model)
                .apply()
        }
        return OllamaProvider(baseUrl, model)
    }
    
    suspend fun complete(
        systemPrompt: String,
        userMessage: String
    ): Result<String> = getActiveProvider().complete(systemPrompt, userMessage)
    
    suspend fun testConnection(): Result<Long> = getActiveProvider().testConnection()
    
    fun saveSettings(name: String, baseUrl: String, apiKey: String, model: String) {
        prefs.edit().apply {
            putString("provider_name", "Ollama (Local)")
            putString("provider_base_url", baseUrl.trim().trimEnd('/').ifBlank { DEFAULT_OLLAMA_URL })
            putString("provider_api_key", "")
            putString("provider_model", model.trim().ifBlank { DEFAULT_OLLAMA_MODEL })
            apply()
        }
    }
    
    fun getProviderName(): String = "Ollama (Local)"
    
    companion object {
        private var modelManager: ModelManager? = null
        
        const val DEFAULT_OLLAMA_URL = "http://127.0.0.1:11434"
        const val DEFAULT_OLLAMA_MODEL = "qwen2.5:1.5b"

        val AVAILABLE_PROVIDERS = listOf("Ollama (Local)")
        
        fun getModelManager(context: Context): ModelManager {
            return modelManager ?: synchronized(this) {
                modelManager ?: ModelManager(context.applicationContext).also { modelManager = it }
            }
        }
        
        fun isLocalModelLoaded(): Boolean {
            return modelManager?.state?.value is ModelManager.ModelState.Loaded
        }
        
        suspend fun loadLocalModel(tier: ModelManager.ModelTier): Result<Unit> {
            return modelManager?.loadModel(tier) ?: Result.failure(Exception("ModelManager not initialized"))
        }
        
        fun getDefaultModel(provider: String): String = when (provider) {
            "Groq" -> "openai/gpt-oss-120b"
            "Google Gemini" -> "gemini-1.5-flash"
            "OpenRouter" -> "meta-llama/llama-3-8b-instruct:free"
            "Anthropic Claude" -> "claude-haiku-4-20250514"
            "OpenAI" -> "gpt-4o-mini"
            "Ollama (Local)" -> DEFAULT_OLLAMA_MODEL
            else -> DEFAULT_OLLAMA_MODEL
        }
    }
}