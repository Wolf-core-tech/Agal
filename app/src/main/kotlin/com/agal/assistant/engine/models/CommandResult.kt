package com.agal.assistant.engine.models

enum class CommandType {
    OPEN_YOUTUBE,
    OPEN_BLUETOOTH,
    TOGGLE_FLASHLIGHT,
    TOGGLE_WIFI,
    TOGGLE_DND,
    TOGGLE_SILENT,
    TOGGLE_AIRPLANE,
    TOGGLE_HOTSPOT,
    CALL_ACTION,
    DIAL_CONTACT,
    CLEAR_NOTIFICATIONS,
    CLOSE_ALL_APPS,
    CAMERA_PHOTO,
    CAMERA_VIDEO,
    CAMERA_RECORDING,
    OPEN_CAMERA,
    OPEN_MAPS,
    NAVIGATE_MAPS,
    OPEN_APP,
    SET_REMINDER,
    BUCKET_LIST,
    TRIP_PLANNER,
    FINANCE_TRACKER,
    OPEN_SETTINGS,
    GET_TIME,
    GET_DATE,
    GET_BATTERY,
    VOLUME_CONTROL,
    PLAY_MUSIC,
    MEDIA_CONTROL,
    TELL_JOKE,
    ASSISTANT_INFO,
    HELP,
    LOCAL_AI_QUERY,
    CUSTOM_COMMAND,
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
