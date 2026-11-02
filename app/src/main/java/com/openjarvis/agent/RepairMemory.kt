package com.openjarvis.agent

import android.content.Context
import org.json.JSONObject
import java.util.Locale

/**
 * Small persistent memory for verified command corrections and recurring failures.
 * Only successful command mappings are learned; failed model guesses are never saved.
 */
class RepairMemory(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "jarvis_repair_memory_v1",
        Context.MODE_PRIVATE
    )

    @Synchronized
    fun normalize(command: String): String {
        val key = normalizeKey(command)
        if (key.isBlank()) return command
        val learned = runCatching {
            JSONObject(prefs.getString(KEY_ALIASES, "{}") ?: "{}").optString(key, "")
        }.getOrDefault("")
        return learned.ifBlank { command }
    }

    @Synchronized
    fun rememberVerifiedCorrection(original: String, corrected: String) {
        val key = normalizeKey(original)
        val value = corrected.trim()
        if (key.isBlank() || value.isBlank() || key == normalizeKey(value)) return
        runCatching {
            val aliases = JSONObject(prefs.getString(KEY_ALIASES, "{}") ?: "{}")
            aliases.put(key, value)
            // Bound storage so repeated corrections cannot grow indefinitely.
            val bounded = JSONObject()
            val keys = aliases.keys().asSequence().toList().takeLast(MAX_ALIASES)
            keys.forEach { alias -> bounded.put(alias, aliases.optString(alias)) }
            prefs.edit().putString(KEY_ALIASES, bounded.toString()).apply()
        }
    }

    @Synchronized
    fun recordFailure(code: String, stage: String) {
        val key = normalizeKey(code).ifBlank { "unknown" }
        val counts = runCatching {
            JSONObject(prefs.getString(KEY_FAILURES, "{}") ?: "{}")
        }.getOrDefault(JSONObject())
        counts.put(key, counts.optInt(key, 0) + 1)
        prefs.edit()
            .putString(KEY_FAILURES, counts.toString())
            .putString(KEY_LAST_FAILURE, "$key@$stage")
            .apply()
    }

    @Synchronized
    fun recordRecovery(code: String) {
        val key = normalizeKey(code).ifBlank { "unknown" }
        val recoveries = runCatching {
            JSONObject(prefs.getString(KEY_RECOVERIES, "{}") ?: "{}")
        }.getOrDefault(JSONObject())
        recoveries.put(key, recoveries.optInt(key, 0) + 1)
        prefs.edit()
            .putString(KEY_RECOVERIES, recoveries.toString())
            .putString(KEY_LAST_RECOVERY, key)
            .apply()
    }

    fun summary(): String {
        val lastFailure = prefs.getString(KEY_LAST_FAILURE, null)
        val lastRecovery = prefs.getString(KEY_LAST_RECOVERY, null)
        return buildString {
            append("Verified repair memory enabled.")
            if (!lastFailure.isNullOrBlank()) append(" Last failure: ").append(lastFailure).append(".")
            if (!lastRecovery.isNullOrBlank()) append(" Last recovered: ").append(lastRecovery).append(".")
        }
    }

    private fun normalizeKey(value: String): String =
        value.trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")

    companion object {
        private const val KEY_ALIASES = "verified_aliases"
        private const val KEY_FAILURES = "failure_counts"
        private const val KEY_RECOVERIES = "recovery_counts"
        private const val KEY_LAST_FAILURE = "last_failure"
        private const val KEY_LAST_RECOVERY = "last_recovery"
        private const val MAX_ALIASES = 100
    }
}
