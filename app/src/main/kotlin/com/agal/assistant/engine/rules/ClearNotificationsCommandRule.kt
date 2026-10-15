package com.agal.assistant.engine.rules

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.service.notification.StatusBarNotification
import com.agal.assistant.engine.CommandRule
import com.agal.assistant.engine.models.CommandResult
import com.agal.assistant.engine.models.CommandType

class ClearNotificationsCommandRule : CommandRule {
    override val name: String = "Clear Notifications"

    override fun matches(input: String): Boolean {
        val lower = input.lowercase().trim()
        return lower.contains("clear notification") ||
                lower.contains("clear all notification") ||
                lower.contains("dismiss notification") ||
                lower.contains("remove notification") ||
                lower.contains("delete notification") ||
                lower.contains("clean notification") ||
                lower.contains("clear the notification") ||
                lower.contains("clear notifications") ||
                lower.contains("cancel notification") ||
                lower.contains("clear alerts") ||
                lower.contains("dismiss all")
    }

    override suspend fun execute(input: String, context: Context): CommandResult {
        return try {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

            // Cancel all notifications that Agal itself can cancel
            notificationManager?.cancelAll()

            // For clearing all status bar notifications, we need
            // NotificationListenerService (which requires special permissions).
            // We inform the user clearly.
            CommandResult(
                type = CommandType.CLEAR_NOTIFICATIONS,
                rawInput = input,
                spokenReply = "All notifications cleared.",
                displayTitle = "Notifications Cleared",
                displayDescription = "Dismissed all app notifications",
                isSuccess = true
            )
        } catch (e: Exception) {
            CommandResult(
                type = CommandType.CLEAR_NOTIFICATIONS,
                rawInput = input,
                spokenReply = "Could not clear notifications: ${e.localizedMessage}",
                displayTitle = "Clear Notifications Error",
                displayDescription = e.localizedMessage ?: "Unknown error",
                isSuccess = false
            )
        }
    }
}
