package com.agal.assistant.engine.rules

import android.content.Context
import com.agal.assistant.engine.CommandRule
import com.agal.assistant.engine.models.CommandResult
import com.agal.assistant.engine.models.CommandType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TimeDateRule : CommandRule {
    override val name: String = "Time & Date"

    override fun matches(input: String): Boolean {
        val lower = input.lowercase().trim()
        return lower.contains("time") || lower.contains("date") || lower.contains("day is it")
    }

    override suspend fun execute(input: String, context: Context): CommandResult {
        val lower = input.lowercase().trim()
        val now = Date()

        return if (lower.contains("date") || lower.contains("day is it")) {
            val dateStr = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(now)
            CommandResult(
                type = CommandType.GET_DATE,
                rawInput = input,
                spokenReply = "Today is $dateStr.",
                displayTitle = "Current Date",
                displayDescription = dateStr,
                isSuccess = true
            )
        } else {
            val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(now)
            CommandResult(
                type = CommandType.GET_TIME,
                rawInput = input,
                spokenReply = "It is $timeStr.",
                displayTitle = "Current Time",
                displayDescription = timeStr,
                isSuccess = true
            )
        }
    }
}
