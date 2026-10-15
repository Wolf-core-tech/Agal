package com.agal.assistant.engine.models

enum class CommandType {
    OPEN_YOUTUBE,
    OPEN_BLUETOOTH,
    TOGGLE_FLASHLIGHT,
    OPEN_CAMERA,
    OPEN_SETTINGS,
    GET_TIME,
    GET_DATE,
    GET_BATTERY,
    VOLUME_CONTROL,
    TELL_JOKE,
    ASSISTANT_INFO,
    HELP,
    LOCAL_AI_QUERY,
    UNKNOWN
}

data class CommandResult(
    val type: CommandType,
    val rawInput: String,
    val spokenReply: String,
    val displayTitle: String,
    val displayDescription: String,
    val isSuccess: Boolean = true
)
