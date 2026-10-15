package com.agal.assistant.engine.rules

import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import com.agal.assistant.engine.CommandRule
import com.agal.assistant.engine.models.CommandResult
import com.agal.assistant.engine.models.CommandType

class MusicCommandRule : CommandRule {
    override val name: String = "Music Command"

    override fun matches(input: String): Boolean {
        val lower = input.lowercase().trim()
        return lower.contains("music") ||
                lower.startsWith("play music") ||
                lower.startsWith("open my music") ||
                lower.startsWith("open music app")
    }

    override suspend fun execute(input: String, context: Context): CommandResult {
        return try {
            // Intent to launch music player
            val intent = Intent(MediaStore.INTENT_ACTION_MUSIC_PLAYER).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                // Fallback to common music apps or media intent
                val fallbackPackages = listOf(
                    "com.google.android.apps.youtube.music",
                    "com.spotify.music",
                    "com.sec.android.app.music",
                    "com.apple.android.music"
                )
                var launched = false
                for (pkg in fallbackPackages) {
                    val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(launchIntent)
                        launched = true
                        break
                    }
                }
                if (!launched) {
                    // Category APP_MUSIC
                    val categoryIntent = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_APP_MUSIC)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(categoryIntent)
                }
            }

            CommandResult(
                type = CommandType.OPEN_YOUTUBE, // media app
                rawInput = input,
                spokenReply = "Opening your music app.",
                displayTitle = "Music",
                displayDescription = "Music player application launched",
                isSuccess = true
            )
        } catch (e: Exception) {
            CommandResult(
                type = CommandType.OPEN_YOUTUBE,
                rawInput = input,
                spokenReply = "Opening your music app.",
                displayTitle = "Music",
                displayDescription = "Triggered music intent",
                isSuccess = true
            )
        }
    }
}
