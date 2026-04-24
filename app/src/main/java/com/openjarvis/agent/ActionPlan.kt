package com.openjarvis.agent

import org.json.JSONArray
import org.json.JSONObject

data class Action(
    val action: String,
    val packageName: String? = null,
    val label: String? = null,
    val text: String? = null,
    val value: String? = null,
    val hint: String? = null,
    val direction: String? = null,
    val x: Int? = null,
    val y: Int? = null,
    val distance: String? = null,
    val timeoutMs: Long = 3000L,
    val message: String? = null
) {
    companion object {
        const val OPEN_APP = "open_app"
        const val TAP = "tap"
        const val TAP_COORDS = "tap_coords"
        const val TYPE = "type"
        const val CLEAR_TYPE = "clear_type"
        const val LONG_PRESS = "long_press"
        const val SWIPE = "swipe"
        const val SCROLL = "scroll"
        const val PRESS_BACK = "press_back"
        const val PRESS_HOME = "press_home"
        const val PRESS_RECENTS = "press_recents"
        const val WAIT_FOR = "wait_for"
        const val SCREENSHOT = "screenshot"
        const val READ_SCREEN = "read_screen"
        const val ERROR = "error"
    }
}

sealed class AgentState {
    object Idle : AgentState()
    data class Running(val step: String) : AgentState()
    data class Done(val result: String) : AgentState()
    data class Error(val message: String) : AgentState()
}

object ActionJsonParser {
    fun parse(json: String): List<Action>? {
        return try {
            val clean = json
                .trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
            
            val jsonArray = JSONArray(clean)
            (0 until jsonArray.length()).map { i ->
                val obj = jsonArray.getJSONObject(i)
                Action(
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
                    message = obj.optString("message", null).takeIf { it.isNotBlank() }
                )
            }
        } catch (e: Exception) {
            null
        }
    }
}