package com.agal.assistant.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

data class AgalReminder(
    val id: Int = (System.currentTimeMillis() % 100000).toInt(),
    val task: String,
    val triggerTimeMillis: Long,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("task", task)
            put("triggerTimeMillis", triggerTimeMillis)
            put("createdAt", createdAt)
        }
    }

    companion object {
        fun fromJson(obj: JSONObject): AgalReminder {
            return AgalReminder(
                id = obj.optInt("id", 0),
                task = obj.optString("task", ""),
                triggerTimeMillis = obj.optLong("triggerTimeMillis", 0L),
                createdAt = obj.optLong("createdAt", 0L)
            )
        }
    }
}

class ReminderManager(private val context: Context) {

    companion object {
        private const val TAG = "ReminderManager"
        private const val PREFS_NAME = "agal_reminders_pref"
        private const val KEY_REMINDERS = "reminders_list"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getAllReminders(): List<AgalReminder> {
        val raw = prefs.getString(KEY_REMINDERS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<AgalReminder>()
            for (i in 0 until array.length()) {
                list.add(AgalReminder.fromJson(array.getJSONObject(i)))
            }
            list.filter { it.triggerTimeMillis > System.currentTimeMillis() - 60000 }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing reminders: ${e.localizedMessage}")
            emptyList()
        }
    }

    private fun saveReminders(list: List<AgalReminder>) {
        val array = JSONArray()
        list.forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY_REMINDERS, array.toString()).apply()
    }

    fun scheduleReminder(task: String, triggerTimeMillis: Long): AgalReminder {
        val reminder = AgalReminder(
            id = (System.currentTimeMillis() % 100000).toInt(),
            task = task,
            triggerTimeMillis = triggerTimeMillis
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            putExtra(ReminderAlarmReceiver.EXTRA_TASK_TEXT, reminder.task)
            putExtra(ReminderAlarmReceiver.EXTRA_REMINDER_ID, reminder.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (alarmManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMillis,
                        pendingIntent
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling alarm: ${e.localizedMessage}")
            alarmManager?.set(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent)
        }

        val current = getAllReminders().toMutableList()
        current.add(reminder)
        saveReminders(current)
        Log.i(TAG, "Reminder scheduled: '${reminder.task}' at $triggerTimeMillis")
        return reminder
    }

    fun clearAllReminders(): Int {
        val all = getAllReminders()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        for (reminder in all) {
            val intent = Intent(context, ReminderAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                reminder.id,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null && alarmManager != null) {
                alarmManager.cancel(pendingIntent)
            }
        }
        saveReminders(emptyList())
        return all.size
    }
}
