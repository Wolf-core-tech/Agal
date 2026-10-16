package com.agal.assistant.asr

import android.content.Context
import android.util.Log
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService
import java.io.IOException

class VoskSpeechRecognizer(
    private val context: Context,
    private val onResultCallback: (String, Boolean) -> Unit
) : RecognitionListener {

    private val TAG = "VoskSpeechRecognizer"
    private var model: Model? = null
    private var speechService: SpeechService? = null
    private var isListening = false

    /**
     * Narrow grammar for standby wake-word detection.
     * Vosk will only recognise these words/phrases — everything else is discarded.
     * This dramatically reduces false wake-word triggers and CPU usage.
     */
    private val standbyGrammarJson = """["agal", "hey agal", "jarvis", "[unk]"]"""

    fun initModel(onModelReady: (Boolean) -> Unit) {
        StorageService.unpack(context, "model-en-us", "model",
            { unpackedModel: Model ->
                model = unpackedModel
                Log.i(TAG, "Vosk offline acoustic model loaded successfully")
                onModelReady(true)
            },
            { exception: IOException ->
                Log.w(TAG, "Could not unpack bundled Vosk model: ${exception.localizedMessage}. Fallback recognition active.")
                onModelReady(false)
            }
        )
    }

    /**
     * STANDBY mode: uses the narrow wake-word grammar.
     * Vosk only recognises "agal" / "hey agal" — all other audio is silently ignored.
     * Low CPU, no false command triggers.
     */
    fun startListeningStandby() {
        if (isListening) stopListening()
        try {
            if (model != null) {
                val recognizer = Recognizer(model, 16000.0f, standbyGrammarJson)
                speechService = SpeechService(recognizer, 16000.0f)
                speechService?.startListening(this)
                isListening = true
                Log.i(TAG, "Vosk STANDBY (wake-word grammar) listening started")
            } else {
                Log.w(TAG, "Vosk model not yet loaded — standby not started")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting Vosk standby: ${e.localizedMessage}")
        }
    }

    /**
     * COMMAND mode: open vocabulary — recognises any spoken words.
     * Used after the wake word fires to capture the user's full command.
     */
    fun startListeningCommand() {
        if (isListening) stopListening()
        try {
            if (model != null) {
                val recognizer = Recognizer(model, 16000.0f)
                speechService = SpeechService(recognizer, 16000.0f)
                speechService?.startListening(this)
                isListening = true
                Log.i(TAG, "Vosk COMMAND (open vocabulary) listening started")
            } else {
                Log.w(TAG, "Vosk model not yet loaded — command not started")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting Vosk command: ${e.localizedMessage}")
        }
    }

    /** Legacy method kept for backward compatibility with ViewModel direct calls */
    fun startListening() = startListeningCommand()

    fun stopListening() {
        if (!isListening) return
        try {
            speechService?.stop()
            isListening = false
            Log.i(TAG, "Vosk speech recognition stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping Vosk: ${e.localizedMessage}")
        }
    }

    override fun onPartialResult(hypothesis: String?) {
        if (hypothesis.isNullOrBlank()) return
        val text = extractTextFromJson(hypothesis)
        if (text.isNotEmpty()) {
            onResultCallback(text, false)
        }
    }

    override fun onResult(hypothesis: String?) {
        if (hypothesis.isNullOrBlank()) return
        val text = extractTextFromJson(hypothesis)
        if (text.isNotEmpty()) {
            onResultCallback(text, true)
        }
    }

    override fun onFinalResult(hypothesis: String?) {
        if (hypothesis.isNullOrBlank()) return
        val text = extractTextFromJson(hypothesis)
        if (text.isNotEmpty()) {
            onResultCallback(text, true)
        }
    }

    override fun onError(exception: java.lang.Exception?) {
        Log.e(TAG, "Vosk recognition error: ${exception?.localizedMessage}")
        isListening = false
    }

    override fun onTimeout() {
        Log.i(TAG, "Vosk recognition timeout")
        isListening = false
    }

    private fun extractTextFromJson(jsonStr: String): String {
        return try {
            val json = JSONObject(jsonStr)
            json.optString("text", json.optString("partial", ""))
        } catch (e: Exception) {
            ""
        }
    }

    fun release() {
        stopListening()
        speechService?.shutdown()
        speechService = null
        model = null
    }

    fun isListening(): Boolean = isListening
}
