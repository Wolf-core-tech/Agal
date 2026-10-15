package com.agal.assistant.engine.rules

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import com.agal.assistant.engine.CommandRule
import com.agal.assistant.engine.models.CommandResult
import com.agal.assistant.engine.models.CommandType

class FlashlightCommandRule : CommandRule {
    override val name: String = "Flashlight Command"
    private var isTorchOn = false

    override fun matches(input: String): Boolean {
        val lower = input.lowercase().trim()
        return lower.contains("flashlight") ||
                lower.contains("torch") ||
                lower.contains("turn on light") ||
                lower.contains("turn off light")
    }

    override suspend fun execute(input: String, context: Context): CommandResult {
        val lower = input.lowercase().trim()
        val turnOff = lower.contains("off") || lower.contains("disable")
        val targetState = !turnOff

        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            if (cameraManager == null) {
                return CommandResult(
                    type = CommandType.TOGGLE_FLASHLIGHT,
                    rawInput = input,
                    spokenReply = "Camera hardware service unavailable.",
                    displayTitle = "Flashlight",
                    displayDescription = "Camera service null",
                    isSuccess = false
                )
            }

            var torchCameraId: String? = null
            for (id in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    torchCameraId = id
                    break
                }
            }

            if (torchCameraId == null && cameraManager.cameraIdList.isNotEmpty()) {
                torchCameraId = cameraManager.cameraIdList[0]
            }

            if (torchCameraId != null) {
                cameraManager.setTorchMode(torchCameraId, targetState)
                isTorchOn = targetState
                CommandResult(
                    type = CommandType.TOGGLE_FLASHLIGHT,
                    rawInput = input,
                    spokenReply = if (targetState) "Flashlight turned on." else "Flashlight turned off.",
                    displayTitle = "Flashlight",
                    displayDescription = if (targetState) "LED torch activated" else "LED torch deactivated",
                    isSuccess = true
                )
            } else {
                CommandResult(
                    type = CommandType.TOGGLE_FLASHLIGHT,
                    rawInput = input,
                    spokenReply = "No camera with flash found on this device.",
                    displayTitle = "Flashlight",
                    displayDescription = "No flash unit available",
                    isSuccess = false
                )
            }
        } catch (e: Exception) {
            CommandResult(
                type = CommandType.TOGGLE_FLASHLIGHT,
                rawInput = input,
                spokenReply = "Failed to toggle flashlight: ${e.localizedMessage}",
                displayTitle = "Flashlight Error",
                displayDescription = e.localizedMessage ?: "Hardware exception",
                isSuccess = false
            )
        }
    }
}
