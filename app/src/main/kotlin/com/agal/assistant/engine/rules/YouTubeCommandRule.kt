package com.agal.assistant.engine.rules

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.agal.assistant.engine.CommandRule
import com.agal.assistant.engine.models.CommandResult
import com.agal.assistant.engine.models.CommandType

class YouTubeCommandRule : CommandRule {
    override val name: String = "YouTube Command"

    override fun matches(input: String): Boolean {
        val lower = input.lowercase().trim()
        return lower.contains("youtube") ||
                lower.startsWith("open youtube") ||
                lower.startsWith("play youtube") ||
                lower.startsWith("search youtube")
    }

    override suspend fun execute(input: String, context: Context): CommandResult {
        val lower = input.lowercase().trim()
        val queryRegex = Regex("(?:search youtube for|play on youtube|search on youtube)\\s+(.*)", RegexOption.IGNORE_CASE)
        val match = queryRegex.find(input)
        val query = match?.groups?.get(1)?.value?.trim()

        return try {
            val intent: Intent
            if (!query.isNullOrBlank()) {
                val encodedQuery = Uri.encode(query)
                val appUri = Uri.parse("vnd.youtube://results?q=$encodedQuery")
                intent = Intent(Intent.ACTION_VIEW, appUri).apply {
                    setPackage("com.google.android.youtube")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                val launchIntent = context.packageManager.getLaunchIntentForPackage("com.google.android.youtube")
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return CommandResult(
                        type = CommandType.OPEN_YOUTUBE,
                        rawInput = input,
                        spokenReply = "Opening YouTube.",
                        displayTitle = "YouTube",
                        displayDescription = "Launched native YouTube app",
                        isSuccess = true
                    )
                }
                intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                val webUri = if (!query.isNullOrBlank()) {
                    Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(query))
                } else {
                    Uri.parse("https://www.youtube.com")
                }
                context.startActivity(Intent(Intent.ACTION_VIEW, webUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }

            CommandResult(
                type = CommandType.OPEN_YOUTUBE,
                rawInput = input,
                spokenReply = if (!query.isNullOrBlank()) "Searching YouTube for $query." else "Opening YouTube.",
                displayTitle = "YouTube",
                displayDescription = if (!query.isNullOrBlank()) "Searching for \"$query\"" else "Opened YouTube",
                isSuccess = true
            )
        } catch (e: Exception) {
            CommandResult(
                type = CommandType.OPEN_YOUTUBE,
                rawInput = input,
                spokenReply = "Failed to launch YouTube: ${e.localizedMessage}",
                displayTitle = "YouTube Error",
                displayDescription = e.localizedMessage ?: "Unknown error",
                isSuccess = false
            )
        }
    }
}
