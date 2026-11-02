package com.openjarvis.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.UUID

class AndroidTTSEngine(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var isReady = false
    private var speechRate = 1.05f
    private var pitch = 0.95f

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isReady = true
                tts?.language = Locale.forLanguageTag("hi-IN")
                tts?.setSpeechRate(speechRate)
                tts?.setPitch(pitch)
                
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {}
                    override fun onError(utteranceId: String?) {}
                })
            }
        }
    }

    fun speak(text: String) {
        if (!isReady) return
        
        val clean = text
            .replace(Regex("\\[\\{.*?\\}\\]"), "")
            .replace(Regex("[\\[\\]{}\":]"), " ")
            .trim()
        
        if (clean.isBlank()) return
        configureVoiceForText(clean)
        
        val utteranceId = "jarvis_${UUID.randomUUID()}"
        tts?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun speakQueued(text: String) {
        if (!isReady) return
        
        val clean = text
            .replace(Regex("\\[\\{.*?\\}\\]"), "")
            .replace(Regex("[\\[\\]{}\":]"), " ")
            .trim()
        
        if (clean.isBlank()) return
        configureVoiceForText(clean)
        
        val utteranceId = "jarvis_${UUID.randomUUID()}"
        tts?.speak(clean, TextToSpeech.QUEUE_ADD, null, utteranceId)
    }

    private fun configureVoiceForText(text: String) {
        val hasDevanagari = text.any { it in '\u0900'..'\u097F' }
        val marathiMarkers = listOf("आहे", "आणि", "मला", "काय", "करा", "झाले", "झालं", "नाही", "दाखवा", "उघडा", "करायचे", "माझे", "तुम्ही", "सांगा", "नको")
        val language = when {
            !hasDevanagari -> "en"
            marathiMarkers.any { text.contains(it) } -> "mr"
            else -> "hi"
        }
        val locale = Locale.forLanguageTag("$language-IN")
        val engine = tts ?: return
        val bestVoice = engine.voices
            ?.filter { it.locale.language == language && it.locale.country.equals("IN", ignoreCase = true) }
            ?.maxWithOrNull(compareBy<android.speech.tts.Voice> { it.quality }.thenBy { it.isNetworkConnectionRequired })
        if (bestVoice != null) {
            engine.voice = bestVoice
        } else {
            engine.language = locale
        }
    }
    fun stop() {
        tts?.stop()
    }

    fun setSpeechRate(rate: Float) {
        speechRate = rate.coerceIn(0.5f, 2.0f)
        tts?.setSpeechRate(speechRate)
    }

    fun setPitch(pitchValue: Float) {
        pitch = pitchValue.coerceIn(0.5f, 2.0f)
        tts?.setPitch(pitch)
    }

    fun getSpeechRate(): Float = speechRate
    fun getPitch(): Float = pitch

    fun isSpeaking(): Boolean = tts?.isSpeaking ?: false

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}