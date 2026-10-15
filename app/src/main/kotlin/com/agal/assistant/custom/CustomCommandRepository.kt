package com.agal.assistant.custom

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

class CustomCommandRepository(private val context: Context) {

    private val TAG = "CustomCommandRepo"
    private val PREF_NAME = "agal_custom_commands_pref"
    private val KEY_COMMANDS = "custom_commands_json"

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun getCommands(): List<CustomVoiceCommand> {
        val jsonStr = prefs.getString(KEY_COMMANDS, null)
        if (jsonStr.isNullOrEmpty()) {
            val defaults = getDefaultCommands()
            saveCommands(defaults)
            return defaults
        }

        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<CustomVoiceCommand>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(CustomVoiceCommand.fromJson(obj))
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing custom commands: ${e.localizedMessage}")
            getDefaultCommands()
        }
    }

    fun saveCommands(commands: List<CustomVoiceCommand>) {
        try {
            val array = JSONArray()
            for (cmd in commands) {
                array.put(cmd.toJson())
            }
            prefs.edit().putString(KEY_COMMANDS, array.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving custom commands: ${e.localizedMessage}")
        }
    }

    fun addCommand(command: CustomVoiceCommand): List<CustomVoiceCommand> {
        val current = getCommands().toMutableList()
        current.add(0, command)
        saveCommands(current)
        return current
    }

    fun updateCommand(command: CustomVoiceCommand): List<CustomVoiceCommand> {
        val current = getCommands().map {
            if (it.id == command.id) command else it
        }
        saveCommands(current)
        return current
    }

    fun deleteCommand(id: String): List<CustomVoiceCommand> {
        val current = getCommands().filterNot { it.id == id }
        saveCommands(current)
        return current
    }

    fun toggleCommand(id: String, isEnabled: Boolean): List<CustomVoiceCommand> {
        val current = getCommands().map {
            if (it.id == id) it.copy(isEnabled = isEnabled) else it
        }
        saveCommands(current)
        return current
    }

    /**
     * Match a spoken input against registered custom triggers
     */
    fun match(rawInput: String): CustomVoiceCommand? {
        val clean = rawInput.lowercase().trim()
            .replace(Regex("[,.!?]"), "")
            .replace("hey agal", "")
            .replace("agal", "")
            .trim()

        val commands = getCommands().filter { it.isEnabled }

        for (cmd in commands) {
            val trigger = cmd.triggerPhrase.lowercase().trim()
                .replace(Regex("[,.!?]"), "")

            if (trigger.isEmpty()) continue

            if (clean == trigger || clean.contains(trigger) || rawInput.lowercase().contains(trigger)) {
                return cmd
            }
        }
        return null
    }

    /**
     * Executes the hardware/system action for a custom command and returns the reply text
     */
    fun executeAction(cmd: CustomVoiceCommand): String {
        Log.i(TAG, "Executing custom action: ${cmd.actionType} for trigger '${cmd.triggerPhrase}'")

        when (cmd.actionType) {
            CustomActionType.TOGGLE_FLASHLIGHT -> {
                val turnOn = cmd.actionPayload.equals("OFF", ignoreCase = true).not()
                toggleFlashlight(turnOn)
            }
            CustomActionType.SET_VOLUME -> {
                when (cmd.actionPayload.uppercase()) {
                    "MUTE" -> setVolumePercent(0)
                    "MAX" -> setVolumePercent(100)
                    "HALF" -> setVolumePercent(50)
                    else -> setVolumePercent(50)
                }
            }
            CustomActionType.OPEN_APP -> {
                openApp(cmd.actionPayload)
            }
            CustomActionType.FIND_MY_PHONE -> {
                triggerPhoneAlert()
            }
            CustomActionType.BLUETOOTH_ACTION -> {
                try {
                    val intent = Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Bluetooth settings error: ${e.localizedMessage}")
                }
            }
            CustomActionType.SPEAK_REPLY -> {
                // Voice reply only
            }
        }

        return if (cmd.spokenReply.isNotBlank()) {
            cmd.spokenReply
        } else {
            "Custom command ${cmd.title.ifBlank { cmd.triggerPhrase }} executed."
        }
    }

    private fun toggleFlashlight(on: Boolean) {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return
            cameraManager.setTorchMode(cameraId, on)
        } catch (e: Exception) {
            Log.e(TAG, "Flashlight error: ${e.localizedMessage}")
        }
    }

    private fun setVolumePercent(percent: Int) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val target = (max * (percent / 100.0)).toInt()
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, AudioManager.FLAG_SHOW_UI)
        } catch (e: Exception) {
            Log.e(TAG, "Volume error: ${e.localizedMessage}")
        }
    }

    private fun openApp(payload: String) {
        try {
            when (payload.lowercase().trim()) {
                "youtube" -> {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:")).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }
                "music" -> {
                    val intent = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_APP_MUSIC)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }
                "camera" -> {
                    val intent = Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }
                "whatsapp" -> {
                    val launchIntent = context.packageManager.getLaunchIntentForPackage("com.whatsapp")
                    if (launchIntent != null) {
                        launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(launchIntent)
                    }
                }
                else -> {
                    val pkg = payload.trim()
                    val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)
                    if (launchIntent != null) {
                        launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(launchIntent)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Open app error: ${e.localizedMessage}")
        }
    }

    private fun triggerPhoneAlert() {
        try {
            val vibrator = context.getSystemService(Vibrator::class.java)
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val timings = longArrayOf(0, 300, 200, 500, 200, 700)
                    val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(1000)
                }
            }
            // Also bump volume up so reply can be heard across the room
            setVolumePercent(100)
        } catch (e: Exception) {
            Log.e(TAG, "Phone alert error: ${e.localizedMessage}")
        }
    }

    private fun getDefaultCommands(): List<CustomVoiceCommand> {
        return listOf(
            CustomVoiceCommand(
                triggerPhrase = "party time",
                title = "Party Mode",
                actionType = CustomActionType.TOGGLE_FLASHLIGHT,
                actionPayload = "ON",
                spokenReply = "Party mode activated! Let's celebrate!"
            ),
            CustomVoiceCommand(
                triggerPhrase = "where are you",
                title = "Find My Phone",
                actionType = CustomActionType.FIND_MY_PHONE,
                actionPayload = "ALERT",
                spokenReply = "I am right here! Agal is listening."
            ),
            CustomVoiceCommand(
                triggerPhrase = "good night",
                title = "Good Night Routine",
                actionType = CustomActionType.SET_VOLUME,
                actionPayload = "MUTE",
                spokenReply = "Good night Saran, muting media and sweet dreams."
            ),
            CustomVoiceCommand(
                triggerPhrase = "study time",
                title = "Study Mode",
                actionType = CustomActionType.SET_VOLUME,
                actionPayload = "MUTE",
                spokenReply = "Distraction-free focus mode enabled. Happy studying!"
            )
        )
    }
}
