package com.openjarvis.voice

interface STTEngine {
    fun startListening(onResult: (String?) -> Unit, onError: (String) -> Unit)
    fun stop(): String? // Returns transcribed text or null
    fun isListening(): Boolean
    fun release()
}

enum class STTMode {
    PUSH_TO_TALK,  // Hold mic → record, release → transcribe
    AUTO_VAD      // Tap → record until silence → auto-transcribe
}