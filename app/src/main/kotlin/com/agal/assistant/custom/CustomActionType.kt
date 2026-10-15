package com.agal.assistant.custom

enum class CustomActionType(val displayName: String, val iconName: String) {
    SPEAK_REPLY("Speak Voice Reply", "RecordVoiceOver"),
    TOGGLE_FLASHLIGHT("Toggle Flashlight", "Highlight"),
    OPEN_APP("Launch App", "Apps"),
    SET_VOLUME("Set Volume", "VolumeUp"),
    FIND_MY_PHONE("Vibrate & Alert", "NotificationsActive"),
    BLUETOOTH_ACTION("Bluetooth Action", "Bluetooth")
}
