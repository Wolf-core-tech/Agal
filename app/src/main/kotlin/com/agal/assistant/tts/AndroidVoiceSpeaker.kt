package com.agal.assistant.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import java.util.Locale

class AndroidVoiceSpeaker(
    private val context: Context,
    private val onSpeakingChanged: (Boolean) -> Unit = {}
) : TextToSpeech.OnInitListener {

    private val TAG = "AndroidVoiceSpeaker"
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    // Natural human female defaults: slightly warm pitch & conversational flow
    var speechRate: Float = 0.98f
        set(value) {
            field = value.coerceIn(0.5f, 2.0f)
            tts?.setSpeechRate(field)
        }

    var pitch: Float = 1.12f
        set(value) {
            field = value.coerceIn(0.5f, 2.0f)
            tts?.setPitch(field)
        }

    var isMuted: Boolean = false

    fun init() {
        if (tts == null) {
            tts = TextToSpeech(context.applicationContext, this)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.let { engine ->
                // Set language to US or IN English
                val langResult = engine.setLanguage(Locale.US)
                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    engine.setLanguage(Locale.getDefault())
                }

                // Pick the best natural human female voice from available engine voices
                selectHumanFemaleVoice(engine)

                engine.setSpeechRate(speechRate)
                engine.setPitch(pitch)

                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        onSpeakingChanged(true)
                    }

                    override fun onDone(utteranceId: String?) {
                        onSpeakingChanged(false)
                    }

                    override fun onError(utteranceId: String?) {
                        onSpeakingChanged(false)
                    }
                })

                isInitialized = true
                Log.i(TAG, "Natural Human Female TTS Voice Initialized")
            }
        } else {
            Log.e(TAG, "TTS initialization failed with status $status")
        }
    }

    private fun selectHumanFemaleVoice(engine: TextToSpeech) {
        try {
            val availableVoices = engine.voices
            if (!availableVoices.isNullOrEmpty()) {
                // Look for high-quality female network/natural voices (Google TTS female voice sets)
                val targetVoice = availableVoices.firstOrNull { voice ->
                    val name = voice.name.lowercase()
                    // Target female, high quality / neural voices
                    (name.contains("female") || name.contains("en-us-x-sfg") || name.contains("en-in-x-cfl") ||
                     name.contains("en-us-x-tpd") || name.contains("en-us-x-iom") || name.contains("network")) &&
                    !voice.isNetworkConnectionRequired &&
                    voice.quality >= Voice.QUALITY_NORMAL
                } ?: availableVoices.firstOrNull { voice ->
                    val name = voice.name.lowercase()
                    (name.contains("female") || name.contains("woman") || name.contains("girl") || name.contains("#female"))
                } ?: availableVoices.firstOrNull { voice ->
                    voice.locale.language == "en" && voice.quality >= Voice.QUALITY_HIGH
                }

                if (targetVoice != null) {
                    engine.voice = targetVoice
                    Log.i(TAG, "Selected Natural Female Voice: ${targetVoice.name}")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Voice selection fallback: ${e.localizedMessage}")
        }
    }

    fun speak(text: String, onDone: (() -> Unit)? = null) {
        if (isMuted || text.trim().isEmpty()) {
            onDone?.invoke()
            return
        }

        if (!isInitialized) {
            init()
        }

        val utteranceId = "Agal_TTS_${System.currentTimeMillis()}"
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        tts?.stop()
        onSpeakingChanged(false)
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
