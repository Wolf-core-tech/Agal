package com.agal.assistant.engine.rules

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.agal.assistant.engine.CommandRule
import com.agal.assistant.engine.models.CommandResult
import com.agal.assistant.engine.models.CommandType

class HotspotCommandRule : CommandRule {
    override val name: String = "Hotspot Command"

    override fun matches(input: String): Boolean {
        val lower = input.lowercase().trim()
        return lower.contains("hotspot") ||
                lower.contains("mobile data sharing") ||
                lower.contains("tethering") ||
                lower.contains("wifi hotspot") ||
                lower.contains("personal hotspot") ||
                lower.contains("internet sharing")
    }

    override suspend fun execute(input: String, context: Context): CommandResult {
        val lower = input.lowercase().trim()
        val isTurnOff = lower.contains("off") || lower.contains("disable") || lower.contains("turn off")

        return try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                Intent("android.intent.action.VIEW").apply {
                    setClassName(
                        "com.android.settings",
                        "com.android.settings.TetherSettings"
                    )
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }

            try {
                context.startActivity(intent)
            } catch (_: Exception) {
                val fallback = Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
            }

            CommandResult(
                type = CommandType.TOGGLE_HOTSPOT,
                rawInput = input,
                spokenReply = if (isTurnOff) "Opening hotspot settings to turn it off." else "Opening hotspot settings to turn it on.",
                displayTitle = if (isTurnOff) "Hotspot Off" else "Hotspot On",
                displayDescription = "Mobile hotspot settings opened",
                isSuccess = true
            )
        } catch (e: Exception) {
            CommandResult(
                type = CommandType.TOGGLE_HOTSPOT,
                rawInput = input,
                spokenReply = "Could not open hotspot settings: ${e.localizedMessage}",
                displayTitle = "Hotspot Error",
                displayDescription = e.localizedMessage ?: "Unknown error",
                isSuccess = false
            )
        }
    }
}
