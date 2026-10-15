package com.agal.assistant.engine.rules

import android.content.Context
import android.os.BatteryManager
import com.agal.assistant.engine.CommandRule
import com.agal.assistant.engine.models.CommandResult
import com.agal.assistant.engine.models.CommandType

class BatteryRule : CommandRule {
    override val name: String = "Battery Rule"

    override fun matches(input: String): Boolean {
        val lower = input.lowercase().trim()
        return lower.contains("battery")
    }

    override suspend fun execute(input: String, context: Context): CommandResult {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val level = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1

        val reply = if (level >= 0) {
            "Your device battery is at $level percent."
        } else {
            "Battery status is currently unavailable."
        }

        return CommandResult(
            type = CommandType.GET_BATTERY,
            rawInput = input,
            spokenReply = reply,
            displayTitle = "Battery",
            displayDescription = if (level >= 0) "$level%" else "Unavailable",
            isSuccess = level >= 0
        )
    }
}
