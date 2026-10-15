package com.agal.assistant.custom

import org.json.JSONObject
import java.util.UUID

data class CustomVoiceCommand(
    val id: String = UUID.randomUUID().toString(),
    val triggerPhrase: String,      // e.g. "party time" or "study mode" or "good night"
    val title: String,              // e.g. "Party Mode"
    val actionType: CustomActionType,
    val actionPayload: String = "", // e.g. "ON", "OFF", "MAX", "MUTE", "com.google.android.youtube"
    val spokenReply: String = "",   // What Agal will say aloud
    val isEnabled: Boolean = true
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("triggerPhrase", triggerPhrase)
            put("title", title)
            put("actionType", actionType.name)
            put("actionPayload", actionPayload)
            put("spokenReply", spokenReply)
            put("isEnabled", isEnabled)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): CustomVoiceCommand {
            val typeStr = json.optString("actionType", CustomActionType.SPEAK_REPLY.name)
            val actionType = try {
                CustomActionType.valueOf(typeStr)
            } catch (e: Exception) {
                CustomActionType.SPEAK_REPLY
            }

            return CustomVoiceCommand(
                id = json.optString("id", UUID.randomUUID().toString()),
                triggerPhrase = json.optString("triggerPhrase", ""),
                title = json.optString("title", ""),
                actionType = actionType,
                actionPayload = json.optString("actionPayload", ""),
                spokenReply = json.optString("spokenReply", ""),
                isEnabled = json.optBoolean("isEnabled", true)
            )
        }
    }
}
