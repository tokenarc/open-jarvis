package com.openjarvis.agent

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

object LLMResponseValidator {
    
    private val knownActions = setOf(
        "open_app", "tap", "type", "press_back", "press_home",
        "press_recents", "wait_for", "read_screen", "ai_prompt",
        "extract_text", "error"
    )
    
    fun validate(rawResponse: String): ValidationResult {
        val errors = mutableListOf<String>()
        val clean = rawResponse.trim()
            .replace(Regex("(?s)^\\x60\\x60\\x60(?:json)?\\s*"), "")
            .replace(Regex("(?s)\\s*\\x60\\x60\\x60$"), "")
            .trim()

        if (clean.isBlank()) {
            return ValidationResult(false, emptyList(), listOf("Ollama returned an empty action plan"), false)
        }

        // Accept a JSON array, a single action object, or common wrapper objects.
        val parsed = try {
            parseActionArray(clean)
        } catch (e: Exception) {
            errors.add("Invalid AI JSON: ${e.message ?: "could not parse response"}")
            return ValidationResult(false, emptyList(), errors, false)
        }

        val actions = mutableListOf<Action>()
        for (i in 0 until parsed.length()) {
            val item = parsed.opt(i)
            val obj = when (item) {
                is JSONObject -> item
                is String -> runCatching { JSONObject(item.trim()) }.getOrNull()
                else -> null
            }
            if (obj == null) {
                errors.add("Item $i is not an action object (received ${item?.javaClass?.simpleName ?: "null"}); ask Ollama for [{\"action\":\"...\"}]")
                return ValidationResult(false, emptyList(), errors, false)
            }

            val actionType = obj.optString("action", "")
            if (actionType.isBlank()) {
                errors.add("Item $i missing 'action' field")
                return ValidationResult(false, emptyList(), errors, false)
            }
            if (actionType !in knownActions) {
                errors.add("Unsupported action type: $actionType")
                return ValidationResult(false, emptyList(), errors, false)
            }
            actions.add(parseAction(obj))
        }

        if (actions.size > 50) {
            errors.add("Suspiciously long action plan (${actions.size} steps) — capped at 50")
            return ValidationResult(true, actions.take(50), errors, true)
        }
        return ValidationResult(actions.isNotEmpty(), actions, errors, false)
    }

    private fun parseActionArray(raw: String): JSONArray {
        fun fromValue(value: Any?): JSONArray? {
            return when (value) {
                is JSONArray -> value
                is JSONObject -> when {
                    value.has("action") -> JSONArray().put(value)
                    value.opt("actions") is JSONArray -> value.getJSONArray("actions")
                    value.opt("plan") is JSONArray -> value.getJSONArray("plan")
                    else -> null
                }
                is String -> runCatching { parseActionArray(value) }.getOrNull()
                else -> null
            }
        }

        val direct = runCatching {
            val value = org.json.JSONTokener(raw).nextValue()
            fromValue(value)
        }.getOrNull()
        if (direct != null) return direct

        // Recover JSON embedded in a short explanation or markdown response.
        val firstArray = raw.indexOf('[')
        val firstObject = raw.indexOf('{')
        val start = listOf(firstArray, firstObject).filter { it >= 0 }.minOrNull()
            ?: throw JSONException("No JSON array or object found")
        val opening = raw[start]
        val closing = if (opening == '[') ']' else '}'
        var depth = 0
        var inString = false
        var escaped = false
        for (i in start until raw.length) {
            val ch = raw[i]
            if (inString) {
                if (escaped) escaped = false
                else if (ch == '\\') escaped = true
                else if (ch == '"') inString = false
            } else {
                if (ch == '"') inString = true
                else if (ch == opening) depth++
                else if (ch == closing) {
                    depth--
                    if (depth == 0) {
                        val extracted = raw.substring(start, i + 1)
                        val value = org.json.JSONTokener(extracted).nextValue()
                        return fromValue(value) ?: throw JSONException("JSON does not contain action objects")
                    }
                }
            }
        }
        throw JSONException("Incomplete JSON action plan")
    }

    private fun parseAction(obj: JSONObject): Action {
        return Action(
            action = obj.getString("action"),
            packageName = obj.optString("package", null).takeIf { it.isNotBlank() },
            label = obj.optString("label", null).takeIf { it.isNotBlank() },
            text = obj.optString("text", null).takeIf { it.isNotBlank() },
            value = obj.optString("value", null).takeIf { it.isNotBlank() },
            hint = obj.optString("hint", null).takeIf { it.isNotBlank() },
            direction = obj.optString("direction", null).takeIf { it.isNotBlank() },
            x = if (obj.has("x")) obj.getInt("x") else null,
            y = if (obj.has("y")) obj.getInt("y") else null,
            distance = obj.optString("distance", null).takeIf { it.isNotBlank() },
            timeoutMs = obj.optLong("timeout_ms", 3000L),
            message = obj.optString("message", null).takeIf { it.isNotBlank() },
            prompt = obj.optString("prompt", null).takeIf { it.isNotBlank() },
            outputKey = obj.optString("outputKey", null).takeIf { it.isNotBlank() }
        )
    }
    
    data class ValidationResult(
        val isValid: Boolean,
        val actions: List<Action>,
        val errors: List<String>,
        val wasRepaired: Boolean
    )
}