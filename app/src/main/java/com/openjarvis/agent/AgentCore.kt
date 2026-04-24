package com.openjarvis.agent

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import com.openjarvis.accessibility.JarvisAccessibilityService
import com.openjarvis.graphify.GraphifyRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AgentCore(private val context: Context) {

    private val graphifyRepo = GraphifyRepository(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _state = MutableStateFlow<AgentState>(AgentState.Idle)
    val state: StateFlow<AgentState> = _state

    fun parseCommand(input: String): CommandResult {
        val normalized = input.lowercase().trim()
        
        when {
            normalized.startsWith("open ") -> {
                val appName = input.substring(5).trim()
                return CommandResult(CommandType.OPEN_APP, appName, appName)
            }
            normalized.startsWith("launch ") -> {
                val appName = input.substring(6).trim()
                return CommandResult(CommandType.OPEN_APP, appName, appName)
            }
            normalized == "back" || normalized == "go back" -> {
                return CommandResult(CommandType.BACK, null, null)
            }
            normalized == "home" -> {
                return CommandResult(CommandType.HOME, null, null)
            }
            normalized == "recents" || normalized == "recent apps" -> {
                return CommandResult(CommandType.RECENTS, null, null)
            }
            else -> {
                return CommandResult(CommandType.OPEN_APP, input, input)
            }
        }
    }

    fun buildActionPlan(command: String): List<Action> {
        val parsed = parseCommand(command)
        
        return when (parsed.type) {
            CommandType.OPEN_APP -> {
                listOf(Action(Action.OPEN_APP, label = parsed.label))
            }
            CommandType.BACK -> {
                listOf(Action(Action.PRESS_BACK))
            }
            CommandType.HOME -> {
                listOf(Action(Action.PRESS_HOME))
            }
            CommandType.RECENTS -> {
                listOf(Action(Action.PRESS_RECENTS))
            }
            CommandType.UNKNOWN -> {
                listOf(Action(Action.OPEN_APP, label = command))
            }
        }
    }

    fun executeTask(command: String) {
        scope.launch {
            try {
                _state.value = AgentState.Running("Parsing command...")
                
                val plan = buildActionPlan(command)
                if (plan.isEmpty()) {
                    _state.value = AgentState.Error("Could not understand command: $command")
                    return@launch
                }

                _state.value = AgentState.Running("Executing actions...")
                
                val firstAction = plan.first()
                when (firstAction.action) {
                    Action.OPEN_APP -> {
                        val label = firstAction.label
                        if (label != null) {
                            val packageName = findPackageByLabel(label)
                            if (packageName != null) {
                                JarvisAccessibilityService.instance?.openAppByPackage(packageName)
                                graphifyRepo.logAppOpened(packageName, label)
                                _state.value = AgentState.Done("Opened $label")
                            } else {
                                _state.value = AgentState.Error("App not found: $label")
                            }
                        }
                    }
                    Action.PRESS_BACK -> {
                        JarvisAccessibilityService.instance?.pressBack()
                        _state.value = AgentState.Done("Pressed back")
                    }
                    Action.PRESS_HOME -> {
                        JarvisAccessibilityService.instance?.pressHome()
                        _state.value = AgentState.Done("Pressed home")
                    }
                    Action.PRESS_RECENTS -> {
                        JarvisAccessibilityService.instance?.pressRecents()
                        _state.value = AgentState.Done("Opened recents")
                    }
                }
            } catch (e: Exception) {
                _state.value = AgentState.Error(e.message ?: "Unknown error")
            }
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

    enum class CommandType {
        OPEN_APP, BACK, HOME, RECENTS, UNKNOWN
    }

    data class CommandResult(
        val type: CommandType,
        val extractedValue: String?,
        val label: String?
    )
}