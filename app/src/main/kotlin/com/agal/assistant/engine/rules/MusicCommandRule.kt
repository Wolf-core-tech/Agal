package com.agal.assistant.engine.rules

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.SystemClock
import android.provider.MediaStore
import android.view.KeyEvent
import com.agal.assistant.engine.CommandRule
import com.agal.assistant.engine.models.CommandResult
import com.agal.assistant.engine.models.CommandType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class MusicCommandRule : CommandRule {
    override val name: String = "Music & Media Command"

    override fun matches(input: String): Boolean {
        val lower = input.lowercase().trim()

        val isNext = lower.contains("next song") ||
                lower.contains("next track") ||
                lower.contains("play next") ||
                lower.contains("skip song") ||
                lower.contains("skip track") ||
                lower.contains("next music") ||
                lower == "next"

        val isPrev = lower.contains("previous song") ||
                lower.contains("prev song") ||
                lower.contains("previous track") ||
                lower.contains("prev track") ||
                lower.contains("play previous") ||
                lower.contains("last song") ||
                lower.contains("back song") ||
                lower.contains("replay song") ||
                lower == "previous"

        val isPause = (lower.contains("pause") || lower.contains("stop")) &&
                (lower.contains("music") || lower.contains("song") || lower.contains("track") || lower.contains("playback"))

        val isPlay = lower.contains("music") ||
                lower.contains("play song") ||
                lower.contains("play songs") ||
                lower.contains("play track") ||
                lower.contains("start music") ||
                lower.contains("open music") ||
                lower.contains("open my music") ||
                lower.contains("open music app") ||
                lower.contains("listen to music") ||
                lower.contains("resume song") ||
                lower.contains("resume music")

        return isNext || isPrev || isPause || isPlay
    }

    override suspend fun execute(input: String, context: Context): CommandResult {
        val lower = input.lowercase().trim()

        // 1. Play Next Song
        if (lower.contains("next song") ||
            lower.contains("next track") ||
            lower.contains("play next") ||
            lower.contains("skip song") ||
            lower.contains("skip track") ||
            lower.contains("next music") ||
            lower == "next"
        ) {
            dispatchMediaKeyEvent(context, KeyEvent.KEYCODE_MEDIA_NEXT)
            return CommandResult(
                type = CommandType.MEDIA_CONTROL,
                rawInput = input,
                spokenReply = "Playing next song.",
                displayTitle = "Next Song",
                displayDescription = "Skipped to next music track",
                isSuccess = true
            )
        }

        // 2. Play Previous Song
        if (lower.contains("previous song") ||
            lower.contains("prev song") ||
            lower.contains("previous track") ||
            lower.contains("prev track") ||
            lower.contains("play previous") ||
            lower.contains("last song") ||
            lower.contains("back song") ||
            lower.contains("replay song") ||
            lower == "previous"
        ) {
            dispatchMediaKeyEvent(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            return CommandResult(
                type = CommandType.MEDIA_CONTROL,
                rawInput = input,
                spokenReply = "Playing previous song.",
                displayTitle = "Previous Song",
                displayDescription = "Returned to previous music track",
                isSuccess = true
            )
        }

        // 3. Pause / Stop Song
        if ((lower.contains("pause") || lower.contains("stop")) &&
            (lower.contains("music") || lower.contains("song") || lower.contains("track") || lower.contains("playback"))
        ) {
            dispatchMediaKeyEvent(context, KeyEvent.KEYCODE_MEDIA_PAUSE)
            return CommandResult(
                type = CommandType.MEDIA_CONTROL,
                rawInput = input,
                spokenReply = "Music paused.",
                displayTitle = "Music Paused",
                displayDescription = "Playback paused",
                isSuccess = true
            )
        }

        // 4. Play Music: Opens music app and starts playback
        return try {
            val appOpened = launchMusicApp(context)

            // Dispatch MEDIA_PLAY immediately to wake active media session
            dispatchMediaKeyEvent(context, KeyEvent.KEYCODE_MEDIA_PLAY)

            // Also dispatch play event after small delay so launched music player catches play action
            withContext(Dispatchers.IO) {
                delay(650)
                dispatchMediaKeyEvent(context, KeyEvent.KEYCODE_MEDIA_PLAY)
            }

            CommandResult(
                type = CommandType.PLAY_MUSIC,
                rawInput = input,
                spokenReply = "Opening your music app and playing music.",
                displayTitle = "Play Music",
                displayDescription = if (appOpened) "Music app opened & playback started" else "Media playback started",
                isSuccess = true
            )
        } catch (e: Exception) {
            dispatchMediaKeyEvent(context, KeyEvent.KEYCODE_MEDIA_PLAY)
            CommandResult(
                type = CommandType.PLAY_MUSIC,
                rawInput = input,
                spokenReply = "Playing music.",
                displayTitle = "Play Music",
                displayDescription = e.localizedMessage ?: "Playback triggered",
                isSuccess = true
            )
        }
    }

    private fun launchMusicApp(context: Context): Boolean {
        val pm = context.packageManager

        // Known installed music apps (YouTube Music, Spotify, Samsung Music, etc.)
        val musicPackages = listOf(
            "com.google.android.apps.youtube.music",
            "com.spotify.music",
            "com.sec.android.app.music",
            "com.apple.android.music",
            "com.amazon.mp3",
            "com.jio.media.jiobeats",
            "com.bsbportal.music",
            "com.gaana",
            "com.soundcloud.android",
            "com.netease.cloudmusic",
            "com.maxmpz.audioplayer"
        )

        for (pkg in musicPackages) {
            val launchIntent = pm.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return true
            }
        }

        // Default system music player intent
        val musicPlayerIntent = Intent(MediaStore.INTENT_ACTION_MUSIC_PLAYER).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (musicPlayerIntent.resolveActivity(pm) != null) {
            context.startActivity(musicPlayerIntent)
            return true
        }

        // Category APP_MUSIC intent
        val categoryIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_MUSIC)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (categoryIntent.resolveActivity(pm) != null) {
            context.startActivity(categoryIntent)
            return true
        }

        return false
    }

    private fun dispatchMediaKeyEvent(context: Context, keyCode: Int) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val downTime = SystemClock.uptimeMillis()

        val downEvent = KeyEvent(downTime, downTime, KeyEvent.ACTION_DOWN, keyCode, 0)
        val upEvent = KeyEvent(downTime, SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, keyCode, 0)

        audioManager?.dispatchMediaKeyEvent(downEvent)
        audioManager?.dispatchMediaKeyEvent(upEvent)

        // Broadcast fallback for media button receivers
        try {
            val downIntent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                putExtra(Intent.EXTRA_KEY_EVENT, downEvent)
            }
            context.sendOrderedBroadcast(downIntent, null)

            val upIntent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                putExtra(Intent.EXTRA_KEY_EVENT, upEvent)
            }
            context.sendOrderedBroadcast(upIntent, null)
        } catch (_: Exception) {
        }
    }
}
