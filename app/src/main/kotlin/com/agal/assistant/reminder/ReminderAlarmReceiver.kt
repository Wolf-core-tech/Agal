package com.agal.assistant.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.core.app.NotificationCompat
import com.agal.assistant.MainActivity
import com.agal.assistant.R

class ReminderAlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ReminderAlarmReceiver"
        const val CHANNEL_ID = "agal_reminders_channel"
        const val EXTRA_TASK_TEXT = "extra_reminder_task"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskText = intent.getStringExtra(EXTRA_TASK_TEXT) ?: "Time for your scheduled task!"
        val reminderId = intent.getIntExtra(EXTRA_REMINDER_ID, (System.currentTimeMillis() % 10000).toInt())

        Log.i(TAG, "Reminder triggered: $taskText (ID: $reminderId)")

        // 1. Play vibration
        try {
            val vibrator = context.getSystemService(Vibrator::class.java)
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val timings = longArrayOf(0, 400, 200, 400, 200, 600)
                    val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(longArrayOf(0, 400, 200, 400, 200, 600), -1)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Vibrate error on reminder: ${e.localizedMessage}")
        }

        // 2. Post high-priority Heads-up Notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        createReminderChannel(context, notificationManager)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            reminderId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("⏰ Agal Reminder")
            .setContentText(taskText)
            .setStyle(NotificationCompat.BigTextStyle().bigText("Hey! You asked me to remind you:\n$taskText"))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true)
            .build()

        notificationManager?.notify(reminderId, notification)
    }

    private fun createReminderChannel(context: Context, manager: NotificationManager?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Agal Scheduled Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority alarms and reminders spoken by Agal"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400, 200, 600)
                setSound(soundUri, audioAttributes)
            }
            manager?.createNotificationChannel(channel)
        }
    }
}
