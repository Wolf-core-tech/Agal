package com.agal.assistant.engine.rules

import android.content.Context
import android.media.AudioManager
import android.os.SystemClock
import android.telephony.TelephonyManager
import android.view.KeyEvent
import com.agal.assistant.engine.CommandRule
import com.agal.assistant.engine.models.CommandResult
import com.agal.assistant.engine.models.CommandType

class CallCommandRule : CommandRule {
    override val name: String = "Call Action Command"

    override fun matches(input: String): Boolean {
        val lower = input.lowercase().trim()
        // Attend/Answer (active ringing call)
        val isAttend = lower.contains("attend the call") ||
                lower.contains("attend call") ||
                lower.contains("answer the call") ||
                lower.contains("answer call") ||
                lower.contains("accept call") ||
                lower.contains("pick up call") ||
                lower.contains("pick up the call") ||
                lower.contains("receive call") ||
                lower.contains("take the call")

        // Hang up / End / Cut (active call)
        val isHangup = lower.contains("hang up") ||
                lower.contains("hangup") ||
                lower.contains("end call") ||
                lower.contains("cut the call") ||
                lower.contains("cut call") ||
                lower.contains("reject call") ||
                lower.contains("decline call") ||
                lower.contains("disconnect call") ||
                lower.contains("drop call") ||
                lower.contains("stop the call")

        // Do NOT match "call [name]" patterns — those go to DialContactCommandRule
        val isDialIntent = lower.startsWith("call ") && !isAttend && !isHangup

        return (isAttend || isHangup) && !isDialIntent
    }

    override suspend fun execute(input: String, context: Context): CommandResult {
        val lower = input.lowercase().trim()

        val isHangup = lower.contains("hang up") ||
                lower.contains("hangup") ||
                lower.contains("end call") ||
                lower.contains("cut the call") ||
                lower.contains("cut call") ||
                lower.contains("reject call") ||
                lower.contains("decline call") ||
                lower.contains("disconnect call") ||
                lower.contains("drop call") ||
                lower.contains("stop the call")

        return try {
            if (isHangup) {
                // Hang up / End call
                dispatchHeadsetHookEvent(context)
                // Also use AudioManager to end the call
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                val downTime = SystemClock.uptimeMillis()
                val endKeyEvent = KeyEvent(downTime, downTime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENDCALL, 0)
                audioManager?.dispatchMediaKeyEvent(endKeyEvent)
                audioManager?.dispatchMediaKeyEvent(
                    KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENDCALL, 0)
                )

                CommandResult(
                    type = CommandType.CALL_ACTION,
                    rawInput = input,
                    spokenReply = "Hanging up the call.",
                    displayTitle = "Call Ended",
                    displayDescription = "Call disconnected",
                    isSuccess = true
                )
            } else {
                // Answer / Attend call
                dispatchHeadsetHookEvent(context)

                // For Android 8+, use KEYCODE_CALL to answer
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                val downTime = SystemClock.uptimeMillis()
                val callKeyEvent = KeyEvent(downTime, downTime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_CALL, 0)
                audioManager?.dispatchMediaKeyEvent(callKeyEvent)
                audioManager?.dispatchMediaKeyEvent(
                    KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, KeyEvent.KEYCODE_CALL, 0)
                )

                CommandResult(
                    type = CommandType.CALL_ACTION,
                    rawInput = input,
                    spokenReply = "Attending the call.",
                    displayTitle = "Call Answered",
                    displayDescription = "Call accepted",
                    isSuccess = true
                )
            }
        } catch (e: Exception) {
            CommandResult(
                type = CommandType.CALL_ACTION,
                rawInput = input,
                spokenReply = "Could not perform call action: ${e.localizedMessage}",
                displayTitle = "Call Action Error",
                displayDescription = e.localizedMessage ?: "Unknown error",
                isSuccess = false
            )
        }
    }

    private fun dispatchHeadsetHookEvent(context: Context) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            val downTime = SystemClock.uptimeMillis()
            val hookDown = KeyEvent(downTime, downTime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_HEADSETHOOK, 0)
            val hookUp = KeyEvent(downTime, SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, KeyEvent.KEYCODE_HEADSETHOOK, 0)
            audioManager?.dispatchMediaKeyEvent(hookDown)
            audioManager?.dispatchMediaKeyEvent(hookUp)
        } catch (_: Exception) {}
    }
}
