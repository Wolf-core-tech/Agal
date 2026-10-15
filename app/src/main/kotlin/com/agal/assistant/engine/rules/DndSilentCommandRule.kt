package com.agal.assistant.engine.rules

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.agal.assistant.engine.CommandRule
import com.agal.assistant.engine.models.CommandResult
import com.agal.assistant.engine.models.CommandType

class DndSilentCommandRule : CommandRule {
    override val name: String = "DND & Silent Mode Command"

    override fun matches(input: String): Boolean {
        val lower = input.lowercase().trim()
        return lower.contains("do not disturb") ||
                lower.contains("dnd") ||
                lower.contains("silent mode") ||
                lower.contains("silent on") ||
                lower.contains("silent off") ||
                lower.contains("turn on silent") ||
                lower.contains("turn off silent") ||
                lower.contains("enable silent") ||
                lower.contains("disable silent") ||
                lower.contains("ringer off") ||
                lower.contains("ringer on") ||
                lower.contains("vibrate mode") ||
                lower.contains("sound on") ||
                lower.contains("sound off") ||
                (lower.contains("silent") && (lower.contains("on") || lower.contains("off") || lower.contains("enable") || lower.contains("disable")))
    }

    override suspend fun execute(input: String, context: Context): CommandResult {
        val lower = input.lowercase().trim()
        val isDnd = lower.contains("do not disturb") || lower.contains("dnd")
        val isTurnOff = lower.contains("off") || lower.contains("disable") ||
                lower.contains("sound on") || lower.contains("ringer on")

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

        // Check if we have DND access permission
        if (isDnd) {
            if (notificationManager?.isNotificationPolicyAccessGranted == false) {
                // Direct user to grant DND policy access
                try {
                    val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (_: Exception) {}
                return CommandResult(
                    type = CommandType.TOGGLE_DND,
                    rawInput = input,
                    spokenReply = "Please grant Do Not Disturb permission to Agal. Opening settings.",
                    displayTitle = "DND Permission Needed",
                    displayDescription = "Notification policy access required",
                    isSuccess = false
                )
            }

            return try {
                val newFilter = if (isTurnOff) {
                    NotificationManager.INTERRUPTION_FILTER_ALL  // Normal — DND off
                } else {
                    NotificationManager.INTERRUPTION_FILTER_NONE // DND on (total silence)
                }
                notificationManager?.setInterruptionFilter(newFilter)
                val reply = if (isTurnOff) "Do Not Disturb turned off." else "Do Not Disturb turned on."
                CommandResult(
                    type = CommandType.TOGGLE_DND,
                    rawInput = input,
                    spokenReply = reply,
                    displayTitle = if (isTurnOff) "DND Off" else "DND On",
                    displayDescription = if (isTurnOff) "Interruptions restored" else "All interruptions blocked",
                    isSuccess = true
                )
            } catch (e: Exception) {
                CommandResult(
                    type = CommandType.TOGGLE_DND,
                    rawInput = input,
                    spokenReply = "Failed to toggle Do Not Disturb: ${e.localizedMessage}",
                    displayTitle = "DND Error",
                    displayDescription = e.localizedMessage ?: "Unknown error",
                    isSuccess = false
                )
            }
        }

        // Silent / Vibrate / Normal ringer mode
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
            if (notificationManager?.isNotificationPolicyAccessGranted == false) {
                try {
                    val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (_: Exception) {}
                return CommandResult(
                    type = CommandType.TOGGLE_SILENT,
                    rawInput = input,
                    spokenReply = "Please grant Do Not Disturb permission to control ringer mode. Opening settings.",
                    displayTitle = "Permission Needed",
                    displayDescription = "Notification policy access required",
                    isSuccess = false
                )
            }

            when {
                lower.contains("vibrate") -> {
                    audioManager?.ringerMode = android.media.AudioManager.RINGER_MODE_VIBRATE
                    CommandResult(
                        type = CommandType.TOGGLE_SILENT,
                        rawInput = input,
                        spokenReply = "Vibrate mode enabled.",
                        displayTitle = "Vibrate Mode",
                        displayDescription = "Ringer set to vibrate",
                        isSuccess = true
                    )
                }
                isTurnOff -> {
                    // Turn off silent = restore normal sound
                    audioManager?.ringerMode = android.media.AudioManager.RINGER_MODE_NORMAL
                    CommandResult(
                        type = CommandType.TOGGLE_SILENT,
                        rawInput = input,
                        spokenReply = "Silent mode turned off. Sound restored.",
                        displayTitle = "Silent Off",
                        displayDescription = "Ringer mode set to normal",
                        isSuccess = true
                    )
                }
                else -> {
                    // Turn on silent
                    audioManager?.ringerMode = android.media.AudioManager.RINGER_MODE_SILENT
                    CommandResult(
                        type = CommandType.TOGGLE_SILENT,
                        rawInput = input,
                        spokenReply = "Silent mode enabled.",
                        displayTitle = "Silent Mode On",
                        displayDescription = "Phone is now silent",
                        isSuccess = true
                    )
                }
            }
        } catch (e: Exception) {
            CommandResult(
                type = CommandType.TOGGLE_SILENT,
                rawInput = input,
                spokenReply = "Failed to change ringer mode: ${e.localizedMessage}",
                displayTitle = "Silent Mode Error",
                displayDescription = e.localizedMessage ?: "Unknown error",
                isSuccess = false
            )
        }
    }
}
