package com.agal.assistant.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.core.app.NotificationCompat
import com.agal.assistant.MainActivity
import com.agal.assistant.R
import com.agal.assistant.tts.AndroidVoiceSpeaker
import com.agal.assistant.wakeword.PorcupineWakeWordEngine

class AgalVoiceService : Service() {

    private val TAG = "AgalVoiceService"
    private val CHANNEL_ID = "agal_voice_channel"
    private val NOTIF_ID = 1001

    private var wakeLock: PowerManager.WakeLock? = null
    private var porcupineEngine: PorcupineWakeWordEngine? = null
    private var voiceSpeaker: AndroidVoiceSpeaker? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val notification = buildNotification("Listening for \"Hey Agal\" or \"Agal\"")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIF_ID, notification)
        }

        acquireWakeLock()
        initEngines()
    }

    private fun initEngines() {
        voiceSpeaker = AndroidVoiceSpeaker(this).apply { init() }

        porcupineEngine = PorcupineWakeWordEngine(this) { detectedKeyword ->
            Log.i(TAG, "Wake word detected in background/lock screen: $detectedKeyword")
            handleLockScreenWake(detectedKeyword)
        }
        porcupineEngine?.init()
        porcupineEngine?.start()
    }

    private fun handleLockScreenWake(keyword: String) {
        // 1. Wake the screen
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        @Suppress("DEPRECATION")
        val screenWakeLock = powerManager?.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
            "Agal:ScreenWakeLock"
        )
        screenWakeLock?.acquire(5000)

        // 2. Vibrate
        try {
            val vibrator = getSystemService(Vibrator::class.java)
            if (vibrator?.hasVibrator() == true) {
                vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Vibrate error: ${e.localizedMessage}")
        }

        // 3. Launch MainActivity over lock screen
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("WAKE_TRIGGERED", true)
            putExtra("KEYWORD", keyword)
        }
        startActivity(launchIntent)

        // 4. Voice reply directly
        voiceSpeaker?.speak("I'm listening.")
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Agal:BackgroundVoiceLock")
        wakeLock?.acquire()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Agal Offline Voice Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps offline wake-word detector active on lock screen"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(contentText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Agal • Offline Assistant Active")
            .setContentText(contentText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        porcupineEngine?.release()
        voiceSpeaker?.shutdown()
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
