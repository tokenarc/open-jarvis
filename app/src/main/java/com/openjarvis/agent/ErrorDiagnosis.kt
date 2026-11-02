package com.openjarvis.agent

/** Converts low-level failures into actionable diagnostics while retaining technical detail. */
object ErrorDiagnosis {
    data class Diagnosis(
        val code: String,
        val stage: String,
        val reason: String,
        val suggestedFix: String,
        val retryable: Boolean
    ) {
        fun display(detail: String): String =
            "$code | $stage: $reason. Fix: $suggestedFix. Detail: $detail"
    }

    fun fromThrowable(error: Throwable, stage: String = "Ollama"): Diagnosis {
        val detail = generateSequence(error) { it.cause }
            .joinToString(" ") { "${it.javaClass.simpleName}: ${it.message.orEmpty()}" }
            .lowercase()

        return when {
            "timed out" in detail || "timeout" in detail ->
                Diagnosis("JARVIS-NET-001", stage, "Ollama response timed out",
                    "Check that Ollama is running, the model is loaded, and the device can reach its server.", true)
            "unable to resolve" in detail || "failed to connect" in detail ||
                "connection refused" in detail || "connectexception" in detail ->
                Diagnosis("JARVIS-OLLAMA-001", stage, "Cannot connect to the Ollama server",
                    "Start Ollama and verify the configured server URL and port 11434.", true)
            "model" in detail && ("not found" in detail || "does not exist" in detail) ->
                Diagnosis("JARVIS-OLLAMA-002", stage, "Configured Ollama model is missing",
                    "Pull the configured model in Ollama, then test the connection again.", false)
            "http 429" in detail ->
                Diagnosis("JARVIS-OLLAMA-429", stage, "Ollama server is overloaded",
                    "Wait briefly and retry after the model finishes its current request.", true)
            "http 5" in detail ->
                Diagnosis("JARVIS-OLLAMA-5XX", stage, "Ollama server returned a server error",
                    "Check Ollama logs and available RAM, then retry.", true)
            else ->
                Diagnosis("JARVIS-UNKNOWN-001", stage, "Unexpected failure",
                    "Review the technical detail and diagnostic log before changing settings.", false)
        }
    }

    fun invalidPlan(reason: String): Diagnosis =
        Diagnosis("JARVIS-JSON-001", "AI response parsing",
            "Ollama returned an invalid or empty action plan: $reason",
            "A strict JSON repair request will be attempted once; if it still fails, inspect the raw response.",
            true)
}
