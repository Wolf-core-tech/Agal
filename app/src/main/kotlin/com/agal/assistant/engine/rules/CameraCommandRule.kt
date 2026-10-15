package com.agal.assistant.engine.rules

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.SystemClock
import android.provider.MediaStore
import android.view.KeyEvent
import com.agal.assistant.engine.CommandRule
import com.agal.assistant.engine.models.CommandResult
import com.agal.assistant.engine.models.CommandType

class CameraCommandRule : CommandRule {
    override val name: String = "Camera Command"

    companion object {
        // Shared recording state — so pause/stop can reference it
        var isRecording: Boolean = false
        var isPaused: Boolean = false
    }

    override fun matches(input: String): Boolean {
        val lower = input.lowercase().trim()

        val isPhoto = lower.contains("take a picture") ||
                lower.contains("take picture") ||
                lower.contains("take a photo") ||
                lower.contains("take photo") ||
                lower.contains("take pic") ||
                lower.contains("capture photo") ||
                lower.contains("capture picture") ||
                lower.contains("snap a photo") ||
                lower.contains("click a photo") ||
                lower.contains("click photo")

        val isVideo = lower.contains("take video") ||
                lower.contains("take a video") ||
                lower.contains("record video") ||
                lower.contains("record a video") ||
                lower.contains("start video") ||
                lower.contains("video mode")

        val isRecordCtrl = lower.contains("start recording") ||
                lower.contains("begin recording") ||
                lower.contains("pause recording") ||
                lower.contains("resume recording") ||
                lower.contains("stop recording") ||
                lower.contains("end recording") ||
                lower.contains("finish recording")

        val isOpenCam = lower.contains("open camera") ||
                lower.contains("launch camera") ||
                lower.contains("camera on") ||
                lower.contains("start camera")

        return isPhoto || isVideo || isRecordCtrl || isOpenCam ||
                lower == "camera"
    }

    override suspend fun execute(input: String, context: Context): CommandResult {
        val lower = input.lowercase().trim()

        // --- Recording controls (dispatch media key events) ---
        when {
            lower.contains("pause recording") -> {
                isPaused = true
                dispatchMediaKey(context, KeyEvent.KEYCODE_MEDIA_PAUSE)
                return CommandResult(
                    type = CommandType.CAMERA_RECORDING,
                    rawInput = input,
                    spokenReply = "Recording paused.",
                    displayTitle = "Recording Paused",
                    displayDescription = "Video recording paused",
                    isSuccess = true
                )
            }
            lower.contains("resume recording") -> {
                isPaused = false
                dispatchMediaKey(context, KeyEvent.KEYCODE_MEDIA_PLAY)
                return CommandResult(
                    type = CommandType.CAMERA_RECORDING,
                    rawInput = input,
                    spokenReply = "Recording resumed.",
                    displayTitle = "Recording Resumed",
                    displayDescription = "Video recording resumed",
                    isSuccess = true
                )
            }
            lower.contains("stop recording") || lower.contains("end recording") || lower.contains("finish recording") -> {
                isRecording = false
                isPaused = false
                dispatchMediaKey(context, KeyEvent.KEYCODE_MEDIA_STOP)
                return CommandResult(
                    type = CommandType.CAMERA_RECORDING,
                    rawInput = input,
                    spokenReply = "Recording stopped.",
                    displayTitle = "Recording Stopped",
                    displayDescription = "Video recording ended",
                    isSuccess = true
                )
            }
        }

        // --- Photo mode ---
        if (lower.contains("take a picture") ||
            lower.contains("take picture") ||
            lower.contains("take a photo") ||
            lower.contains("take photo") ||
            lower.contains("take pic") ||
            lower.contains("capture photo") ||
            lower.contains("capture picture") ||
            lower.contains("snap a photo") ||
            lower.contains("click a photo") ||
            lower.contains("click photo")
        ) {
            return try {
                // STILL_IMAGE_CAMERA opens camera ready to take a photo
                val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                CommandResult(
                    type = CommandType.CAMERA_PHOTO,
                    rawInput = input,
                    spokenReply = "Camera opened. Ready to take a photo.",
                    displayTitle = "Camera – Photo Mode",
                    displayDescription = "Camera launched in photo mode",
                    isSuccess = true
                )
            } catch (e: Exception) {
                try {
                    val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    CommandResult(
                        type = CommandType.CAMERA_PHOTO,
                        rawInput = input,
                        spokenReply = "Camera opened for photo capture.",
                        displayTitle = "Camera – Photo",
                        displayDescription = "Image capture intent launched",
                        isSuccess = true
                    )
                } catch (err: Exception) {
                    CommandResult(
                        type = CommandType.CAMERA_PHOTO,
                        rawInput = input,
                        spokenReply = "Could not open camera: ${err.localizedMessage}",
                        displayTitle = "Camera Error",
                        displayDescription = err.localizedMessage ?: "Unknown error",
                        isSuccess = false
                    )
                }
            }
        }

        // --- Video / Start Recording mode ---
        if (lower.contains("take video") ||
            lower.contains("take a video") ||
            lower.contains("record video") ||
            lower.contains("record a video") ||
            lower.contains("start video") ||
            lower.contains("start recording") ||
            lower.contains("begin recording") ||
            lower.contains("video mode")
        ) {
            isRecording = true
            isPaused = false
            return try {
                val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                CommandResult(
                    type = CommandType.CAMERA_VIDEO,
                    rawInput = input,
                    spokenReply = "Camera opened in video mode. Ready to record.",
                    displayTitle = "Camera – Video Mode",
                    displayDescription = "Video capture intent launched",
                    isSuccess = true
                )
            } catch (e: Exception) {
                CommandResult(
                    type = CommandType.CAMERA_VIDEO,
                    rawInput = input,
                    spokenReply = "Could not open video mode: ${e.localizedMessage}",
                    displayTitle = "Video Error",
                    displayDescription = e.localizedMessage ?: "Unknown error",
                    isSuccess = false
                )
            }
        }

        // --- Generic open camera ---
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
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                CommandResult(
                    type = CommandType.OPEN_CAMERA,
                    rawInput = input,
                    spokenReply = "Opening Camera.",
                    displayTitle = "Camera",
                    displayDescription = "Camera launched",
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

    private fun dispatchMediaKey(context: Context, keyCode: Int) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            val downTime = SystemClock.uptimeMillis()
            audioManager?.dispatchMediaKeyEvent(
                KeyEvent(downTime, downTime, KeyEvent.ACTION_DOWN, keyCode, 0)
            )
            audioManager?.dispatchMediaKeyEvent(
                KeyEvent(downTime, SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, keyCode, 0)
            )
        } catch (_: Exception) {}
    }
}
