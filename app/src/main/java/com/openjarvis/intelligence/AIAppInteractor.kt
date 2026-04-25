package com.openjarvis.intelligence

import android.content.Context
import com.openjarvis.accessibility.JarvisAccessibilityService
import com.openjarvis.accessibility.ScreenReader
import kotlinx.coroutines.delay

class AIAppInteractor(private val context: Context) {
    
    private val screenReader = ScreenReader(context)
    private val workingMemory = TaskWorkingMemory()
    
    fun openAIApp(meta: AIAppMeta): Boolean {
        return JarvisAccessibilityService.instance?.openAppByPackage(meta.packageName) ?: false
    }
    
    fun clearContext() {
        try {
            JarvisAccessibilityService.instance?.pressBack()
            delay(300)
        } catch (e: Exception) { }
    }
    
    fun typePrompt(prompt: String): Boolean {
        return JarvisAccessibilityService.instance?.typeText(prompt) ?: false
    }
    
    suspend fun waitForResponse(timeoutMs: Long = 60_000): String {
        val startTime = System.currentTimeMillis()
        var lastText = ""
        var sameCount = 0
        
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            delay(1000)
            
            val currentText = screenReader.extractAllText()
            
            if (currentText == lastText) {
                sameCount++
                if (sameCount >= 2) {
                    return currentText
                }
            } else {
                sameCount = 0
                lastText = currentText
            }
        }
        
        return lastText
    }
    
    fun extractResponse(meta: AIAppMeta): String {
        when (meta.responseExtraction) {
            ResponseExtraction.SCREEN_TEXT -> {
                return screenReader.extractAllText()
            }
            ResponseExtraction.OCR_REQUIRED -> {
                return screenReader.extractAllText()
            }
            ResponseExtraction.COPY_BUTTON -> {
                return tryCopyFromUI()
            }
            ResponseExtraction.SHARE_MENU -> {
                return tryShareFromUI()
            }
        }
        return ""
    }
    
    fun getWorkingMemory(): TaskWorkingMemory = workingMemory
    
    private fun tryCopyFromUI(): String {
        return screenReader.extractAllText()
    }
    
    private fun tryShareFromUI(): String {
        return screenReader.extractAllText()
    }
}