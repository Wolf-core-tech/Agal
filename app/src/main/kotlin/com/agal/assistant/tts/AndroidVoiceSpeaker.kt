package com.agal.assistant.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

class AndroidVoiceSpeaker(
    private val context: Context,
    private val onSpeakingChanged: (Boolean) -> Unit = {}
) : TextToSpeech.OnInitListener {

    private val TAG = "AndroidVoiceSpeaker"
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    var speechRate: Float = 1.0f
        set(value) {
            field = value.coerceIn(0.5f, 2.0f)
            tts?.setSpeechRate(field)
        }

    var pitch: Float = 1.0f
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
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w(TAG, "Language en-US missing or not supported in local TTS")
            } else {
                isInitialized = true
                tts?.setSpeechRate(speechRate)
                tts?.setPitch(pitch)

                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
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
            }
        } else {
            Log.e(TAG, "TTS initialization failed with status $status")
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
