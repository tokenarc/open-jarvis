package com.openjarvis.agent

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Persistent diagnostics, stack traces and Ollama-generated patch proposals.
 * Proposals are never applied inside the running app: they must pass GitHub CI first.
 */
class RepairCenter(context: Context) {
    private val app = context.applicationContext
    private val history = app.getSharedPreferences("jarvis_repair_history_v1", Context.MODE_PRIVATE)
    private val provider = runCatching {
        val key = androidx.security.crypto.MasterKey.Builder(app)
            .setKeyScheme(androidx.security.crypto.MasterKey.KeyScheme.AES256_GCM).build()
        androidx.security.crypto.EncryptedSharedPreferences.create(
            app, "jarvis_encrypted_prefs", key,
            androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }.getOrElse { app.getSharedPreferences("jarvis_encrypted_prefs", Context.MODE_PRIVATE) }
    private val http = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    data class Event(
        val id: String,
        val time: Long,
        val code: String,
        val stage: String,
        val summary: String,
        val stackTrace: String,
        val status: String,
        val patchProposal: String
    )

    @Synchronized
    fun recordFailure(error: Throwable, stage: String): Event {
        val diagnosis = ErrorDiagnosis.fromThrowable(error, stage)
        return append(
            diagnosis.code, stage, diagnosis.reason,
            android.util.Log.getStackTraceString(error).take(MAX_TRACE),
            "DIAGNOSED", ""
        )
    }

    @Synchronized
    fun recordFailure(code: String, stage: String, summary: String, trace: String = ""): Event =
        append(code, stage, summary, trace.take(MAX_TRACE), "DIAGNOSED", "")

    @Synchronized
    fun recordStatus(id: String, status: String, proposal: String = "") {
        val items = readArray()
        for (i in 0 until items.length()) {
            val item = items.optJSONObject(i) ?: continue
            if (item.optString("id") == id) {
                item.put("status", status.take(48))
                if (proposal.isNotBlank()) item.put("patch", proposal.take(MAX_PATCH))
                break
            }
        }
        history.edit().putString(KEY_EVENTS, items.toString()).apply()
    }

    fun events(): List<Event> {
        val items = readArray()
        return (0 until items.length()).mapNotNull { i ->
            val o = items.optJSONObject(i) ?: return@mapNotNull null
            Event(
                o.optString("id"), o.optLong("time"), o.optString("code"),
                o.optString("stage"), o.optString("summary"), o.optString("trace"),
                o.optString("status"), o.optString("patch")
            )
        }.sortedByDescending { it.time }
    }

    fun latest(): Event? = events().firstOrNull()

    /** Sends only diagnostic context to the configured local Ollama endpoint.
     * Returns a review-only unified diff; it never writes source files or executes code.
     */
    suspend fun generatePatchProposal(event: Event = latest()
        ?: throw IllegalStateException("No diagnostic event available")): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val baseUrl = (provider.getString("provider_base_url", "http://127.0.0.1:11434")
                    ?: "http://127.0.0.1:11434").trimEnd('/')
                val model = provider.getString("provider_model", "qwen2.5:1.5b")
                    ?.takeIf { it.isNotBlank() } ?: "qwen2.5:1.5b"
                val prompt = """
You are the Kotlin patch proposal generator for Open Jarvis Android.
Analyze this diagnostic and propose a minimal unified diff only.
Allowed paths: app/src/main/java/com/openjarvis/** and app/src/test/java/com/openjarvis/**.
Never edit Euronics assets, provider configuration, signing files, secrets, manifests or Gradle dependencies.
Never remove existing features. Never weaken permission checks or safety confirmations.
Do not repeat device actions or introduce automatic destructive retries.
If evidence is insufficient, return exactly: NO_SAFE_PATCH
Error code: ${event.code}
Stage: ${event.stage}
Summary: ${event.summary}
Stack trace:
${event.stackTrace.take(7000)}
""".trimIndent()
                val body = JSONObject().apply {
                    put("model", model)
                    put("stream", false)
                    put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", prompt)))
                    put("options", JSONObject().put("temperature", 0.1).put("num_predict", 1800))
                }.toString()
                val request = Request.Builder().url("$baseUrl/api/chat")
                    .post(body.toRequestBody("application/json".toMediaType()))
                    .build()
                http.newCall(request).execute().use { response ->
                    val raw = response.body?.string().orEmpty()
                    check(response.isSuccessful) { "Ollama HTTP ${response.code}: ${raw.take(400)}" }
                    val rawProposal = JSONObject(raw).optJSONObject("message")?.optString("content").orEmpty().trim()
                    val proposal = rawProposal
                        .replace(Regex("^```(?:diff)?\\s*"), "")
                        .replace(Regex("\\s*```$"), "")
                        .trim()
                    check(proposal.isNotBlank()) { "Ollama returned an empty patch proposal" }
                    val safe = if (proposal == "NO_SAFE_PATCH") proposal else {
                        check(RepairPatchPolicy.validateDiff(proposal)) {
                            "Patch failed path allow-list or unified-diff validation"
                        }
                        proposal
                    }
                    recordStatus(event.id, if (safe == "NO_SAFE_PATCH") "NO_SAFE_PATCH" else "PATCH_PROPOSED", safe)
                    safe
                }
            }
        }

    suspend fun checkOllama(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val baseUrl = (provider.getString("provider_base_url", "http://127.0.0.1:11434")
                ?: "http://127.0.0.1:11434").trimEnd('/')
            val model = provider.getString("provider_model", "qwen2.5:1.5b")
                ?.takeIf { it.isNotBlank() } ?: "qwen2.5:1.5b"
            val request = Request.Builder().url("$baseUrl/api/tags").get().build()
            http.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                check(response.isSuccessful) { "Ollama HTTP ${response.code}" }
                val models = JSONObject(raw).optJSONArray("models") ?: JSONArray()
                val found = (0 until models.length()).any { i ->
                    val name = models.optJSONObject(i)?.optString("name").orEmpty()
                    name == model || name.startsWith("$model:")
                }
                if (found) "Ollama reachable; model $model is installed."
                else "Ollama reachable, but model $model is missing. Run: ollama pull $model"
            }
        }
    }

    suspend fun pullConfiguredModel(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val baseUrl = (provider.getString("provider_base_url", "http://127.0.0.1:11434")
                ?: "http://127.0.0.1:11434").trimEnd('/')
            val model = provider.getString("provider_model", "qwen2.5:1.5b")
                ?.takeIf { it.isNotBlank() } ?: "qwen2.5:1.5b"
            val body = JSONObject().put("name", model).put("stream", false).toString()
            val request = Request.Builder().url("$baseUrl/api/pull")
                .post(body.toRequestBody("application/json".toMediaType())).build()
            http.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                check(response.isSuccessful) { "Ollama pull failed HTTP ${response.code}: ${raw.take(300)}" }
                "Ollama model pull completed for $model. Run Check Ollama to verify."
            }
        }
    }

    fun dashboardSummary(): String {
        val all = events()
        val failed = all.size
        val proposed = all.count { it.status == "PATCH_PROPOSED" }
        val recovered = all.count { it.status == "VERIFIED_RECOVERY" }
        return "Diagnostics: $failed  •  Patch proposals: $proposed  •  Verified recoveries: $recovered"
    }

    private fun append(code: String, stage: String, summary: String, trace: String, status: String, patch: String): Event {
        val id = "${System.currentTimeMillis()}-${code.hashCode()}"
        val item = JSONObject().apply {
            put("id", id); put("time", System.currentTimeMillis()); put("code", code)
            put("stage", stage); put("summary", summary.take(600)); put("trace", trace)
            put("status", status); put("patch", patch)
        }
        val items = readArray()
        items.put(item)
        val bounded = JSONArray()
        val start = (items.length() - MAX_EVENTS).coerceAtLeast(0)
        for (i in start until items.length()) bounded.put(items.getJSONObject(i))
        history.edit().putString(KEY_EVENTS, bounded.toString()).apply()
        return Event(id, item.getLong("time"), code, stage, summary, trace, status, patch)
    }

    private fun readArray(): JSONArray = runCatching {
        JSONArray(history.getString(KEY_EVENTS, "[]") ?: "[]")
    }.getOrDefault(JSONArray())

    companion object {
        private const val KEY_EVENTS = "events"
        private const val MAX_EVENTS = 50
        private const val MAX_TRACE = 12000
        private const val MAX_PATCH = 24000
    }
}
