package com.openjarvis.agent

import android.content.Context
import java.security.MessageDigest

/**
 * Persistent guard for automatic recovery.
 *
 * It allows one bounded retry for a retryable failure before any device action starts.
 * The cooldown survives process restarts, preventing infinite retry loops. It deliberately
 * never retries a partially executed device-action sequence.
 */
class SelfHealingEngine(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "jarvis_self_healing_v1",
        Context.MODE_PRIVATE
    )

    @Synchronized
    fun claimRetry(command: String, errorCode: String): Boolean {
        val commandKey = digest(command.trim().lowercase())
        val codeKey = digest(errorCode.trim().uppercase())
        val key = "retry_${commandKey}_${codeKey}"
        val now = System.currentTimeMillis()
        val lastAttempt = prefs.getLong(key, 0L)
        if (lastAttempt > 0L && now - lastAttempt < RETRY_COOLDOWN_MS) return false
        prefs.edit()
            .putLong(key, now)
            .putString(KEY_LAST_RETRY, "${errorCode.uppercase()} for command fingerprint ${commandKey.take(8)}")
            .apply()
        return true
    }

    @Synchronized
    fun recordSuccess(command: String) {
        val commandKey = digest(command.trim().lowercase())
        val prefix = "retry_${commandKey}_"
        val editor = prefs.edit()
        prefs.all.keys.filter { it.startsWith(prefix) }.forEach { editor.remove(it) }
        editor.putString(KEY_LAST_SUCCESS, System.currentTimeMillis().toString()).apply()
    }

    fun summary(): String {
        val lastRetry = prefs.getString(KEY_LAST_RETRY, null)
        val lastSuccess = prefs.getString(KEY_LAST_SUCCESS, null)
        return buildString {
            append("Self-healing policy active: bounded retry, persistent cooldown, no replay after device actions.")
            if (!lastRetry.isNullOrBlank()) append(" Last recovery attempt: ").append(lastRetry).append(".")
            if (!lastSuccess.isNullOrBlank()) append(" Last successful recovery timestamp: ").append(lastSuccess).append(".")
        }
    }

    private fun digest(value: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val RETRY_COOLDOWN_MS = 10 * 60 * 1000L
        private const val KEY_LAST_RETRY = "last_retry"
        private const val KEY_LAST_SUCCESS = "last_success"
    }
}
