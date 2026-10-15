package com.agal.assistant.engine.rules

import android.content.Context
import com.agal.assistant.engine.CommandRule
import com.agal.assistant.engine.models.CommandResult
import com.agal.assistant.engine.models.CommandType
import com.agal.assistant.reminder.ReminderManager
import java.text.SimpleDateFormat
import java.util.*

class ReminderCommandRule : CommandRule {
    override val name: String = "Reminder Command"

    override fun matches(input: String): Boolean {
        val lower = input.lowercase().trim()
        return lower.startsWith("remind me") ||
                lower.startsWith("remember me") ||
                lower.startsWith("set a reminder") ||
                lower.startsWith("set reminder") ||
                lower.contains("remind me to") ||
                lower.contains("remember me to") ||
                lower.contains("remind me about") ||
                lower.contains("remember me about")
    }

    override suspend fun execute(input: String, context: Context): CommandResult {
        val lower = input.lowercase().trim()
        val reminderManager = ReminderManager(context)

        // Parse task and delay/time
        val (task, triggerMillis) = parseReminder(lower)

        val cleanTask = if (task.isNotBlank()) task else "do your scheduled task"
        val reminder = reminderManager.scheduleReminder(cleanTask, triggerMillis)

        val timeDiffSeconds = ((triggerMillis - System.currentTimeMillis()) / 1000).toInt()
        val timeDescription = formatTimeDescription(timeDiffSeconds, triggerMillis)

        val reply = "Got it! I will remind you to $cleanTask $timeDescription."

        return CommandResult(
            type = CommandType.SET_REMINDER,
            rawInput = input,
            spokenReply = reply,
            displayTitle = "⏰ Reminder Set",
            displayDescription = "Task: $cleanTask ($timeDescription)",
            isSuccess = true
        )
    }

    private fun parseReminder(input: String): Pair<String, Long> {
        var text = input
            .replace(Regex("^(?:hey|hello|hi)?\\s*agal[,\\s]*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^(?:please\\s+)?(?:remind|remember)\\s+me\\s+(?:to|about)?\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^(?:set\\s+a\\s+reminder|set\\s+reminder)\\s+(?:to|for)?\\s*", RegexOption.IGNORE_CASE), "")
            .trim()

        val now = System.currentTimeMillis()
        var delayMillis = 10 * 60 * 1000L // Default 10 minutes if no time specified

        // 1. Check relative duration: "in X minutes / seconds / hours"
        val inTimeRegex = Regex("(.*?)\\s+(?:in|after)\\s+(\\d+)\\s*(seconds?|secs?|minutes?|mins?|hours?|hrs?)$", RegexOption.IGNORE_CASE)
        val inMatch = inTimeRegex.find(text)
        if (inMatch != null) {
            val taskPart = inMatch.groups[1]?.value?.trim().orEmpty()
            val num = inMatch.groups[2]?.value?.toLongOrNull() ?: 5L
            val unit = inMatch.groups[3]?.value?.lowercase().orEmpty()

            val multiplier = when {
                unit.startsWith("sec") -> 1000L
                unit.startsWith("hour") || unit.startsWith("hr") -> 3600 * 1000L
                else -> 60 * 1000L // minutes
            }
            delayMillis = num * multiplier
            return Pair(taskPart, now + delayMillis)
        }

        // 2. Check time of day: "at 5 pm", "at 10:30 am", "at 8 o'clock"
        val atTimeRegex = Regex("(.*?)\\s+at\\s+(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?$", RegexOption.IGNORE_CASE)
        val atMatch = atTimeRegex.find(text)
        if (atMatch != null) {
            val taskPart = atMatch.groups[1]?.value?.trim().orEmpty()
            var hour = atMatch.groups[2]?.value?.toIntOrNull() ?: 12
            val min = atMatch.groups[3]?.value?.toIntOrNull() ?: 0
            val ampm = atMatch.groups[4]?.value?.lowercase()

            if (ampm == "pm" && hour < 12) hour += 12
            if (ampm == "am" && hour == 12) hour = 0

            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, min)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            // If the time already passed today, set for tomorrow
            if (cal.timeInMillis <= now) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            return Pair(taskPart, cal.timeInMillis)
        }

        // 3. Fallback: if user simply says "remember me to call mom", default to 10 minutes
        return Pair(text, now + delayMillis)
    }

    private fun formatTimeDescription(seconds: Int, targetMillis: Long): String {
        return when {
            seconds < 60 -> "in $seconds seconds"
            seconds < 3600 -> "in ${seconds / 60} minute" + if (seconds / 60 > 1) "s" else ""
            seconds < 86400 -> "at " + SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(targetMillis))
            else -> "on " + SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(Date(targetMillis))
        }
    }
}
