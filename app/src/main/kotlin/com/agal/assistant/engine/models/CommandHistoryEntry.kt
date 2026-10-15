package com.agal.assistant.engine.models

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CommandHistoryEntry(
    val id: String = System.currentTimeMillis().toString(),
    val userInput: String,
    val assistantReply: String,
    val commandType: CommandType,
    val timestamp: Long = System.currentTimeMillis(),
    val isOffline: Boolean = true,
    val isSuccess: Boolean = true
) {
    val formattedTime: String
        get() = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
}
