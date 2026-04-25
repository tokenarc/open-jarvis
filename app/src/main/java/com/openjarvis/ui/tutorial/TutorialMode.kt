package com.openjarvis.ui.tutorial

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import com.openjarvis.accessibility.JarvisAccessibilityService
import com.openjarvis.agent.Action
import com.openjarvis.voice.VoiceManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TutorialMode(private val context: Context) {
    
    private val voiceManager = VoiceManager(context)
    private val modeFlow = MutableStateFlow(TutorialState.OFF)
    private val currentStepFlow = MutableStateFlow<TutorialStep?>(null)
    
    private var overlayWindow: WindowManager? = null
    private var highlightView: TutorialHighlightView? = null
    private val handler = Handler(Looper.getMainLooper())
    
    private val stepHistory = mutableListOf<String>()
    
    sealed class TutorialState {
        object Off : TutorialState()
        object Guided : TutorialState()
        object Observe : TutorialState()
        object Shadow : TutorialState()
    }
    
    data class TutorialStep(
        val stepNumber: Int,
        val description: String,
        val targetBounds: RectF?,
        val narration: String
    )
    
    val state: StateFlow<TutorialState> = modeFlow
    val currentStep: StateFlow<TutorialStep?> = currentStepFlow
    
    suspend fun startGuidedMode() {
        modeFlow.value = TutorialState.Guided
    }
    
    suspend fun executeStep(action: Action, bounds: RectF?) {
        if (modeFlow.value != TutorialState.Guided) return
        
        val stepNum = stepHistory.size + 1
        val narration = generateNarration(action)
        
        val step = TutorialStep(stepNum, action.description ?: action.action, bounds, narration)
        currentStepFlow.value = step
        
        showHighlight(bounds, narration)
        
        speak(narration)
        
        delay(1000)
        
        executeAction(action)
        
        stepHistory.add("${stepNum}. ${action.action}")
        
        currentStepFlow.value = null
        hideHighlight()
    }
    
    private fun generateNarration(action: Action): String {
        return when (action.action) {
            Action.OPEN_APP -> "Opening ${action.label ?: action.packageName}"
            Action.TAP -> "Tapping ${action.text ?: "the button"}"
            Action.TYPE -> "Typing ${action.value}"
            Action.SCROLL -> "Scrolling ${action.direction ?: "down"}"
            Action.PRESS_BACK -> "Pressing back"
            Action.PRESS_HOME -> "Going to home screen"
            else -> "Performing ${action.action}"
        }
    }
    
    private fun speak(text: String) {
        voiceManager.speak(text)
    }
    
    private fun showHighlight(bounds: RectF?, text: String) {
        if (bounds == null) return
        
        try {
            highlightView = TutorialHighlightView(context).apply {
                setTarget(bounds, text)
            }
            
            overlayWindow = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            overlayWindow?.addView(highlightView, WindowManager.LayoutParams())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun hideHighlight() {
        try {
            highlightView?.let { view ->
                overlayWindow?.removeView(view)
            }
        } catch (e: Exception) { }
        highlightView = null
    }
    
    private fun executeAction(action: Action) {
        val service = JarvisAccessibilityService.instance
        
        when (action.action) {
            Action.OPEN_APP -> action.packageName?.let { service?.openAppByPackage(it) }
            Action.TAP -> action.text?.let { service?.tapByText(it) }
            Action.TYPE -> action.value?.let { service?.typeText(it) }
            Action.PRESS_BACK -> service?.pressBack()
            Action.PRESS_HOME -> service?.pressHome()
            else -> { }
        }
    }
    
    fun getSummary(): String {
        return stepHistory.mapIndexed { i, step -> "${i + 1}. $step" }
            .joinToString("\n")
    }
    
    fun endSession() {
        hideHighlight()
        modeFlow.value = TutorialState.Off
        currentStepFlow.value = null
    }
    
    fun isActive(): Boolean = modeFlow.value != TutorialState.Off
    
    private class TutorialHighlightView(context: Context) : View(context) {
        private var targetBounds: RectF? = null
        private var targetText: String = ""
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF9B59B6.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 8f
        }
        
        fun setTarget(bounds: RectF, text: String) {
            targetBounds = bounds
            targetText = text
            invalidate()
        }
        
        override fun onDraw(canvas: Canvas) {
            targetBounds?.let { bounds ->
                val animatedAlpha = (0.4f + 0.6f * (System.currentTimeMillis() % 1000) / 1000f).toFloat()
                paint.alpha = (animatedAlpha * 255).toInt()
                canvas.drawRoundRect(bounds, 16f, 16f, paint)
            }
        }
    }
}