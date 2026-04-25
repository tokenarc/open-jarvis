package com.openjarvis.watch

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.os.BatteryManager
import android.os.PowerManager
import com.openjarvis.accessibility.JarvisAccessibilityService
import com.openjarvis.accessibility.ScreenReader
import com.openjarvis.graphify.GraphifyRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ScreenWatcher(private val context: Context, private val agentCore: com.openjarvis.agent.AgentCore) {
    
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var watcherJob: Job? = null
    private var isActive = false
    
    private val _state = MutableStateFlow<WatcherState>(WatcherState.Inactive)
    val state: StateFlow<WatcherState> = _state
    
    private val rules = mutableListOf<WatchRule>()
    private val screenReader = ScreenReader(context)
    private val graphifyRepo = GraphifyRepository(context)
    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    
    private val _rulesFlow = MutableStateFlow<List<WatchRule>>(emptyList())
    val rulesFlow: StateFlow<List<WatchRule>> = _rulesFlow
    
    fun start() {
        if (isActive) return
        isActive = true
        
        loadBuiltInRules()
        
        watcherJob = scope.launch {
            _state.value = WatcherState.Active
            
            while (isActive && isActive) {
                if (!isScreenOn()) {
                    delay(2000)
                    continue
                }
                
                pollAndEvaluate()
                delay(POLL_INTERVAL_MS)
            }
        }
    }
    
    fun stop() {
        isActive = false
        watcherJob?.cancel()
        watcherJob = null
        _state.value = WatcherState.Inactive
    }
    
    private suspend fun pollAndEvaluate() {
        try {
            val screenText = getCurrentScreenText()
            val packageName = getCurrentPackage()
            val battery = getBatteryLevel()
            val now = System.currentTimeMillis()
            
            for (rule in rules) {
                if (!rule.enabled) continue
                if (now - rule.lastTriggered < rule.cooldownMs) continue
                
                val triggered = when (val trigger = rule.trigger) {
                    is WatchTrigger.TextAppears -> {
                        if (trigger.packageFilter != null && packageName != trigger.packageFilter) false
                        else screenText.contains(trigger.text, ignoreCase = true)
                    }
                    is WatchTrigger.TextContains -> screenText.contains(trigger.keyword, ignoreCase = true)
                    is WatchTrigger.BatteryBelow -> battery < trigger.percent
                    is WatchTrigger.AppOpened -> packageName == trigger.packageName
                    is WatchTrigger.TimeReached -> {
                        val cal = java.util.Calendar.getInstance()
                        cal.get(java.util.Calendar.HOUR_OF_DAY) == trigger.hour &&
                        cal.get(java.util.Calendar.MINUTE) == trigger.minute
                    }
                    else -> false
                }
                
                if (triggered) {
                    executeAction(rule.action, screenText)
                    rule.lastTriggered = now
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private suspend fun executeAction(action: WatchAction, screenText: String) {
        when (val watchAction = action) {
            is WatchAction.TapElement -> {
                JarvisAccessibilityService.instance?.tapByText(watchAction.text)
            }
            is WatchAction.RunCommand -> {
                agentCore.executeTask(watchAction.command)
            }
            is WatchAction.SpeakText -> {
                com.openjarvis.voice.VoiceManager(context).speak(watchAction.template)
            }
            is WatchAction.ShowNotification -> {
                showNotification(watchAction.title, watchAction.body)
            }
        }
        
        graphifyRepo.logTask("watch: ${action.javaClass.simpleName}", "triggered", "watch", 0)
    }
    
    private fun getCurrentScreenText(): String {
        return try {
            screenReader.extractAllText()
        } catch (e: Exception) {
            ""
        }
    }
    
    private fun getCurrentPackage(): String {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            val runningTasks = context.getSystemService(Context.ACTIVITY_SERVICE)
                .javaClass.getMethod("getRunningTasks", Int::class.javaPrimitiveType)
            "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }
    
    private fun getBatteryLevel(): Int {
        return try {
            batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        } catch (e: Exception) {
            100
        }
    }
    
    private fun isScreenOn(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isScreenOn
    }
    
    private fun showNotification(title: String, body: String) {
        val notification = android.app.Notification.Builder(context, "watch_channel")
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(com.openjarvis.R.drawable.ic_notification)
            .setAutoCancel(true)
            .build()
        
        val manager = context.getSystemService(android.app.NotificationManager::class.java)
        manager.notify(WATCH_NOTIFICATION_ID, notification)
    }
    
    private fun loadBuiltInRules() {
        rules.clear()
        
        rules.add(WatchRule(
            id = "skip-youtube-ads",
            name = "Auto-skip YouTube ads",
            enabled = true,
            trigger = WatchTrigger.TextAppears("Skip Ad", "com.google.android.youtube"),
            action = WatchAction.TapElement("Skip Ad"),
            cooldownMs = 10_000
        ))
        
        rules.add(WatchRule(
            id = "low-battery-warning",
            name = "Low battery helper",
            enabled = true,
            trigger = WatchTrigger.BatteryBelow(15),
            action = WatchAction.ShowNotification("Low Battery", "Battery below 15%"),
            cooldownMs = 3_600_000
        ))
        
        _rulesFlow.value = rules.toList()
    }
    
    fun addRule(rule: WatchRule) {
        rules.add(rule)
        _rulesFlow.value = rules.toList()
    }
    
    fun removeRule(id: String) {
        rules.removeAll { it.id == id }
        _rulesFlow.value = rules.toList()
    }
    
    fun toggleRule(id: String, enabled: Boolean) {
        val index = rules.indexOfFirst { it.id == id }
        if (index >= 0) {
            rules[index] = rules[index].copy(enabled = enabled)
            _rulesFlow.value = rules.toList()
        }
    }
    
    fun getActiveRuleCount(): Int = rules.count { it.enabled }
    
    companion object {
        const val POLL_INTERVAL_MS = 2000L
        const val WATCH_NOTIFICATION_ID = 2001
    }
    
    sealed class WatcherState {
        object Inactive : WatcherState()
        object Active : WatcherState()
        object Paused : WatcherState()
    }
}

data class WatchRule(
    val id: String,
    val name: String,
    val enabled: Boolean,
    val trigger: WatchTrigger,
    val action: WatchAction,
    val cooldownMs: Long = 30_000,
    val lastTriggered: Long = 0
)

sealed class WatchTrigger {
    data class TextAppears(val text: String, val packageFilter: String? = null) : WatchTrigger()
    data class TextContains(val keyword: String) : WatchTrigger()
    data class BatteryBelow(val percent: Int) : WatchTrigger()
    data class AppOpened(val packageName: String) : WatchTrigger()
    data class TimeReached(val hour: Int, val minute: Int) : WatchTrigger()
}

sealed class WatchAction {
    data class TapElement(val text: String) : WatchAction()
    data class RunCommand(val command: String) : WatchAction()
    data class SpeakText(val template: String) : WatchAction()
    data class ShowNotification(val title: String, val body: String) : WatchAction()
}