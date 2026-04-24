package com.openjarvis.agent

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import com.openjarvis.accessibility.JarvisAccessibilityService
import com.openjarvis.accessibility.ScreenReader
import com.openjarvis.graphify.AnalysisEngine
import com.openjarvis.graphify.GraphifyRepository
import com.openjarvis.llm.UniversalAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AgentCore(private val context: Context) {

    private val graphifyRepo = GraphifyRepository(context)
    private val analysisEngine = AnalysisEngine(context)
    private val universalAdapter = UniversalAdapter(context)
    private val screenReader = ScreenReader(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _state = MutableStateFlow<AgentState>(AgentState.Idle)
    val state: StateFlow<AgentState> = _state

    private val systemPrompt = """
You are Open Jarvis — an Android device control AI agent.
The user gives you a command in natural language.
You must respond with ONLY a valid JSON array of actions. No explanation. No markdown fences. No preamble. Pure JSON array only.

AVAILABLE ACTIONS:
open_app     → {"action":"open_app","package":"com.package","label":"AppName"}
tap          → {"action":"tap","text":"Button text on screen"}
tap_coords   → {"action":"tap_coords","x":540,"y":960}
long_press   → {"action":"long_press","text":"Element text"}
type         → {"action":"type","value":"text to type"}
clear_type   → {"action":"clear_type","value":"clears field then types"}
swipe        → {"action":"swipe","direction":"up|down|left|right","distance":"short|medium|long"}
scroll       → {"action":"scroll","direction":"up|down"}
press_back   → {"action":"press_back"}
press_home   → {"action":"press_home"}
press_recents → {"action":"press_recents"}
wait_for     → {"action":"wait_for","text":"expected text","timeout_ms":3000}
screenshot   → {"action":"screenshot"}
read_screen  → {"action":"read_screen"}

CURRENT SCREEN CONTENT: {SCREEN_OCR}

RECENT MEMORY CONTEXT: {GRAPHIFY_CONTEXT}

RULES:
- Always start complex tasks with open_app
- Add wait_for after open_app to confirm app loaded
- If screen content is empty or unclear, add read_screen as first action
- Never assume UI state — always verify with wait_for
- Keep action arrays short: 2-8 steps per task
- If a task is impossible to do safely, return: [{"action":"error","message":"reason"}]
""".trimIndent()

    fun executeTask(command: String) {
        scope.launch {
            try {
                _state.value = AgentState.Running("reading screen...")
                
                val screenText = withContext(Dispatchers.IO) {
                    screenReader.extractAllText()
                }
                
                _state.value = AgentState.Running("getting context...")
                
                val memoryContext = graphifyRepo.buildMemoryContext(command)
                
                val fullSystem = systemPrompt
                    .replace("{SCREEN_OCR}", screenText.take(2000))
                    .replace("{GRAPHIFY_CONTEXT}", if (memoryContext.isBlank()) "No recent tasks" else memoryContext)
                
                _state.value = AgentState.Running("thinking...")
                
                val startTime = System.currentTimeMillis()
                
                val result = universalAdapter.complete(fullSystem, command)
                result.fold(
                    onSuccess = { rawJson ->
                        val latency = System.currentTimeMillis() - startTime
                        
                        val actions = ActionJsonParser.parse(rawJson)
                            ?: run {
                                val retry = universalAdapter.complete(
                                    fullSystem,
                                    "$command\n\nRespond with JSON array ONLY. No other text."
                                )
                                retry.getOrNull()?.let { ActionJsonParser.parse(it) }
                            }
                        
                        if (actions == null) {
                            _state.value = AgentState.Error("Could not parse AI response")
                            graphifyRepo.logTask(command, "failed: parse error", "", 0)
                            return@fold
                        }
                        
                        _state.value = AgentState.Running("executing ${actions.size} actions...")
                        
                        executeActions(actions)
                        
                        graphifyRepo.logTask(
                            command = command,
                            result = "success",
                            provider = universalAdapter.getProviderName(),
                            latencyMs = latency
                        )
                        
                        analysisEngine.analyzeLastTask()
                        
                        _state.value = AgentState.Done("done in ${latency}ms")
                    },
                    onFailure = { error ->
                        val msg = when {
                            error.message?.contains("401") == true -> "Invalid API key"
                            error.message?.contains("429") == true -> "Rate limited — wait a moment"
                            error.message?.contains("timeout") == true -> "Request timed out"
                            error.message?.contains("Unable to resolve") == true -> "Network error — check connection"
                            else -> error.message ?: "Unknown error"
                        }
                        _state.value = AgentState.Error(msg)
                        graphifyRepo.logTask(command, "failed: $msg", "", 0)
                    }
                )
            } catch (e: Exception) {
                _state.value = AgentState.Error(e.message ?: "Unknown error")
                graphifyRepo.logTask(command, "failed: ${e.message}", "", 0)
            }
        }
    }

    suspend fun testConnection(): Result<Long> {
        return universalAdapter.testConnection()
    }
    
    fun getCurrentProviderName(): String {
        return universalAdapter.getProviderName()
    }
    
    fun getStateFlow(): StateFlow<AgentState> = state

    private suspend fun executeActions(actions: List<Action>) {
        for ((index, action) in actions.withIndex()) {
            _state.value = AgentState.Running("action ${index + 1}/${actions.size}")
            
            when (action.action) {
                Action.OPEN_APP -> {
                    val label = action.label
                    if (label != null) {
                        val packageName = findPackageByLabel(label)
                        if (packageName != null) {
                            JarvisAccessibilityService.instance?.openAppByPackage(packageName)
                            graphifyRepo.logAppOpened(packageName, label)
                        } else {
                            _state.value = AgentState.Error("App not found: $label")
                            return
                        }
                    }
                }
                Action.TAP -> {
                    action.text?.let { text ->
                        JarvisAccessibilityService.instance?.tapByText(text)
                    }
                }
                Action.TYPE -> {
                    action.value?.let { value ->
                        JarvisAccessibilityService.instance?.typeText(value)
                    }
                }
                Action.PRESS_BACK -> {
                    JarvisAccessibilityService.instance?.pressBack()
                }
                Action.PRESS_HOME -> {
                    JarvisAccessibilityService.instance?.pressHome()
                }
                Action.PRESS_RECENTS -> {
                    JarvisAccessibilityService.instance?.pressRecents()
                }
                Action.ERROR -> {
                    _state.value = AgentState.Error(action.message ?: "Task failed")
                    return
                }
            }
            
            kotlinx.coroutines.delay(500)
        }
    }

    private fun findPackageByLabel(label: String): String? {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        
        val apps: List<ResolveInfo> = pm.queryIntentActivities(intent, 0)
        
        val normalizedLabel = label.lowercase().trim()
        
        for (app in apps) {
            val appLabel = app.loadLabel(pm).toString().lowercase()
            if (appLabel == normalizedLabel || appLabel.contains(normalizedLabel) || normalizedLabel.contains(appLabel)) {
                return app.activityInfo.packageName
            }
        }
        
        return null
    }
}