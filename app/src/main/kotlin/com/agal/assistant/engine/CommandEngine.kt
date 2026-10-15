package com.agal.assistant.engine

import android.content.Context
import com.agal.assistant.ai.LocalAiBridge
import com.agal.assistant.engine.models.CommandResult
import com.agal.assistant.engine.models.CommandType
import com.agal.assistant.engine.rules.*

class CommandEngine(private val context: Context) {

    private val rules: List<CommandRule> = listOf(
        YouTubeCommandRule(),
        MusicCommandRule(),
        BluetoothCommandRule(),
        FlashlightCommandRule(),
        CameraCommandRule(),
        SystemSettingsRule(),
        TimeDateRule(),
        BatteryRule(),
        JokeRule()
    )

    private val localAi = LocalAiBridge(context)
    private val customCommandRepo = com.agal.assistant.custom.CustomCommandRepository(context)

    suspend fun execute(rawInput: String): CommandResult {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) {
            return CommandResult(
                type = CommandType.UNKNOWN,
                rawInput = rawInput,
                spokenReply = "I didn't hear a command. Try saying Open YouTube or Turn on Bluetooth.",
                displayTitle = "No Input",
                displayDescription = "Empty phrase received",
                isSuccess = false
            )
        }

        // 1. Check User-Defined Custom Commands & Routines First
        val matchedCustom = customCommandRepo.match(trimmed)
        if (matchedCustom != null) {
            val spokenReply = customCommandRepo.executeAction(matchedCustom)
            return CommandResult(
                type = CommandType.CUSTOM_COMMAND,
                rawInput = rawInput,
                spokenReply = spokenReply,
                displayTitle = "⚡ Custom: ${matchedCustom.title.ifBlank { matchedCustom.triggerPhrase }}",
                displayDescription = "Custom Action: ${matchedCustom.actionType.displayName}",
                isSuccess = true
            )
        }

        // 2. Match against registered Kotlin Rules
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
                spokenReply = "You can say: Open YouTube, Turn on Bluetooth, Turn on flashlight, Open camera, What time is it, or Battery status.",
                displayTitle = "Supported Commands",
                displayDescription = "YouTube • Bluetooth • Flashlight • Camera • Time • Battery",
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
