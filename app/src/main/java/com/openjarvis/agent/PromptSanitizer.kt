package com.openjarvis.agent

object PromptSanitizer {
    
    private val injectionPatterns = listOf(
        Regex("ignore (all |previous |above )?instructions", RegexOption.IGNORE_CASE),
        Regex("you are now", RegexOption.IGNORE_CASE),
        Regex("new instructions", RegexOption.IGNORE_CASE),
        Regex("system prompt", RegexOption.IGNORE_CASE),
        Regex("disregard", RegexOption.IGNORE_CASE),
        Regex("pretend you", RegexOption.IGNORE_CASE),
        Regex("act as", RegexOption.IGNORE_CASE),
        Regex("jailbreak", RegexOption.IGNORE_CASE),
        Regex("DAN mode", RegexOption.IGNORE_CASE)
    )
    
    private const val MAX_COMMAND_LENGTH = 2000
    
    fun sanitize(input: String): SanitizeResult {
        val trimmed = input.trim()
        
        if (trimmed.isBlank()) {
            return SanitizeResult.Rejected("Command cannot be empty")
        }
        
        if (trimmed.length > MAX_COMMAND_LENGTH) {
            return SanitizeResult.Rejected("Command too long. Max $MAX_COMMAND_LENGTH characters.")
        }
        
        for (pattern in injectionPatterns) {
            if (pattern.containsMatchIn(trimmed)) {
                return SanitizeResult.Suspicious(
                    sanitized = trimmed,
                    warning = "Unusual instruction pattern detected"
                )
            }
        }
        
        val clean = trimmed.replace(Regex("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]"), "")
        
        return SanitizeResult.Clean(clean)
    }
    
    sealed class SanitizeResult {
        data class Clean(val text: String) : SanitizeResult()
        data class Suspicious(val sanitized: String, val warning: String) : SanitizeResult()
        data class Rejected(val reason: String) : SanitizeResult()
    }
}