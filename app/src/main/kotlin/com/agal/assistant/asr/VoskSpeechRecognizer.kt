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

    private val grammarJson = """["open youtube", "turn on bluetooth", "turn off bluetooth", "open camera", "turn on flashlight", "turn off flashlight", "what time is it", "what is today's date", "battery status", "tell me a joke", "open settings", "volume up", "volume down", "mute", "hey agal", "agal", "help", "[unk]"]"""

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

    fun startListening() {
        if (isListening) return

        try {
            if (model != null) {
                val recognizer = Recognizer(model, 16000.0f, grammarJson)
                speechService = SpeechService(recognizer, 16000.0f)
                speechService?.startListening(this)
                isListening = true
                Log.i(TAG, "Vosk offline speech recognition started")
            } else {
                Log.w(TAG, "Vosk model not yet loaded. Using direct command loop.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting Vosk speech service: ${e.localizedMessage}")
        }
    }

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
