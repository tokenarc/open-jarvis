package com.openjarvis.agent

import android.content.Context
import com.openjarvis.graphify.GraphifyRepository
import com.openjarvis.llm.LLMProvider
import com.openjarvis.llm.UniversalAdapter
import kotlinx.coroutines.flow.MutableStateFlow

class ProviderFallbackChain(private val context: Context) {
    
    private val graphifyRepo = GraphifyRepository(context)
    private val cooldownMap = mutableMapOf<String, Long>()
    private val _notification = MutableStateFlow<String?>(null)
    
    private val prefs = context.getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE)
    
    private val fallbackOrder: List<String>
        get() = prefs.getString("fallback_order", "Groq,Google Gemini,OpenRouter,Anthropic Claude,OpenAI")
            ?.split(",") ?: emptyList()
    
    suspend fun completeWithFallback(system: String, user: String): Result<String> {
        val providers = fallbackOrder.mapNotNull { name ->
            try {
                val adapter = UniversalAdapter(context)
                val provider = getProvider(name, adapter)
                provider to name
            } catch (e: Exception) {
                null
            }
        }
        
        for ((provider, name) in providers) {
            if (isInCooldown(name)) continue
            
            val result = runCatching { provider.complete(system, user) }
            
            if (result.isSuccess) {
                return result
            }
            
            val errorMsg = result.exceptionOrNull()?.message ?: ""
            
            graphifyRepo.logProviderFailure(name, errorMsg)
            
            if (errorMsg.contains("429")) {
                cooldownMap[name] = System.currentTimeMillis() + 60_000
                _notification.value = "Rate limited by $name, trying next provider..."
            }
            
            if (errorMsg.contains("401")) {
                cooldownMap[name] = System.currentTimeMillis() + 3600_000
            }
        }
        
        return Result.failure(Exception("All providers failed or rate limited"))
    }
    
    private fun getProvider(name: String, adapter: UniversalAdapter): LLMProvider {
        return adapter.getActiveProvider()
    }
    
    private fun isInCooldown(name: String): Boolean {
        val cooldownUntil = cooldownMap[name] ?: return false
        return System.currentTimeMillis() < cooldownUntil
    }
    
    fun getNotification() = _notification
}

object JarvisErrorTranslator {
    
    fun translate(error: Throwable, providerName: String?): String {
        return when {
            error.message?.contains("401") == true ->
                "API key rejected by ${providerName ?: "provider"}. Check your key in Settings."
            
            error.message?.contains("429") == true ->
                "Rate limited by ${providerName ?: "provider"}. Trying next provider..."
            
            error.message?.contains("503") == true ->
                "${providerName ?: "Provider"} is down. Try a different provider in Settings."
            
            error.message?.contains("timeout") == true ->
                "Request timed out after 30s. Your internet may be slow."
            
            error.message?.contains("Unable to resolve host") == true ->
                "No internet connection. Switch to Local Model in Settings for offline use."
            
            error is JSONException ->
                "AI returned unexpected response. Retrying with stricter instructions..."
            
            error.message?.contains("SecurityException") == true ->
                "Permission denied. Check that Accessibility Service is still enabled."
            
            error.message?.contains("NullPointerException") == true ->
                "Screen changed unexpectedly. Trying to recover..."
            
            else -> "Something went wrong: ${error.message?.take(100) ?: "Unknown error"}"
        }
    }
}