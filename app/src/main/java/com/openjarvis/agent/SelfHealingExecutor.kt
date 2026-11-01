package com.openjarvis.agent

import android.content.Context
import com.openjarvis.accessibility.ScreenReader
import com.openjarvis.graphify.GraphifyRepository
import com.openjarvis.llm.UniversalAdapter
import kotlinx.coroutines.delay

class SelfHealingExecutor(private val context: Context) {

    private val screenReader = ScreenReader(context)
    private val graphifyRepo = GraphifyRepository(context)
    private val llm = UniversalAdapter.getModelManager(context)

    private val maxAttempts = 3
    private val baseDelayMs = 1000L

    suspend fun executeWithHealing(
        action: Action,
        context: ExecutionContext,
        attempt: Int = 1
    ): ActionResult {

        val result = tryExecuteAction(action, context)

        if (result is ActionResult.Success) return result

        if (attempt >= maxAttempts) {
            return ActionResult.Failed(
                "Could not complete after $maxAttempts attempts"
            )
        }

        val currentScreen = screenReader.extractAllText()

        val healingPrompt = buildHealingPrompt(
            action,
            context,
            currentScreen,
            attempt
        )

        val alternativeResponse: String? = try {
            val response = llm.complete(
                healingPrompt.first,
                healingPrompt.second
            )
            response?.toString()
        } catch (e: Exception) {
            null
        }

        delay(baseDelayMs * attempt)

        if (!alternativeResponse.isNullOrBlank()) {
            val alternativeAction =
                parseActionFromLLM(alternativeResponse)

            if (alternativeAction != null) {
                return executeWithHealing(
                    alternativeAction,
                    context,
                    attempt + 1
                )
            }
        }

        return executeWithHealing(
            action,
            context,
            attempt + 1
        )
    }

    private fun tryExecuteAction(
        action: Action,
        context: ExecutionContext
    ): ActionResult {

        return try {
            val service =
                com.openjarvis.accessibility.JarvisAccessibilityService.instance

            when (action.action) {

                Action.TAP -> {
                    val tapped =
                        service?.tapByText(action.text ?: "") == true

                    if (tapped) {
                        ActionResult.Success("tapped ${action.text}")
                    } else {
                        ActionResult.Failed("tap failed")
                    }
                }

                Action.TYPE -> {
                    val typed =
                        service?.typeText(action.value ?: "") == true

                    if (typed) {
                        ActionResult.Success("typed ${action.value}")
                    } else {
                        ActionResult.Failed("type failed")
                    }
                }

                Action.OPEN_APP -> {
                    val opened =
                        service?.openAppByPackage(
                            action.packageName ?: ""
                        )

                    if (opened == true) {
                        ActionResult.Success("opened ${action.packageName}")
                    } else {
                        ActionResult.Failed("app open failed")
                    }
                }

                Action.PRESS_BACK -> {
                    if (service != null) {
                        service.pressBack()
                        ActionResult.Success("pressed back")
                    } else {
                        ActionResult.Failed("accessibility service unavailable")
                    }
                }

                else -> {
                    ActionResult.Success(
                        "action ${action.action} executed"
                    )
                }
            }

        } catch (e: Exception) {
            ActionResult.Failed(
                e.message ?: "Unknown error"
            )
        }
    }

    private fun buildHealingPrompt(
        action: Action,
        context: ExecutionContext,
        currentScreen: String,
        attempt: Int
    ): Pair<String, String> {

        val system = """
Action failed: ${action.description ?: action.action}
Expected to see: ${context.expectedState ?: "task completion"}
Current screen shows: ${currentScreen.take(500)}
Attempt: $attempt/$maxAttempts

What went wrong and what alternative action should I try?
Respond with a single alternative Action JSON.
        """.trimIndent()

        return system to """
The action failed. Current screen: "$currentScreen"
Give me one alternative action that might work.
        """.trimIndent()
    }

    private fun parseActionFromLLM(
        llmResponse: String
    ): Action? {

        return try {
            val json = org.json.JSONObject(llmResponse.trim())

            Action(
                action = json.getString("action"),
                text = json.optString("text", null),
                value = json.optString("value", null),
                packageName = json.optString("package", null)
            )

        } catch (e: Exception) {
            null
        }
    }

    data class ExecutionContext(
        val expectedState: String?,
        val phaseId: String,
        val previousPhaseOutcome: String?,
        val screenBefore: String
    )

    sealed class ActionResult {
        data class Success(val message: String) : ActionResult()
        data class Failed(val reason: String) : ActionResult()
    }
}
