package com.agal.assistant.engine

import android.content.Context
import com.agal.assistant.ai.LocalAiBridge
import com.agal.assistant.engine.models.CommandResult
import com.agal.assistant.engine.models.CommandType
import com.agal.assistant.engine.rules.*

class CommandEngine(private val context: Context) {

    private val rules: List<CommandRule> = listOf(
        // Dial contact with SIM selection (first — handles SIM follow-up state)
        DialContactCommandRule(),
        // Call controls (answer/hangup active calls)
        CallCommandRule(),
        // Close all apps
        CloseAllAppsCommandRule(),
        // Clear notifications
        ClearNotificationsCommandRule(),
        // Volume & Music
        VolumeCommandRule(),
        MusicCommandRule(),
        // Hardware toggles
        WifiCommandRule(),
        DndSilentCommandRule(),
        AirplaneModeCommandRule(),
        HotspotCommandRule(),
        BluetoothCommandRule(),
        FlashlightCommandRule(),
        // Camera (handles photo, video, recording)
        CameraCommandRule(),
        // Maps & Navigation
        MapsCommandRule(),
        // Reminders & Alarms
        ReminderCommandRule(),
        // Lifestyle: Bucket List, Trip Destinations, Finance Tracking
        BucketListCommandRule(),
        TripCommandRule(),
        FinanceCommandRule(),
        // Apps & Info
        YouTubeCommandRule(),
        AppActionCommandRule(),
        SystemSettingsRule(),
        TimeDateRule(),
        BatteryRule(),
        JokeRule()
    )

    private val localAi = LocalAiBridge(context)

    suspend fun execute(rawInput: String): CommandResult {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) {
            return CommandResult(
                type = CommandType.UNKNOWN,
                rawInput = rawInput,
                spokenReply = "I didn't hear a command. Try saying Play music, Volume up, or Open YouTube.",
                displayTitle = "No Input",
                displayDescription = "Empty phrase received",
                isSuccess = false
            )
        }

        // Match against registered Kotlin Rules
        for (rule in rules) {
            if (rule.matches(trimmed)) {
                return rule.execute(trimmed, context)
            }
        }

        // Help check
        val lower = trimmed.lowercase()
        if (lower.contains("help") || lower.contains("what can you do") || lower.contains("commands")) {
            return CommandResult(
                type = CommandType.HELP,
                rawInput = rawInput,
                spokenReply = "You can say: Call mom, Close all apps, Open map, Navigate to Chennai, Play music, Volume up, Turn on WiFi, Silent mode, Take a photo, and more.",
                displayTitle = "Supported Commands",
                displayDescription = "Call • Close All • Maps • Music • Volume • WiFi • BT • DND • Camera • Photo • Video",
                isSuccess = true
            )
        }

        // Fallback to on-device Local AI
        val aiReply = localAi.generateResponse(trimmed)
        return CommandResult(
            type = CommandType.LOCAL_AI_QUERY,
            rawInput = rawInput,
            spokenReply = aiReply,
            displayTitle = "Local AI Reply",
            displayDescription = "Processed by on-device model (Zero Cloud)",
            isSuccess = true
        )
    }
}
