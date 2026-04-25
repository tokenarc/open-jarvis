package com.openjarvis.skills

import android.content.Context
import com.openjarvis.agent.Action
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.regex.Pattern

class SkillEngine(private val context: Context) {
    
    private val skillsDir: File
        get() = File(context.filesDir, "skills").also { it.mkdirs() }
    
    private var skillsLoaded = false
    private val skillsCache = mutableListOf<Skill>()
    
    suspend fun loadSkills() = withContext(Dispatchers.IO) {
        if (skillsLoaded) return@withContext
        
        copyBuiltinSkillsIfNeeded()
        
        val jsonFiles = skillsDir.listFiles { _, name -> name.endsWith(".json") }
        
        skillsCache.clear()
        jsonFiles?.forEach { file ->
            try {
                val json = JSONObject(file.readText())
                skillsCache.add(parseSkill(json))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        skillsLoaded = true
    }
    
    private fun copyBuiltinSkillsIfNeeded() {
        val builtinDir = File(context.assets, "skills")
        if (!builtinDir.exists()) return
        
        builtinDir.listFiles()?.forEach { assetFile ->
            val destFile = File(skillsDir, assetFile.name)
            if (!destFile.exists()) {
                try {
                    context.assets.open("skills/${assetFile.name}").use { input ->
                        destFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    
    private fun parseSkill(json: JSONObject): Skill {
        val triggerPhrases = json.getJSONArray("triggerPhrases").let { arr ->
            (0 until arr.length()).map { arr.getString(it) }
        }
        
        val variables = json.getJSONArray("variables").let { arr ->
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                SkillVariable(
                    key = obj.getString("key"),
                    description = obj.optString("description", ""),
                    extractFrom = obj.optString("extractFrom", "trigger")
                )
            }
        }
        
        val actionTemplate = json.getJSONArray("actionTemplate").let { arr ->
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                Action(
                    action = obj.getString("action"),
                    packageName = obj.optString("package", null),
                    label = obj.optString("label", null),
                    text = obj.optString("text", null),
                    value = obj.optString("value", null)
                )
            }
        }
        
        val tags = json.getJSONArray("tags").let { arr ->
            (0 until arr.length()).map { arr.getString(it) }
        }
        
        return Skill(
            id = json.getString("id"),
            name = json.optString("name", ""),
            version = json.optString("version", "1.0"),
            author = json.optString("author", ""),
            triggerPhrases = triggerPhrases,
            variables = variables,
            llmHint = json.optString("llmHint", ""),
            actionTemplate = actionTemplate,
            successVerification = json.optString("successVerification", ""),
            tags = tags,
            usageCount = json.optInt("usageCount", 0),
            successRate = json.optDouble("successRate", 1.0).toFloat()
        )
    }
    
    fun findMatch(command: String): Skill? {
        val lower = command.lowercase()
        
        for (skill in skillsCache) {
            for (phrase in skill.triggerPhrases) {
                val pattern = phrase.replace("*", "(.+)")
                val regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE)
                val matcher = regex.matcher(lower)
                
                if (matcher.find()) {
                    return skill
                }
            }
        }
        
        return null
    }
    
    fun extractVariables(command: String, skill: Skill): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val lower = command.lowercase()
        
        for (skillVar in skill.variables) {
            for (phrase in skill.triggerPhrases) {
                val pattern = phrase.replace("*", "(.+)")
                val regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE)
                val matcher = regex.matcher(lower)
                
                if (matcher.find()) {
                    val group = matcher.group(1)?.trim() ?: continue
                    
                    when (skillVar.key) {
                        "contact", "message", "query", "time" -> {
                            result[skillVar.key] = group
                        }
                    }
                    break
                }
            }
        }
        
        return result
    }
    
    fun buildContext(skill: Skill, vars: Map<String, String>): String {
        var hint = skill.llmHint
        for ((key, value) in vars) {
            hint = hint.replace("{$key}", value)
        }
        
        val actionsJson = skill.actionTemplate.joinToString(", ") { action ->
            val value = action.value?.let { v ->
                vars.entries.find { it.key in v }?.value ?: v
            } ?: ""
            """{"action":"${action.action}","value":"$value"}"""
        }
        
        return """
## Skill: ${skill.name}
$hint
Actions: [$actionsJson]
        """.trimIndent()
    }
    
    suspend fun recordUsage(skillId: String, success: Boolean) = withContext(Dispatchers.IO) {
        val index = skillsCache.indexOfFirst { it.id == skillId }
        if (index < 0) return@withContext
        
        val skill = skillsCache[index]
        val newCount = skill.usageCount + 1
        val newRate = if (success) {
            ((skill.successRate * skill.usageCount) + 1f) / newCount
        } else {
            (skill.successRate * skill.usageCount) / newCount
        }
        
        val updated = skill.copy(usageCount = newCount, successRate = newRate)
        skillsCache[index] = updated
        
        saveSkill(updated)
    }
    
    private fun saveSkill(skill: Skill) {
        val json = JSONObject().apply {
            put("id", skill.id)
            put("name", skill.name)
            put("version", skill.version)
            put("author", skill.author)
            put("triggerPhrases", JSONArray(skill.triggerPhrases))
            put("variables", JSONArray(skill.variables.map {
                JSONObject().put("key", it.key).put("description", it.description).put("extractFrom", it.extractFrom)
            }))
            put("llmHint", skill.llmHint)
            put("actionTemplate", JSONArray(skill.actionTemplate.map {
                JSONObject().put("action", it.action).put("package", it.packageName).put("value", it.value)
            }))
            put("successVerification", skill.successVerification)
            put("tags", JSONArray(skill.tags))
            put("usageCount", skill.usageCount)
            put("successRate", skill.successRate.toDouble())
        }
        
        File(skillsDir, "${skill.id}.json").writeText(json.toString(2))
    }
    
    fun generateSkill(command: String, actions: List<Action>, usedPackage: String?): Skill? {
        if (actions.size < 4) return null
        if (usedPackage == null) return null
        
        return Skill(
            id = "auto-${UUID.randomUUID().toString().take(8)}",
            name = command.take(30),
            version = "1.0",
            author = "generated",
            triggerPhrases = listOf(command.lowercase()),
            variables = listOf(SkillVariable("input", "User input", "trigger")),
            llmHint = "Custom action sequence",
            actionTemplate = actions,
            successVerification = "Task completed",
            tags = listOf("generated", "custom")
        )
    }
    
    fun getAllSkills(): List<Skill> = skillsCache.toList()
    
    fun getSkillCount(): Int = skillsCache.size
    
    data class SkillVariable(
        val key: String,
        val description: String,
        val extractFrom: String
    )
}

data class Skill(
    val id: String,
    val name: String,
    val version: String,
    val author: String,
    val triggerPhrases: List<String>,
    val variables: List<SkillEngine.SkillVariable>,
    val llmHint: String,
    val actionTemplate: List<Action>,
    val successVerification: String,
    val tags: List<String>,
    val usageCount: Int,
    val successRate: Float
)