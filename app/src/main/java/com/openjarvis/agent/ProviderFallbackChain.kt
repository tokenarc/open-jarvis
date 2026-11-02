package com.openjarvis.agent

import android.content.Context
import com.openjarvis.llm.LLMProvider
import com.openjarvis.llm.UniversalAdapter
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONException

class ProviderFallbackChain(private val context: Context) {

    private val cooldownMap = mutableMapOf<String, Long>()
    private val _notification = MutableStateFlow<String?>(null)

    suspend fun completeWithFallback(
        system: String,
        user: String
    ): Result<String> {
        val adapter = UniversalAdapter(context)
        val name = adapter.getProviderName()

        if (isInCooldown(name)) {
            return Result.failure(Exception("$name is temporarily rate limited"))
        }

        val provider: LLMProvider = adapter.getActiveProvider()

        val result: Result<String> = try {
            provider.complete(system, user)
        } catch (e: Exception) {
            Result.failure(e)
        }

        if (result.isSuccess) {
            _notification.value = null
            return result
        }

        val error = result.exceptionOrNull()
        val errorMsg = error?.message ?: "Unknown provider error"

        if (errorMsg.contains("429")) {
            cooldownMap[name] = System.currentTimeMillis() + 60_000
            _notification.value = "Rate limited by $name"
        } else if (errorMsg.contains("401")) {
            cooldownMap[name] = System.currentTimeMillis() + 3_600_000
            _notification.value = "API key rejected by $name"
        } else {
            _notification.value = "$name failed: $errorMsg"
        }

        return Result.failure(
            Exception("$name failed: $errorMsg", error)
        )
    }

    private fun isInCooldown(name: String): Boolean {
        val cooldownUntil = cooldownMap[name] ?: return false
        if (System.currentTimeMillis() >= cooldownUntil) {
            cooldownMap.remove(name)
            return false
        }
        return true
    }

    fun getNotification() = _notification
}

object JarvisErrorTranslator {

    fun translate(error: Throwable, providerName: String?): String {
        return when {
            error.message?.contains("401") == true ->
                "API key rejected by ${providerName ?: "provider"}. Check your key in Settings."

            error.message?.contains("429") == true ->
                "Rate limited by ${providerName ?: "provider"}. Try again later."

            error.message?.contains("503") == true ->
                "${providerName ?: "Provider"} is down. Try again later."

            error.message?.contains("timeout", ignoreCase = true) == true ->
                "Request timed out. Check your internet connection."

            error.message?.contains("Unable to resolve host") == true ->
                "No internet connection. Switch to Local Model in Settings for offline use."

            error is JSONException ->
                "AI returned an unexpected response."

            error.message?.contains("SecurityException") == true ->
                "Permission denied. Check Accessibility Service."

            error.message?.contains("NullPointerException") == true ->
                "Screen changed unexpectedly. Trying to recover..."

            else ->
                "Something went wrong: ${error.message?.take(100) ?: "Unknown error"}"
        }
    }
}
