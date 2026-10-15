package com.agal.assistant.wakeword

import android.content.Context
import android.util.Log
import ai.picovoice.porcupine.Porcupine
import ai.picovoice.porcupine.PorcupineManager
import ai.picovoice.porcupine.PorcupineManagerCallback

class PorcupineWakeWordEngine(
    private val context: Context,
    private val onWakeWordDetected: (String) -> Unit
) {
    private val TAG = "PorcupineWakeWord"
    private var porcupineManager: PorcupineManager? = null
    private var isListening = false

    var currentWakeWord: String = "Hey Agal"

    fun init(accessKey: String? = null, customKeywordPath: String? = null) {
        try {
            val key = accessKey ?: ""
            if (key.isNotEmpty()) {
                val callback = PorcupineManagerCallback { keywordIndex ->
                    Log.i(TAG, "Porcupine detected wake word index: $keywordIndex")
                    onWakeWordDetected(currentWakeWord)
                }

                val builder = PorcupineManager.Builder()
                    .setAccessKey(key)

                if (customKeywordPath != null && customKeywordPath.isNotEmpty()) {
                    builder.setKeywordPath(customKeywordPath)
                } else {
                    // Default built-in keyword
                    builder.setKeyword(Porcupine.BuiltInKeyword.JARVIS)
                }

                porcupineManager = builder.build(context, callback)
                Log.i(TAG, "PorcupineManager successfully initialized")
            } else {
                Log.i(TAG, "No Picovoice AccessKey set. Using local acoustic detector fallback.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Porcupine: ${e.localizedMessage}")
        }
    }

    fun start() {
        if (isListening) return
        try {
            porcupineManager?.start()
            isListening = true
            Log.i(TAG, "Porcupine wake-word listening started for: $currentWakeWord")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting Porcupine: ${e.localizedMessage}")
            isListening = false
        }
    }

    fun stop() {
        if (!isListening) return
        try {
            porcupineManager?.stop()
            isListening = false
            Log.i(TAG, "Porcupine wake-word listening stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping Porcupine: ${e.localizedMessage}")
        }
    }

    /**
     * Trigger wake word manually or via simulation
     */
    fun simulateWakeWordTrigger() {
        onWakeWordDetected(currentWakeWord)
    }

    fun isListening(): Boolean = isListening

    fun release() {
        stop()
        try {
            porcupineManager?.delete()
            porcupineManager = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing Porcupine: ${e.localizedMessage}")
        }
    }
}
