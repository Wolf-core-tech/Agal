package com.agal.assistant.lifestyle

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class SavedCredential(
    val id: String = java.util.UUID.randomUUID().toString(),
    val accountName: String,
    val userIdOrEmail: String,
    val passwordOrPin: String,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("accountName", accountName)
            put("userIdOrEmail", userIdOrEmail)
            put("passwordOrPin", passwordOrPin)
            put("notes", notes)
            put("timestamp", timestamp)
        }
    }

    companion object {
        fun fromJson(obj: JSONObject): SavedCredential {
            return SavedCredential(
                id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                accountName = obj.optString("accountName", ""),
                userIdOrEmail = obj.optString("userIdOrEmail", ""),
                passwordOrPin = obj.optString("passwordOrPin", ""),
                notes = obj.optString("notes", ""),
                timestamp = obj.optLong("timestamp", System.currentTimeMillis())
            )
        }
    }
}

class CredentialVaultRepository(private val context: Context) {
    companion object {
        private const val PREFS_NAME = "agal_vault_credentials_prefs"
        private const val KEY_CREDENTIALS = "saved_credentials_json"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getCredentials(): List<SavedCredential> {
        val raw = prefs.getString(KEY_CREDENTIALS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<SavedCredential>()
            for (i in 0 until array.length()) {
                list.add(SavedCredential.fromJson(array.getJSONObject(i)))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveCredential(accountName: String, userIdOrEmail: String, passwordOrPin: String, notes: String = ""): List<SavedCredential> {
        val cred = SavedCredential(
            accountName = accountName.trim(),
            userIdOrEmail = userIdOrEmail.trim(),
            passwordOrPin = passwordOrPin.trim(),
            notes = notes.trim()
        )
        val current = getCredentials().toMutableList()
        current.add(0, cred)
        saveList(current)
        return current
    }

    fun deleteCredential(id: String): List<SavedCredential> {
        val current = getCredentials().filterNot { it.id == id }
        saveList(current)
        return current
    }

    private fun saveList(list: List<SavedCredential>) {
        val array = JSONArray()
        list.forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY_CREDENTIALS, array.toString()).apply()
    }
}
