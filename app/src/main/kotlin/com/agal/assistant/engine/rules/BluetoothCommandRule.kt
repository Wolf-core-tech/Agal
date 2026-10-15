package com.agal.assistant.engine.rules

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Build
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
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = bluetoothManager?.adapter

            if (adapter == null) {
                return CommandResult(
                    type = CommandType.OPEN_BLUETOOTH,
                    rawInput = input,
                    spokenReply = "Bluetooth is not available on this device.",
                    displayTitle = "Bluetooth",
                    displayDescription = "No Bluetooth hardware",
                    isSuccess = false
                )
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+ cannot toggle BT directly — open settings panel
                val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                val reply = if (isTurnOff) "Opening Bluetooth settings to turn it off." else "Opening Bluetooth settings to turn it on."
                CommandResult(
                    type = CommandType.OPEN_BLUETOOTH,
                    rawInput = input,
                    spokenReply = reply,
                    displayTitle = "Bluetooth Settings",
                    displayDescription = "Bluetooth settings opened (Android 13+ policy)",
                    isSuccess = true
                )
            } else {
                @Suppress("DEPRECATION")
                if (isTurnOff) {
                    adapter.disable()
                    CommandResult(
                        type = CommandType.OPEN_BLUETOOTH,
                        rawInput = input,
                        spokenReply = "Bluetooth turned off.",
                        displayTitle = "Bluetooth Off",
                        displayDescription = "Bluetooth disabled",
                        isSuccess = true
                    )
                } else {
                    adapter.enable()
                    CommandResult(
                        type = CommandType.OPEN_BLUETOOTH,
                        rawInput = input,
                        spokenReply = "Bluetooth turned on.",
                        displayTitle = "Bluetooth On",
                        displayDescription = "Bluetooth enabled",
                        isSuccess = true
                    )
                }
            }
        } catch (e: Exception) {
            // Fallback: open settings
            try {
                val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
            CommandResult(
                type = CommandType.OPEN_BLUETOOTH,
                rawInput = input,
                spokenReply = "Opening Bluetooth settings.",
                displayTitle = "Bluetooth Settings",
                displayDescription = "Fallback: settings opened",
                isSuccess = true
            )
        }
    }
}
