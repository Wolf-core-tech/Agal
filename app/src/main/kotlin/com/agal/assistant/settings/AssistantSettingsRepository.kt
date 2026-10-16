package com.agal.assistant.settings

import android.content.Context
import android.content.SharedPreferences

data class WakeWordSettings(
    val activeWakeWord: String = "Hey Agal",
    val listeningReplyText: String = "Yes Venkat",
    val language: String = "EN" // "EN" or "TA"
)

class AssistantSettingsRepository(context: Context) {
    companion object {
        private const val PREFS_NAME = "agal_assistant_prefs"
        private const val KEY_WAKE_WORD = "pref_wake_word"
        private const val KEY_LISTENING_REPLY = "pref_listening_reply"
        private const val KEY_LANGUAGE = "pref_language"

        // Preset listening replies in English and Tamil
        val LISTENING_REPLY_PRESETS_EN = listOf(
            "Yes Venkat",
            "Yes Venkat, I'm listening.",
            "Yes, I'm here.",
            "How can I help you, Venkat?",
            "Agal here, go ahead!"
        )

        val LISTENING_REPLY_PRESETS_TA = listOf(
            "சொல்லுங்க, நான் கேட்கிறேன்.", // Sollunga, naan ketkiren
            "நான் தயாராக இருக்கிறேன்.",   // Naan thayaraga irukkiren
            "உங்களுக்கு என்ன வேண்டும்?",  // Ungalukku enna vendum?
            "வணக்கம், சொல்லுங்க."          // Vanakkam, sollunga
        )

        val WAKE_WORD_PRESETS = listOf(
            "Hey Agal",
            "Agal",
            "வணக்கம் அகல்", // Vanakkam Agal
            "அகல்",          // Agal in Tamil
            "Hey Assistant",
            "Jarvis"
        )
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getWakeWord(): String {
        return prefs.getString(KEY_WAKE_WORD, "Hey Agal") ?: "Hey Agal"
    }

    fun setWakeWord(word: String) {
        prefs.edit().putString(KEY_WAKE_WORD, word.trim()).apply()
    }

    fun getListeningReply(): String {
        val saved = prefs.getString(KEY_LISTENING_REPLY, null)
        return if (saved.isNullOrBlank() || saved == "I'm listening." || saved.contains("Saran")) {
            "Yes Venkat"
        } else {
            saved
        }
    }

    fun setListeningReply(reply: String) {
        val clean = reply.trim().ifEmpty { "Yes Venkat" }
        prefs.edit().putString(KEY_LISTENING_REPLY, clean).apply()
    }

    fun getLanguage(): String {
        return prefs.getString(KEY_LANGUAGE, "EN") ?: "EN"
    }

    fun setLanguage(lang: String) {
        prefs.edit().putString(KEY_LANGUAGE, lang).apply()
    }
}
