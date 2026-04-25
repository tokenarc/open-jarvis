package com.openjarvis.agent

import android.content.Context
import com.openjarvis.graphify.GraphifyRepository
import com.openjarvis.intelligence.TaskRouter
import com.openjarvis.llm.UniversalAdapter
import com.openjarvis.skills.SkillEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class PromptEngine(private val context: Context) {
    
    private val graphifyRepo = GraphifyRepository(context)
    private val skillsEngine = SkillEngine(context)
    private val taskRouter = TaskRouter(context)
    private val conversationContext = ConversationContext()
    
    suspend fun analyzeIntent(prompt: String): Intent = withContext(Dispatchers.IO) {
        val resolvedPrompt = conversationContext.resolveReferences(prompt)
        
        val intentJson = IntentAnalyzer.analyze(resolvedPrompt)
        
        parseIntentFromJson(resolvedPrompt, intentJson)
    }
    
    private fun parseIntentFromJson(rawPrompt: String, json: JSONObject): Intent {
        val phases = json.optJSONArray("phases")?.let { arr ->
            (0 until arr.length()).map { i ->
                parsePhase(arr.getJSONObject(i))
            }
        } ?: emptyList()
        
        val conditions = json.optJSONArray("conditions")?.let { arr ->
            (0 until arr.length()).map { i ->
                parseCondition(arr.getJSONObject(i))
            }
        } ?: emptyList()
        
        val appsRequired = json.optJSONArray("appsRequired")?.let { arr ->
            (0 until arr.length()).map { arr.getString(it) }
        } ?: emptyList()
        
        val dataInputs = mutableMapOf<String, String>()
        val inputsObj = json.optJSONObject("dataInputs")
        inputsObj?.keys()?.forEach { key ->
            dataInputs[key] = inputsObj.getString(key)
        }
        
        return Intent(
            rawPrompt = rawPrompt,
            goalSummary = json.optString("goalSummary", rawPrompt),
            phases = phases,
            conditions = conditions,
            successCriteria = json.optString("successCriteria", "Task completed"),
            estimatedSteps = phases.sumOf { it.actions.size },
            requiresConfirmation = json.optBoolean("requiresConfirmation", false),
            appsRequired = appsRequired,
            dataInputs = dataInputs
        )
    }
    
    private fun parsePhase(json: JSONObject): Phase {
        val actionsJson = json.optJSONArray("actions")
        val actions = actionsJson?.let { arr ->
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                Action(
                    action = obj.getString("action"),
                    packageName = obj.optString("package", null),
                    text = obj.optString("text", null),
                    value = obj.optString("value", null)
                )
            }
        } ?: emptyList()
        
        val failureStrategy = when (json.optString("onFailure", "retry")) {
            "retry" -> PhaseFailureStrategy.Retry
            "abort" -> PhaseFailureStrategy.Abort
            else -> {
                val fallbackId = json.optString("fallbackPhaseId")
                if (fallbackId != null) PhaseFailureStrategy.Fallback(fallbackId)
                else PhaseFailureStrategy.Retry
            }
        }
        
        return Phase(
            id = json.optString("id", "phase_${System.nanoTime()}"),
            description = json.optString("description", ""),
            actions = actions,
            expectedOutcome = json.optString("expectedOutcome", ""),
            onSuccess = json.optString("onSuccess", null),
            onFailure = failureStrategy
        )
    }
    
    private fun parseCondition(json: JSONObject): Condition {
        val type = try {
            ConditionType.valueOf(json.optString("type", "SCREEN_CONTAINS"))
        } catch (e: Exception) {
            ConditionType.SCREEN_CONTAINS
        }
        
        return Condition(
            type = type,
            check = json.optString("check", ""),
            ifTrue = json.optString("ifTrue", ""),
            ifFalse = json.optString("ifFalse", "")
        )
    }
    
    fun buildExecutionPlan(intent: Intent, phases: List<Phase>): ExecutionPlan {
        return ExecutionPlan(
            phases = phases,
            requiresConfirmation = intent.requiresConfirmation,
            conditions = intent.conditions,
            successCriteria = intent.successCriteria
        )
    }
    
    fun getConversationContext(): ConversationContext = conversationContext
    
    data class Intent(
        val rawPrompt: String,
        val goalSummary: String,
        val phases: List<Phase>,
        val conditions: List<Condition>,
        val successCriteria: String,
        val estimatedSteps: Int,
        val requiresConfirmation: Boolean,
        val appsRequired: List<String>,
        val dataInputs: Map<String, String>
    )
    
    data class Phase(
        val id: String,
        val description: String,
        val actions: List<Action>,
        val expectedOutcome: String,
        val onSuccess: String?,
        val onFailure: PhaseFailureStrategy
    )
    
    sealed class PhaseFailureStrategy {
        object Retry : PhaseFailureStrategy()
        object Abort : PhaseFailureStrategy()
        data class Fallback(val phaseId: String) : PhaseFailureStrategy()
        data class AskUser(val question: String) : PhaseFailureStrategy()
    }
    
    data class Condition(
        val type: ConditionType,
        val check: String,
        val ifTrue: String,
        val ifFalse: String
    )
    
    enum class ConditionType {
        SCREEN_CONTAINS, SCREEN_NOT_CONTAINS,
        TASK_SUCCEEDED, TASK_FAILED,
        TIME_ELAPSED, USER_CONFIRMED
    }
    
    data class ExecutionPlan(
        val phases: List<Phase>,
        val requiresConfirmation: Boolean,
        val conditions: List<Condition>,
        val successCriteria: String
    )
    
    companion object {
        fun isMultiPhase(prompt: String): Boolean {
            val indicators = listOf(", then", " and if", "after that", " if ", " otherwise", "but first", "next")
            return indicators.any { prompt.lowercase().contains(it) }
        }
    }
}

object IntentAnalyzer {
    private val systemPrompt = """
You are an intent analyzer for an Android AI agent.
Analyze the user's prompt and extract:
1. What they ultimately want to achieve (goal)
2. What phases/stages are needed
3. What conditions or branches exist (if/else)
4. What data is embedded in the prompt
5. What apps will be needed
6. Whether any risky actions are involved (send, post, call, buy)

Respond ONLY with JSON matching Intent schema.
    """.trimIndent()
    
    suspend fun analyze(prompt: String): JSONObject = withContext(Dispatchers.IO) {
        val adapter = UniversalAdapter.getModelManager(context)
        val fullPrompt = "$systemPrompt\n\nUser prompt: $prompt"
        
        val result = adapter.complete(systemPrompt, prompt)
        result.getOrNull()?.let { jsonStr ->
            try {
                JSONObject(jsonStr.trim())
            } catch (e: Exception) {
                JSONObject()
            }
        } ?: JSONObject()
    }
}

class ConversationContext {
    
    private val turns = mutableListOf<Turn>()
    private val entityMemory = mutableMapOf<String, String>()
    private var lastUsedApp: String? = null
    private var lastCommand: String? = null
    
    data class Turn(
        val role: Role,
        val text: String,
        val timestamp: Long,
        val relatedApps: List<String> = emptyList()
    )
    
    enum class Role { USER, JARVIS }
    
    fun addTurn(role: Role, text: String, apps: List<String> = emptyList()) {
        turns.add(Turn(role, text, System.currentTimeMillis(), apps))
        
        apps.firstOrNull()?.let { lastUsedApp = it }
        if (role == Role.USER) lastCommand = text
        
        extractEntities(text, apps)
    }
    
    private fun extractEntities(text: String, apps: List<String>) {
        val words = text.split(" ")
        for (word in words) {
            if (word.first().isUpperCase() && word.length > 2) {
                if (apps.isNotEmpty()) {
                    entityMemory[word.lowercase()] = "$word (${apps.first()})"
                }
            }
        }
    }
    
    fun resolveReferences(input: String): String {
        var resolved = input
        
        resolved = resolved.replace(Regex("\\bhim\\b", RegexOption.IGNORE_CASE), entityMemory["him"] ?: "him")
        resolved = resolved.replace(Regex("\\bher\\b", RegexOption.IGNORE_CASE), entityMemory["her"] ?: "her")
        resolved = resolved.replace(Regex("\\bit\\b", RegexOption.IGNORE_CASE), entityMemory["it"] ?: lastUsedApp ?: "it")
        resolved = resolved.replace(Regex("\\bthat\\b", RegexOption.IGNORE_CASE), lastCommand ?: "that")
        resolved = resolved.replace(Regex("\\bnow\\b", RegexOption.IGNORE_CASE), if (lastUsedApp != null) "continue in $lastUsedApp" else "now")
        resolved = resolved.replace(Regex("\\bdo that again\\b", RegexOption.IGNORE_CASE), lastCommand ?: "do that again")
        
        return resolved
    }
    
    fun buildContextString(): String {
        return turns.takeLast(5).joinToString("\n") {
            "${if (it.role == Role.USER) "User" else "Jarvis"}: ${it.text}"
        }
    }
    
    fun clearSession() {
        turns.clear()
        entityMemory.clear()
        lastUsedApp = null
        lastCommand = null
    }
}