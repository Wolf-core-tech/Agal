package com.agal.assistant.engine.rules

import android.content.Context
import android.media.AudioManager
import android.os.Build
import com.agal.assistant.engine.CommandRule
import com.agal.assistant.engine.models.CommandResult
import com.agal.assistant.engine.models.CommandType
import kotlin.math.roundToInt

class VolumeCommandRule : CommandRule {
    override val name: String = "Volume Control"

    override fun matches(input: String): Boolean {
        val lower = input.lowercase().trim()
        val hasVolumeWord = lower.contains("volume") || lower.contains("sound") || lower.contains("audio") || lower.contains("loudness")
        val isMute = lower.contains("mute") || lower.contains("unmute") || lower.contains("silence")
        val isFullOrMax = lower.contains("full volume") || lower.contains("max volume") || lower.contains("maximum volume")
        val hasPercentPattern = Regex("""\b\d+\s*%\s*(?:volume|sound)?\b""").containsMatchIn(lower) ||
                Regex("""\b(?:fifty|seventy\s*five|ninety|hundred)\s*percent\b""").containsMatchIn(lower) ||
                Regex("""\b(?:75%|90%|100%|50%|25%)\b""").containsMatchIn(lower)
        val hasPointAdjustment = (lower.contains("point") || lower.contains("step")) &&
                (lower.contains("increase") || lower.contains("decrease") || lower.contains("up") || lower.contains("down") || lower.contains("raise") || lower.contains("lower") || lower.contains("reduce"))
        val hasDirection = (lower.contains("up") || lower.contains("down") || lower.contains("increase") || lower.contains("decrease") || lower.contains("raise") || lower.contains("lower")) && hasVolumeWord

        return hasVolumeWord || isMute || isFullOrMax || hasPercentPattern || hasPointAdjustment || hasDirection
    }

    override suspend fun execute(input: String, context: Context): CommandResult {
        val lower = input.lowercase().trim()

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return CommandResult(
                type = CommandType.VOLUME_CONTROL,
                rawInput = input,
                spokenReply = "Audio manager service is not available on this device.",
                displayTitle = "Volume Error",
                displayDescription = "AudioManager service null",
                isSuccess = false
            )

        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val minVol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC)
        } else {
            0
        }
        val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val currentPercent = if (maxVol > minVol) {
            (((currentVol - minVol).toFloat() / (maxVol - minVol).toFloat()) * 100f).roundToInt()
        } else {
            0
        }

        val isDecrease = lower.contains("decrease") ||
                lower.contains("down") ||
                lower.contains("lower") ||
                lower.contains("reduce") ||
                lower.contains("drop") ||
                lower.contains("cut")

        val isIncrease = lower.contains("increase") ||
                lower.contains("up") ||
                lower.contains("raise") ||
                lower.contains("boost") ||
                lower.contains("higher")

        val points = extractPoints(lower)
        val percentage = extractPercentage(lower)

        var targetVol = currentVol
        var reply = ""
        var title = "Volume"
        var desc = ""

        try {
            when {
                // 1. Specific Point Adjustment (e.g. "one point", "two point", "three point")
                points != null -> {
                    targetVol = if (isDecrease) {
                        (currentVol - points).coerceIn(minVol, maxVol)
                    } else {
                        (currentVol + points).coerceIn(minVol, maxVol)
                    }
                    val resultingPercent = (((targetVol - minVol).toFloat() / (maxVol - minVol).toFloat()) * 100f).roundToInt()
                    val pointText = if (points == 1) "1 point" else "$points points"

                    if (isDecrease) {
                        reply = "Volume decreased by $pointText to $resultingPercent%."
                        title = "Volume Decreased"
                    } else {
                        reply = "Volume increased by $pointText to $resultingPercent%."
                        title = "Volume Increased"
                    }
                    desc = "Media volume set to level $targetVol of $maxVol ($resultingPercent%)"
                }

                // 2. Specific Percentage Target or Level (e.g. "50% volume", "75% volume", "90% volume", "100% volume", "full volume")
                percentage != null -> {
                    targetVol = when (percentage) {
                        0 -> minVol
                        100 -> maxVol
                        else -> {
                            val raw = minVol + (maxVol - minVol) * (percentage / 100.0f)
                            raw.roundToInt().coerceIn(minVol, maxVol)
                        }
                    }
                    val resultingPercent = (((targetVol - minVol).toFloat() / (maxVol - minVol).toFloat()) * 100f).roundToInt()

                    when {
                        percentage == 0 || targetVol == minVol -> {
                            reply = "Volume muted."
                            title = "Volume Muted"
                            desc = "Media volume muted (0%)"
                        }
                        percentage >= 100 || targetVol == maxVol -> {
                            reply = "Volume set to maximum."
                            title = "Full Volume"
                            desc = "Media volume set to maximum (100%)"
                        }
                        isDecrease -> {
                            reply = "Volume decreased to $percentage%."
                            title = "Volume Decreased"
                            desc = "Media volume lowered to $percentage% ($targetVol/$maxVol)"
                        }
                        isIncrease -> {
                            reply = "Volume increased to $percentage%."
                            title = "Volume Increased"
                            desc = "Media volume raised to $percentage% ($targetVol/$maxVol)"
                        }
                        else -> {
                            reply = "Volume set to $percentage%."
                            title = "Volume $percentage%"
                            desc = "Media volume set to $percentage% ($targetVol/$maxVol)"
                        }
                    }
                }

                // 3. Mute / Silence
                lower.contains("mute") || lower.contains("silence") || lower.contains("zero volume") -> {
                    targetVol = minVol
                    reply = "Volume muted."
                    title = "Volume Muted"
                    desc = "Media volume muted (0%)"
                }

                // 4. Unmute
                lower.contains("unmute") -> {
                    targetVol = (maxVol / 2).coerceAtLeast(minVol + 1)
                    val resultingPercent = (((targetVol - minVol).toFloat() / (maxVol - minVol).toFloat()) * 100f).roundToInt()
                    reply = "Volume unmuted to $resultingPercent%."
                    title = "Volume Unmuted"
                    desc = "Media volume restored to level $targetVol"
                }

                // 5. General volume down (no number given)
                isDecrease -> {
                    val step = 1
                    targetVol = (currentVol - step).coerceIn(minVol, maxVol)
                    val resultingPercent = (((targetVol - minVol).toFloat() / (maxVol - minVol).toFloat()) * 100f).roundToInt()
                    reply = "Volume decreased to $resultingPercent%."
                    title = "Volume Decreased"
                    desc = "Media volume lowered to $resultingPercent%"
                }

                // 6. General volume up (no number given)
                else -> {
                    val step = 1
                    targetVol = (currentVol + step).coerceIn(minVol, maxVol)
                    val resultingPercent = (((targetVol - minVol).toFloat() / (maxVol - minVol).toFloat()) * 100f).roundToInt()
                    reply = "Volume increased to $resultingPercent%."
                    title = "Volume Increased"
                    desc = "Media volume raised to $resultingPercent%"
                }
            }

            // Apply volume with visual UI slider flag
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, AudioManager.FLAG_SHOW_UI)

            return CommandResult(
                type = CommandType.VOLUME_CONTROL,
                rawInput = input,
                spokenReply = reply,
                displayTitle = title,
                displayDescription = desc,
                isSuccess = true
            )
        } catch (e: Exception) {
            return CommandResult(
                type = CommandType.VOLUME_CONTROL,
                rawInput = input,
                spokenReply = "Failed to adjust volume: ${e.localizedMessage}",
                displayTitle = "Volume Error",
                displayDescription = e.localizedMessage ?: "Unknown error",
                isSuccess = false
            )
        }
    }

    private fun extractPoints(text: String): Int? {
        val regex = Regex("""\b(?:by\s+)?(one|two|three|four|five|six|seven|eight|nine|ten|\d+)\s*(?:point|points|pt|pts|step|steps|notch|notches)\b""")
        val match = regex.find(text)
        if (match != null) {
            val numStr = match.groupValues[1]
            return parseWordNumber(numStr)
        }
        return null
    }

    private fun extractPercentage(text: String): Int? {
        // Explicit percentage: e.g. "50%", "75 %", "90 percent"
        val percentRegex = Regex("""(\d+)\s*(?:%|percent)""")
        val match = percentRegex.find(text)
        if (match != null) {
            return match.groupValues[1].toIntOrNull()?.coerceIn(0, 100)
        }

        // Patterns like "75% volume" or "90 volume", "100 volume", "50 volume"
        val numberNearVolume = Regex("""\b(\d+)\s*(?:volume|sound)\b""")
        val numMatch = numberNearVolume.find(text)
        if (numMatch != null) {
            val num = numMatch.groupValues[1].toIntOrNull()
            if (num != null && num > 10) {
                return num.coerceIn(0, 100)
            }
        }

        // Patterns like "volume 75", "volume 90", "volume 100", "volume to 50"
        val volNearNumber = Regex("""\b(?:volume|sound)\s*(?:to\s+)?(\d+)\b""")
        val volMatch = volNearNumber.find(text)
        if (volMatch != null) {
            val num = volMatch.groupValues[1].toIntOrNull()
            if (num != null && num > 10) {
                return num.coerceIn(0, 100)
            }
        }

        // Word percentages & keywords
        return when {
            text.contains("hundred percent") || text.contains("full volume") || text.contains("max volume") || text.contains("maximum volume") -> 100
            text.contains("ninety percent") || text.contains("90%") -> 90
            text.contains("seventy five percent") || text.contains("75%") -> 75
            text.contains("fifty percent") || text.contains("half volume") || text.contains("50%") -> 50
            text.contains("twenty five percent") || text.contains("quarter volume") || text.contains("25%") -> 25
            text.contains("zero percent") || text.contains("zero volume") || text.contains("minimum volume") || text.contains("min volume") -> 0
            else -> null
        }
    }

    private fun parseWordNumber(word: String): Int? {
        return when (word.lowercase().trim()) {
            "one", "1" -> 1
            "two", "2" -> 2
            "three", "3" -> 3
            "four", "4" -> 4
            "five", "5" -> 5
            "six", "6" -> 6
            "seven", "7" -> 7
            "eight", "8" -> 8
            "nine", "9" -> 9
            "ten", "10" -> 10
            else -> word.toIntOrNull()
        }
    }
}
