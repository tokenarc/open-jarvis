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
            "unable to resolve" in detail || "unknownhostexception" in detail ||
                "failed to connect" in detail || "connection refused" in detail ||
                "connectexception" in detail || "socketexception" in detail ||
                "sockettimeoutexception" in detail ->
                Diagnosis("JARVIS-OLLAMA-001", stage, "Cannot connect to the Ollama server",
                    "Start Ollama and verify the configured server URL and port 11434.", true)
            "model" in detail && ("not found" in detail || "does not exist" in detail) ->
                Diagnosis("JARVIS-OLLAMA-002", stage, "Configured Ollama model is missing",
                    "Pull the configured model in Ollama, then test the connection again.", false)
            "accessibility service unavailable" in detail || "accessibilityservice not connected" in detail ->

                Diagnosis("JARVIS-ACCESS-001", stage, "Android Accessibility Service is not connected",
                    "Enable Open Jarvis Accessibility Service in Android Settings, then retry.", false)
            "app not found" in detail || "could not open app" in detail ->
                Diagnosis("JARVIS-APP-001", stage, "The requested app could not be matched to an installed app",
                    "Check the app name or install the app, then use its visible launcher name.", false)
            "could not tap" in detail || "tap failed" in detail ->
                Diagnosis("JARVIS-UI-001", stage, "The requested screen control was not found or could not be tapped",
                    "Keep the target screen visible and use the exact button or label text shown on screen.", true)
            "could not type" in detail || "input field not found" in detail ->
                Diagnosis("JARVIS-UI-002", stage, "No editable input field was available",
                    "Tap the intended text field first and confirm the keyboard or cursor is visible.", true)
            "timed out waiting" in detail ->
                Diagnosis("JARVIS-UI-003", stage, "Expected screen text did not appear before timeout",
                    "Check whether the app loaded or whether the expected label differs from the screen text.", true)
            "action not supported" in detail || "unsupported action" in detail ->
                Diagnosis("JARVIS-ACTION-001", stage, "The generated action is not implemented by the executor",
                    "Use a supported action or add and test an executor implementation before using it.", false)
            "http 429" in detail || "too many requests" in detail ->
                Diagnosis("JARVIS-OLLAMA-429", stage, "Ollama server is overloaded",
                    "Wait briefly and retry after the model finishes its current request.", true)
            "http 5" in detail || "service unavailable" in detail || "bad gateway" in detail ->
                Diagnosis("JARVIS-OLLAMA-5XX", stage, "Ollama server returned a server error",
                    "Check Ollama logs and available RAM, then retry.", true)
            "outofmemoryerror" in detail || "out of memory" in detail ->
                Diagnosis("JARVIS-MEM-001", stage, "Android ran out of memory",
                    "Close unused apps, reduce the local model load, and retry after memory is available.", false)
            "securityexception" in detail || "permission denied" in detail ->
                Diagnosis("JARVIS-PERM-001", stage, "Android denied a required permission or operation",
                    "Enable the required Android permission or Accessibility access in system settings.", false)
            "illegalstateexception" in detail ->
                Diagnosis("JARVIS-STATE-001", stage, "A component was used in an invalid runtime state",
                    "Reinitialize the affected component and inspect the recorded stack trace.", false)
            "jsonexception" in detail || "invalid ai json" in detail ->
                Diagnosis("JARVIS-JSON-001", stage, "AI response could not be parsed as an action plan",
                    "Request one strict JSON-only repair response, then stop if validation still fails.", true)
            else ->
                Diagnosis("JARVIS-UNKNOWN-001", stage, "Unexpected failure",
                    "Capture the full stack trace, isolate the failing component, and apply only a verified recovery; no unsafe automatic replay.", false)
        }
    }

    fun invalidPlan(reason: String): Diagnosis =
        Diagnosis("JARVIS-JSON-001", "AI response parsing",
            "Ollama returned an invalid or empty action plan: $reason",
            "A strict JSON repair request will be attempted once; if it still fails, inspect the raw response.",
            true)
}
