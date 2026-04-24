package com.openjarvis.accessibility

import com.openjarvis.agent.Action
import com.openjarvis.agent.ActionPlan
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.concurrent.CopyOnWriteArrayList

class ActionExecutor(private val service: JarvisAccessibilityService) {

    private val screenReader = ScreenReader(service)

    suspend fun execute(actions: List<Action>): ExecutionResult = withContext(Dispatchers.IO) {
        val results = CopyOnWriteArrayList<ActionResult>()
        
        for (action in actions) {
            val stepResult = executeStep(action)
            results.add(stepResult)
            
            if (!stepResult.success) {
                return@withContext ExecutionResult(
                    partialResults = results.toList(),
                    success = false,
                    errorMessage = stepResult.errorMessage
                )
            }
            
            delay(500)
        }
        
        ExecutionResult(
            partialResults = results.toList(),
            success = true,
            errorMessage = null
        )
    }

    private suspend fun executeStep(action: Action): ActionResult {
        return when (action.action) {
            Action.OPEN_APP -> {
                val packageName = action.packageName
                val label = action.label
                
                if (packageName != null) {
                    service.openAppByPackage(packageName)
                    ActionResult(action, true, null)
                } else if (label != null) {
                    val success = service.openAppByLabel(label)
                    ActionResult(action, success, if (!success) "App not found: $label" else null)
                } else {
                    ActionResult(action, false, "No package or label provided")
                }
            }
            
            Action.TAP -> {
                val text = action.text
                if (text != null) {
                    val success = service.tapByText(text)
                    ActionResult(action, success, if (!success) "Could not tap: $text" else null)
                } else {
                    ActionResult(action, false, "No text provided for tap")
                }
            }
            
            Action.TYPE -> {
                val value = action.value
                if (value != null) {
                    val success = service.typeText(value)
                    ActionResult(action, success, if (!success) "Could not type: $value" else null)
                } else {
                    ActionResult(action, false, "No value provided for type")
                }
            }
            
            Action.PRESS_BACK -> {
                service.pressBack()
                ActionResult(action, true, null)
            }
            
            Action.PRESS_HOME -> {
                service.pressHome()
                ActionResult(action, true, null)
            }
            
            Action.PRESS_RECENTS -> {
                service.pressRecents()
                ActionResult(action, true, null)
            }
            
            else -> {
                ActionResult(action, false, "Unsupported action: ${action.action}")
            }
        }
    }

    data class ActionResult(
        val action: Action,
        val success: Boolean,
        val errorMessage: String?
    )

    data class ExecutionResult(
        val partialResults: List<ActionResult>,
        val success: Boolean,
        val errorMessage: String?
    )
}

private object Dispatchers {
    val IO = kotlinx.coroutines.Dispatchers.IO
}