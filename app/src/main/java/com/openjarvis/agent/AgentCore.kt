package com.openjarvis.agent

import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.util.Log
import com.openjarvis.accessibility.JarvisAccessibilityService
import com.openjarvis.accessibility.ScreenReader
import com.openjarvis.graphify.AnalysisEngine
import com.openjarvis.graphify.GraphifyRepository
import com.openjarvis.intelligence.AIAppInteractor
import com.openjarvis.intelligence.AIApps
import com.openjarvis.intelligence.AppAnalyzer
import com.openjarvis.intelligence.TaskRouter
import com.openjarvis.intelligence.TaskWorkingMemory
import com.openjarvis.llm.UniversalAdapter
import com.openjarvis.vision.VisionModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class AgentCore(private val context: Context) {

    private val graphifyRepo = GraphifyRepository(context)
    private val analysisEngine = AnalysisEngine(context)
    private val universalAdapter = UniversalAdapter(context)
    private val screenReader = ScreenReader(context)
    private val visionModule = VisionModule.getInstance(context)
    private val taskRouter = TaskRouter(context)
    private val appAnalyzer = AppAnalyzer(context)
    private val aiAppInteractor = AIAppInteractor(context)

    private var workingMemory = TaskWorkingMemory()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val taskMutex = Mutex()

    private val _state = MutableStateFlow<AgentState>(AgentState.Idle)
    val state: StateFlow<AgentState> = _state

    private val systemPrompt = """
You are Open Jarvis — an Android device control AI agent.
The user gives you a cleanCommand in natural language. The Euronics Service Report is embedded inside this app in a WebView. If the current screen contains Euronics Service Report, WORK COMPLETION REPORT, Petrol Sheet, or service-report fields, operate the visible embedded report directly with tap/type actions. Do not try to open a separate Euronics app. Preserve the user data; never clear app storage or reset the report.
You must respond with ONLY a valid JSON array of actions. No explanation. No markdown fences. No preamble. Pure JSON array only.

AVAILABLE ACTIONS:
open_app     → {"action":"open_app","package":"com.package","label":"AppName"}
tap          → {"action":"tap","text":"Button text on screen"}
type         → {"action":"type","value":"text to type"}
press_back   → {"action":"press_back"}
press_home   → {"action":"press_home"}
press_recents → {"action":"press_recents"}
wait_for     → {"action":"wait_for","text":"expected text","timeout_ms":3000}
read_screen  → {"action":"read_screen"}
extract_text → {"action":"extract_text","outputKey":"page_text"}

CURRENT SCREEN CONTENT: {SCREEN_OCR}

APP SELECTION REASONING: {APP_REASONING}

INSTALLED AI APPS: {AI_APPS}

RECENT MEMORY CONTEXT: {GRAPHIFY_CONTEXT}

RULES:
- Understand Hindi, Marathi, and English commands.
- For open_app, package name is sufficient; label is optional.
- Never emit actions unsupported by the executor.
- Always start complex tasks with open_app
- Add wait_for after open_app to confirm app loaded
- If screen content is empty or unclear, add read_screen as first action
- Never assume UI state — always verify with wait_for
- Keep action arrays short: 2-8 steps per task
- Use only the local Ollama model for all AI reasoning; never delegate to external AI apps
- If a task is impossible to do safely, return: [{"action":"error","message":"reason"}]
""".trimIndent()

    fun executeTask(cleanCommand: String) {
        workingMemory = TaskWorkingMemory()

        scope.launch {
            taskMutex.withLock {
                try {
                    val normalizedCommand = CommandDictionary.normalize(cleanCommand)
                    val sanitized = PromptSanitizer.sanitize(normalizedCommand)

                    when (sanitized) {
                        is PromptSanitizer.SanitizeResult.Rejected -> {
                            _state.value = AgentState.Error(sanitized.reason)
                            return@withLock
                        }

                        is PromptSanitizer.SanitizeResult.Suspicious -> {
                            _state.value = AgentState.Running("analyzing...")
                        }

                        is PromptSanitizer.SanitizeResult.Clean -> {
                        }
                    }

                    val safeCommand = when (sanitized) {
                        is PromptSanitizer.SanitizeResult.Suspicious ->
                            sanitized.sanitized

                        is PromptSanitizer.SanitizeResult.Clean ->
                            sanitized.text

                        else -> cleanCommand
                    }

                    _state.value = AgentState.Running("analyzing task...")

                    val plan = taskRouter.analyze(safeCommand)

                    _state.value = AgentState.Running("reading screen...")

                    val screenText = withContext(Dispatchers.IO) {
                        screenReader.extractAllText()
                    }

                    _state.value = AgentState.Running("getting context...")

                    val memoryContext =
                        graphifyRepo.buildMemoryContext(safeCommand)

                    val fullSystem = systemPrompt
                        .replace("{SCREEN_OCR}", screenText.take(2000))
                        .replace("{APP_REASONING}", plan.reasoning)
                        .replace("{AI_APPS}", getInstalledAIApps())
                        .replace(
                            "{GRAPHIFY_CONTEXT}",
                            if (memoryContext.isBlank()) {
                                "No recent tasks"
                            } else {
                                memoryContext
                            }
                        )

                    _state.value = AgentState.Running("thinking...")

                    val startTime = System.currentTimeMillis()

                    val result = universalAdapter.complete(
                        fullSystem,
                        safeCommand
                    )

                    result.fold(
                        onSuccess = { rawJson ->
                            var validation =
                                LLMResponseValidator.validate(rawJson)
                            var responseForDiagnostics = rawJson

                            // One bounded repair pass for malformed/empty model output. No device
                            // action is executed until a complete plan passes validation.
                            if (!validation.isValid) {
                                val firstReason = validation.errors.firstOrNull() ?: "Empty action plan"
                                _state.value = AgentState.Running("repairing AI action plan...")
                                val repairSystem = """
You repair Open Jarvis Android action plans.
Return ONLY a non-empty JSON array of action objects. Every item must be an object with an "action" string.
Allowed actions: open_app, tap, type, press_back, press_home, press_recents, wait_for, read_screen, extract_text, error.
Fields: package, label, text, value, timeout_ms, message, outputKey.
Do not add explanations, markdown, strings, nulls, or unsupported actions.
Preserve the user's original intent. Never invent a destructive or sensitive action.
""".trimIndent()
                                val repairResult = universalAdapter.complete(
                                    repairSystem,
                                    "Original command: $safeCommand\\nValidation error: $firstReason\\nInvalid response: ${rawJson.take(3500)}"
                                )
                                val repairedResponse = repairResult.getOrNull()
                                if (!repairedResponse.isNullOrBlank()) {
                                    responseForDiagnostics = repairedResponse
                                    validation = LLMResponseValidator.validate(repairedResponse)
                                }
                            }

                            val latency = System.currentTimeMillis() - startTime
                            if (!validation.isValid) {
                                val reason = validation.errors.firstOrNull() ?: "Empty action plan"
                                val diagnosis = ErrorDiagnosis.invalidPlan(reason)
                                val detail = "raw=${responseForDiagnostics.take(240).replace('\\n', ' ')}"
                                _state.value = AgentState.Error(diagnosis.display(detail))
                                graphifyRepo.logTask(safeCommand, "failed: ${diagnosis.display(detail)}", universalAdapter.getProviderName(), latency)
                                return@fold
                            }

                            val actions = validation.actions

                            _state.value = AgentState.Running(
                                "executing ${actions.size} actions..."
                            )

                            executeActions(actions)
                            val executionState = _state.value
                            if (executionState is AgentState.Error) {
                                val diagnosis = ErrorDiagnosis.fromThrowable(
                                    IllegalStateException(executionState.message),
                                    "Action execution"
                                )
                                _state.value = AgentState.Error(diagnosis.display(executionState.message))
                                graphifyRepo.logTask(safeCommand, "failed: ${diagnosis.display(executionState.message)}", universalAdapter.getProviderName(), latency)
                                return@fold
                            }

                            graphifyRepo.logTask(
                                command = safeCommand,
                                result = "success",
                                provider = universalAdapter.getProviderName(),
                                latencyMs = latency
                            )

                            analysisEngine.analyzeLastTask()

                            _state.value =
                                AgentState.Done("done in ${latency}ms")
                        },

                        onFailure = { error ->
                            Log.e("JarvisAgent", "Ollama/agent request failed", error)
                            val detail = "${error.javaClass.simpleName}: ${error.message ?: error.cause?.message ?: "no error message"}"
                            val diagnosis = ErrorDiagnosis.fromThrowable(error, "Ollama request")
                            val msg = diagnosis.display(detail)
                            _state.value = AgentState.Error(msg)

                            graphifyRepo.logTask(
                                safeCommand,
                                "failed: $msg",
                                "",
                                0
                            )
                        }
                    )
                } catch (e: Exception) {
                    Log.e("JarvisAgent", "Unhandled agent exception", e)
                    val detail = "${e.javaClass.simpleName}: ${e.message ?: e.cause?.message ?: "no error message"}"
                    _state.value = AgentState.Error("Agent failure [$detail]")

                    graphifyRepo.logTask(
                        cleanCommand,
                        "failed: $detail",
                        "",
                        0
                    )
                }
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

    private fun getInstalledAIApps(): String {
        return "External AI apps disabled — use local Ollama only"
    }

    suspend fun getAnalyzedAppCount(): Int =
        appAnalyzer.getAnalyzedCount()

    suspend fun getAIAppCount(): Int =
        appAnalyzer.getAICount()

    private suspend fun executeActions(actions: List<Action>) {
        for ((index, action) in actions.withIndex()) {
            _state.value = AgentState.Running(
                "action ${index + 1}/${actions.size}"
            )

            when (action.action) {
                Action.OPEN_APP -> {
                    val label = action.label

                    val packageName = action.packageName ?: label?.let { findPackageByLabel(it) }
                    if (packageName.isNullOrBlank()) {
                        _state.value = AgentState.Error("App not found: ${label ?: "missing package name"}")
                        return
                    }
                    val service = JarvisAccessibilityService.instance
                    if (service == null || !service.openAppByPackage(packageName)) {
                        _state.value = AgentState.Error("Could not open app: ${label ?: packageName}")
                        return
                    }
                    graphifyRepo.logAppOpened(packageName, label ?: packageName)
                }

                Action.TAP -> {
                    val target = action.text
                    val service = JarvisAccessibilityService.instance
                    if (target.isNullOrBlank() || service == null || !service.tapByText(target)) {
                        _state.value = AgentState.Error("Could not tap screen text: ${target ?: "missing target"}")
                        return
                    }
                }

                Action.TYPE -> {
                    val value = action.value
                    val service = JarvisAccessibilityService.instance
                    if (value.isNullOrEmpty() || service == null || !service.typeText(value)) {
                        _state.value = AgentState.Error("Could not type: input field not found")
                        return
                    }
                }

                Action.PRESS_BACK -> {
                    if (JarvisAccessibilityService.instance?.pressBack() != true) {
                        _state.value = AgentState.Error("Back action failed")
                        return
                    }
                }

                Action.PRESS_HOME -> {
                    if (JarvisAccessibilityService.instance?.pressHome() != true) {
                        _state.value = AgentState.Error("Home action failed")
                        return
                    }
                }

                Action.PRESS_RECENTS -> {
                    if (JarvisAccessibilityService.instance?.pressRecents() != true) {
                        _state.value = AgentState.Error("Recent apps action failed")
                        return
                    }
                }

                Action.WAIT_FOR -> {
                    val expected = action.text
                    val timeout = action.timeoutMs.coerceIn(500L, 15000L)
                    val started = System.currentTimeMillis()
                    var found = false
                    while (expected != null && System.currentTimeMillis() - started < timeout) {
                        if (screenReader.extractAllText().contains(expected, ignoreCase = true)) {
                            found = true
                            break
                        }
                        kotlinx.coroutines.delay(300)
                    }
                    if (!found) {
                        _state.value = AgentState.Error("Timed out waiting for: ${expected ?: "screen text"}")
                        return
                    }
                }

                Action.READ_SCREEN -> {
                    val screen = screenReader.extractAllText()
                    if (screen.isBlank()) {
                        _state.value = AgentState.Error("Could not read current screen")
                        return
                    }
                    workingMemory.set(action.outputKey ?: "page_text", screen)
                }

                Action.SCREENSHOT, Action.TAP_COORDS, Action.LONG_PRESS,
                Action.SWIPE, Action.SCROLL, Action.CLEAR_TYPE -> {
                    _state.value = AgentState.Error("Action not supported yet: ${action.action}")
                    return
                }

                Action.AI_PROMPT -> {
                    _state.value = AgentState.Error(
                        "External AI apps are disabled. Jarvis uses local Ollama only."
                    )
                    return
                }

                Action.EXTRACT_TEXT -> {
                    val outputKey = action.outputKey ?: "page_text"
                    val text = screenReader.extractAllText()
                    workingMemory.set(outputKey, text)
                }

                Action.ERROR -> {
                    _state.value = AgentState.Error(
                        action.message ?: "Task failed"
                    )
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

        val apps: List<ResolveInfo> =
            pm.queryIntentActivities(intent, 0)

        val normalizedLabel = label.lowercase().trim()

        for (app in apps) {
            val appLabel =
                app.loadLabel(pm).toString().lowercase()

            if (
                appLabel == normalizedLabel ||
                appLabel.contains(normalizedLabel) ||
                normalizedLabel.contains(appLabel)
            ) {
                return app.activityInfo.packageName
            }
        }

        return null
    }
}
