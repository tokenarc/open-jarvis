package com.openjarvis.intelligence

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TaskRouter(private val context: Context) {
    
    private val analyzer = AppAnalyzer(context)
    
    suspend fun analyze(command: String): ExecutionPlan = withContext(Dispatchers.IO) {
        val lower = command.lowercase()
        val neededCaps = detectNeededCapabilities(lower)
        val targetCategory = detectTargetCategory(lower)
        
        val candidates = analyzer.getAllSortedByTrust()
            .filter { entity ->
                (targetCategory.isEmpty() || entity.category == targetCategory) ||
                neededCaps.any { cap -> entity.capabilities.contains(cap.name) }
            }
            .take(5)
        
        if (candidates.isEmpty()) {
            return@withContext ExecutionPlan(
                steps = emptyList(),
                reasoning = "No matching apps found, using default behavior"
            )
        }
        
        val scoring = candidates.mapIndexed { index, entity ->
            val trustScore = entity.trustScore * 0.4f
            val capMatch = if (neededCaps.any { entity.capabilities.contains(it.name) }) 0.4f else 0f
            val recency = (1f - index * 0.2f).coerceAtLeast(0f) * 0.2f
            val total = trustScore + capMatch + recency
            
            ScoredApp(
                packageName = entity.packageName,
                appName = entity.appName,
                score = total,
                isAIApp = entity.isAIApp
            )
        }.sortedByDescending { it.score }
        
        val topApp = scoring.firstOrNull()
        val reasoning = buildString {
            append("Selected ${topApp?.appName} (score: ${String.format("%.2f", topApp?.score ?: 0f)})")
            if (scoring.size > 1) {
                append(". Alternatives: ${scoring.drop(1).take(2).joinToString { it.appName }}")
            }
        }
        
        val steps = if (topApp != null) {
            listOf(
                ExecutionStep(
                    appPackage = topApp.packageName,
                    appLabel = topApp.appName,
                    stepType = StepType.OPEN_APP
                )
            )
        } else emptyList()
        
        ExecutionPlan(
            steps = steps,
            reasoning = reasoning,
            scoredApps = scoring
        )
    }
    
    private fun detectNeededCapabilities(command: String): List<Capability> {
        val caps = mutableListOf<Capability>()
        
        when {
            command.contains("search") -> caps.add(Capability.SEARCH)
            command.contains("ask ai") || command.contains("ask chat") || command.contains("reason") -> caps.add(Capability.AI_REASONING)
            command.contains("message") || command.contains("send") || command.contains("text to") -> caps.add(Capability.MESSAGING)
            command.contains("read") || command.contains("file") -> caps.add(Capability.FILE_READ)
            command.contains("write") || command.contains("save") || command.contains("note") -> caps.add(Capability.FILE_WRITE)
            command.contains("navigate") || command.contains("direction") || command.contains("directions to") -> caps.add(Capability.NAVIGATION)
            command.contains("pay") || command.contains("payment") || command.contains("buy") -> caps.add(Capability.PAYMENT)
            command.contains("music") || command.contains("play") -> caps.add(Capability.MEDIA_PLAY)
            command.contains("browse") || command.contains("web") || command.contains("internet") -> caps.add(Capability.WEB_BROWSE)
            command.contains("photo") || command.contains("capture") || command.contains("camera") -> caps.add(Capability.IMAGE_CAPTURE)
            command.contains("email") || command.contains("mail") -> caps.add(Capability.EMAIL)
            command.contains("calendar") || command.contains("schedule") -> caps.add(Capability.CALENDAR)
            command.contains("translate") -> caps.add(Capability.TRANSLATE)
        }
        
        if (caps.isEmpty()) caps.add(Capability.AI_REASONING)
        
        return caps
    }
    
    private fun detectTargetCategory(command: String): String {
        return when {
            command.contains("message") || command.contains("text to") || command.contains("whatsapp") -> "COMMUNICATION"
            command.contains("social") || command.contains("post") || command.contains("facebook") -> "SOCIAL"
            command.contains("search") || command.contains("browse") -> "BROWSER"
            command.contains("music") || command.contains("spotify") -> "ENTERTAINMENT"
            command.contains("maps") || command.contains("navigate") -> "UTILITIES"
            command.contains("drive") || command.contains("docs") || command.contains("document") -> "PRODUCTIVITY"
            command.contains("shop") || command.contains("buy") -> "SHOPPING"
            command.contains("email") || command.contains("mail") -> "EMAIL"
            command.contains("calendar") || command.contains("schedule") -> "CALENDAR"
            command.contains("note") || command.contains("keep") -> "NOTES"
            else -> ""
        }
    }
    
    data class ScoredApp(
        val packageName: String,
        val appName: String,
        val score: Float,
        val isAIApp: Boolean
    )
}