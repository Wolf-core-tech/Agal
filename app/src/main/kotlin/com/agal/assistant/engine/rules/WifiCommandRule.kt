package com.agal.assistant.engine.rules

import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import com.agal.assistant.engine.CommandRule
import com.agal.assistant.engine.models.CommandResult
import com.agal.assistant.engine.models.CommandType

class WifiCommandRule : CommandRule {
    override val name: String = "WiFi Command"

    override fun matches(input: String): Boolean {
        val lower = input.lowercase().trim()
        return lower.contains("wifi") || lower.contains("wi-fi") || lower.contains("wi fi") ||
                lower.contains("wireless") || lower.contains("internet connection") ||
                lower.contains("turn on wifi") || lower.contains("turn off wifi") ||
                lower.contains("enable wifi") || lower.contains("disable wifi")
    }

    override suspend fun execute(input: String, context: Context): CommandResult {
        val lower = input.lowercase().trim()
        val isTurnOff = lower.contains("off") || lower.contains("disable")

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ cannot toggle WiFi programmatically; open settings panel
                val panelIntent = Intent(Settings.Panel.ACTION_WIFI).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(panelIntent)
                CommandResult(
                    type = CommandType.TOGGLE_WIFI,
                    rawInput = input,
                    spokenReply = if (isTurnOff) "Opening WiFi settings to turn it off." else "Opening WiFi settings to turn it on.",
                    displayTitle = "WiFi Settings",
                    displayDescription = "WiFi panel opened (Android 10+ policy)",
                    isSuccess = true
                )
            } else {
                @Suppress("DEPRECATION")
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                val newState = !isTurnOff
                @Suppress("DEPRECATION")
                wifiManager?.isWifiEnabled = newState
                CommandResult(
                    type = CommandType.TOGGLE_WIFI,
                    rawInput = input,
                    spokenReply = if (newState) "WiFi turned on." else "WiFi turned off.",
                    displayTitle = if (newState) "WiFi On" else "WiFi Off",
                    displayDescription = if (newState) "WiFi enabled" else "WiFi disabled",
                    isSuccess = true
                )
            }
        } catch (e: Exception) {
            // Fallback: open WiFi settings
            try {
                val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
            CommandResult(
                type = CommandType.TOGGLE_WIFI,
                rawInput = input,
                spokenReply = "Opening WiFi settings.",
                displayTitle = "WiFi Settings",
                displayDescription = "Opened WiFi settings",
                isSuccess = true
            )
        }
    }
}
