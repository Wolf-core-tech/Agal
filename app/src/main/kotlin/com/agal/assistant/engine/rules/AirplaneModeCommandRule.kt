package com.agal.assistant.engine.rules

import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.agal.assistant.engine.CommandRule
import com.agal.assistant.engine.models.CommandResult
import com.agal.assistant.engine.models.CommandType

class AirplaneModeCommandRule : CommandRule {
    override val name: String = "Airplane Mode Command"

    override fun matches(input: String): Boolean {
        val lower = input.lowercase().trim()
        return lower.contains("airplane mode") ||
                lower.contains("aeroplane mode") ||
                lower.contains("flight mode") ||
                lower.contains("plane mode") ||
                lower.contains("air mode") ||
                lower.contains("airplane") ||
                lower.contains("aeroplane")
    }

    override suspend fun execute(input: String, context: Context): CommandResult {
        val lower = input.lowercase().trim()
        val isTurnOff = lower.contains("off") || lower.contains("disable") || lower.contains("turn off")

        return try {
            // Android doesn't allow toggling airplane mode programmatically post-Jelly Bean
            // Open the airplane mode panel or settings instead
            val intent = try {
                Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } catch (_: Exception) {
                Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(intent)

            CommandResult(
                type = CommandType.TOGGLE_AIRPLANE,
                rawInput = input,
                spokenReply = if (isTurnOff) "Opening settings to turn off airplane mode." else "Opening settings to turn on airplane mode.",
                displayTitle = if (isTurnOff) "Airplane Mode Off" else "Airplane Mode On",
                displayDescription = "Airplane mode settings opened",
                isSuccess = true
            )
        } catch (e: Exception) {
            CommandResult(
                type = CommandType.TOGGLE_AIRPLANE,
                rawInput = input,
                spokenReply = "Could not open airplane mode settings: ${e.localizedMessage}",
                displayTitle = "Airplane Mode Error",
                displayDescription = e.localizedMessage ?: "Unknown error",
                isSuccess = false
            )
        }
    }
}
