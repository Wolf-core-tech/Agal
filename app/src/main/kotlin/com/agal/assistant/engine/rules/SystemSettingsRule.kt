package com.agal.assistant.engine.rules

import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.agal.assistant.engine.CommandRule
import com.agal.assistant.engine.models.CommandResult
import com.agal.assistant.engine.models.CommandType

class SystemSettingsRule : CommandRule {
    override val name: String = "Settings Command"

    override fun matches(input: String): Boolean {
        val lower = input.lowercase().trim()
        return lower.contains("setting")
    }

    override suspend fun execute(input: String, context: Context): CommandResult {
        return try {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)

            CommandResult(
                type = CommandType.OPEN_SETTINGS,
                rawInput = input,
                spokenReply = "Opening device settings.",
                displayTitle = "Settings",
                displayDescription = "System settings launched",
                isSuccess = true
            )
        } catch (e: Exception) {
            CommandResult(
                type = CommandType.OPEN_SETTINGS,
                rawInput = input,
                spokenReply = "Could not open settings: ${e.localizedMessage}",
                displayTitle = "Settings Error",
                displayDescription = e.localizedMessage ?: "Unknown error",
                isSuccess = false
            )
        }
    }
}
