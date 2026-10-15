package com.agal.assistant.engine.rules

import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.agal.assistant.engine.CommandRule
import com.agal.assistant.engine.models.CommandResult
import com.agal.assistant.engine.models.CommandType

class BluetoothCommandRule : CommandRule {
    override val name: String = "Bluetooth Command"

    override fun matches(input: String): Boolean {
        val lower = input.lowercase().trim()
        return lower.contains("bluetooth")
    }

    override suspend fun execute(input: String, context: Context): CommandResult {
        val lower = input.lowercase().trim()
        val isTurnOff = lower.contains("off") || lower.contains("disable")

        return try {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)

            val reply = if (isTurnOff) {
                "Opening Bluetooth settings to manage connections."
            } else {
                "Opening Bluetooth settings so you can switch it on."
            }

            CommandResult(
                type = CommandType.OPEN_BLUETOOTH,
                rawInput = input,
                spokenReply = reply,
                displayTitle = "Bluetooth",
                displayDescription = if (isTurnOff) "Bluetooth settings opened (Turn Off)" else "Bluetooth settings opened (Turn On)",
                isSuccess = true
            )
        } catch (e: Exception) {
            CommandResult(
                type = CommandType.OPEN_BLUETOOTH,
                rawInput = input,
                spokenReply = "Could not open Bluetooth settings.",
                displayTitle = "Bluetooth Error",
                displayDescription = e.localizedMessage ?: "Unknown error",
                isSuccess = false
            )
        }
    }
}
