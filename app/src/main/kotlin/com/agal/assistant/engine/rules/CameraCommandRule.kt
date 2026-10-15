package com.agal.assistant.engine.rules

import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import com.agal.assistant.engine.CommandRule
import com.agal.assistant.engine.models.CommandResult
import com.agal.assistant.engine.models.CommandType

class CameraCommandRule : CommandRule {
    override val name: String = "Camera Command"

    override fun matches(input: String): Boolean {
        val lower = input.lowercase().trim()
        return lower.contains("camera") ||
                lower.contains("take a picture") ||
                lower.contains("take a photo") ||
                lower.contains("take picture") ||
                lower.contains("take photo")
    }

    override suspend fun execute(input: String, context: Context): CommandResult {
        return try {
            val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)

            CommandResult(
                type = CommandType.OPEN_CAMERA,
                rawInput = input,
                spokenReply = "Opening Camera.",
                displayTitle = "Camera",
                displayDescription = "Camera app launched",
                isSuccess = true
            )
        } catch (e: Exception) {
            try {
                val fallbackIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
                CommandResult(
                    type = CommandType.OPEN_CAMERA,
                    rawInput = input,
                    spokenReply = "Opening Camera.",
                    displayTitle = "Camera",
                    displayDescription = "Capture intent launched",
                    isSuccess = true
                )
            } catch (err: Exception) {
                CommandResult(
                    type = CommandType.OPEN_CAMERA,
                    rawInput = input,
                    spokenReply = "Could not open Camera: ${err.localizedMessage}",
                    displayTitle = "Camera Error",
                    displayDescription = err.localizedMessage ?: "Unknown error",
                    isSuccess = false
                )
            }
        }
    }
}
