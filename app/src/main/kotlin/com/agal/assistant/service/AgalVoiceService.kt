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
import com.agal.assistant.asr.VoskSpeechRecognizer
import com.agal.assistant.engine.CommandEngine
import com.agal.assistant.tts.AndroidVoiceSpeaker
import com.agal.assistant.wakeword.PorcupineWakeWordEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class AgalVoiceService : Service() {

    private val TAG = "AgalVoiceService"
    private val SERVICE_CHANNEL_ID = "agal_voice_channel"
    private val WAKE_CHANNEL_ID = "agal_wake_channel"
    private val NOTIF_ID = 1001
    private val WAKE_NOTIF_ID = 1002

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    private var wakeLock: PowerManager.WakeLock? = null
    private var porcupineEngine: PorcupineWakeWordEngine? = null
    private var voskRecognizer: VoskSpeechRecognizer? = null
    private var voiceSpeaker: AndroidVoiceSpeaker? = null
    private var commandEngine: CommandEngine? = null

    private var lastWakeTime: Long = 0

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()

        val notification = buildOngoingNotification("Listening for \"Hey Agal\" or \"Agal\"")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIF_ID, notification)
        }

        acquireWakeLock()
        initEngines()
    }

    private fun initEngines() {
        commandEngine = CommandEngine(this)
        voiceSpeaker = AndroidVoiceSpeaker(this).apply { init() }

        // 1. Porcupine Engine (for custom wake words if access key is configured)
        porcupineEngine = PorcupineWakeWordEngine(this) { detectedKeyword ->
            Log.i(TAG, "Porcupine detected wake word: $detectedKeyword")
            handleLockScreenWake(detectedKeyword, "")
        }
        porcupineEngine?.init()
        porcupineEngine?.start()

        // 2. Vosk Offline Acoustic Recognizer (100% offline continuous listener)
        voskRecognizer = VoskSpeechRecognizer(this) { text, isFinal ->
            handleRecognizedSpeech(text, isFinal)
        }
        voskRecognizer?.initModel { isReady ->
            Log.i(TAG, "Vosk offline model initialized in service: $isReady")
            if (isReady) {
                voskRecognizer?.startListening()
            }
        }
    }

    private fun handleRecognizedSpeech(text: String, isFinal: Boolean) {
        val lower = text.lowercase().trim()
        if (lower.isEmpty()) return

        Log.d(TAG, "Acoustic audio heard (final=$isFinal): $lower")

        // Broadcast to any active UI
        val broadcastIntent = Intent("com.agal.assistant.SPEECH_EVENT").apply {
            putExtra("TEXT", text)
            putExtra("IS_FINAL", isFinal)
            setPackage(packageName)
        }
        sendBroadcast(broadcastIntent)

        // Strict wake word check — must contain "agal" as a clear word boundary
        // to avoid false-positives from random words like "eagle", "legal", "regal"
        val isWakeWord = lower == "agal" ||
                lower == "hey agal" ||
                lower == "hello agal" ||
                lower == "hi agal" ||
                lower.startsWith("agal ") ||
                lower.startsWith("hey agal ") ||
                lower.startsWith("hello agal ") ||
                lower.startsWith("hi agal ") ||
                lower.endsWith(" agal") ||
                lower.contains("hey agal") ||
                lower.contains("hello agal") ||
                lower.contains("hi agal")

        val now = System.currentTimeMillis()
        if (isWakeWord && (now - lastWakeTime > 3000)) {
            lastWakeTime = now
            Log.i(TAG, "Wake word match detected in service: $lower")

            // Extract command after wake phrase if user said e.g. "hey agal turn on flashlight"
            val commandPart = lower
                .replace("hey agal", "")
                .replace("hello agal", "")
                .replace("hi agal", "")
                .removePrefix("agal")
                .trim()

            handleLockScreenWake("Agal", commandPart)
        } else if (isFinal && isDirectCommand(lower) && (now - lastWakeTime > 3000)) {
            lastWakeTime = now
            Log.i(TAG, "Direct command match detected on lock screen: $lower")
            handleLockScreenWake("Agal", lower)
        }
    }

    private fun isDirectCommand(text: String): Boolean {
        return text.contains("remind me") ||
                text.contains("remember me") ||
                text.contains("reminder") ||
                text.contains("flashlight") ||
                text.contains("torch") ||
                text.contains("bluetooth") ||
                text.contains("youtube") ||
                text.contains("music") ||
                text.contains("song") ||
                text.contains("track") ||
                text.contains("volume") ||
                text.contains("mute") ||
                text.contains("camera") ||
                text.contains("photo") ||
                text.contains("picture") ||
                text.contains("video") ||
                text.contains("record") ||
                text.contains("battery") ||
                text.contains("time") ||
                text.contains("date") ||
                text.contains("joke") ||
                text.contains("settings") ||
                text.contains("wifi") ||
                text.contains("wi-fi") ||
                text.contains("hotspot") ||
                text.contains("airplane") ||
                text.contains("flight mode") ||
                text.contains("dnd") ||
                text.contains("do not disturb") ||
                text.contains("silent") ||
                text.contains("notification") ||
                text.contains("answer") ||
                text.contains("attend") ||
                text.contains("pick up") ||
                text.contains("accept call") ||
                text.contains("hang up") ||
                text.contains("hangup") ||
                text.contains("end call") ||
                text.contains("cut the call") ||
                text.startsWith("open ") ||
                text.startsWith("launch ") ||
                text.startsWith("start ") ||
                text.contains("app") ||
                text.contains("bucket list") ||
                text.contains("trip") ||
                text.contains("spent") ||
                text.contains("expense")
    }

    private fun handleLockScreenWake(keyword: String, commandToRun: String) {
        Log.i(TAG, "Triggering lock screen wake response! keyword=$keyword command=$commandToRun")

        // 1. Physically turn on the screen
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        @Suppress("DEPRECATION")
        val screenWakeLock = powerManager?.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
            "Agal:ScreenWakeLock"
        )
        screenWakeLock?.acquire(6000)

        // 2. Strong double vibration feedback
        try {
            val vibrator = getSystemService(Vibrator::class.java)
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val timings = longArrayOf(0, 100, 80, 140)
                    val amplitudes = intArrayOf(0, 200, 0, 255)
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(250)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Vibrate error: ${e.localizedMessage}")
        }

        // 3. Launch MainActivity over keyguard via Full Screen Intent
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_CLEAR_TOP
            )
            putExtra("WAKE_TRIGGERED", true)
            putExtra("KEYWORD", keyword)
            if (commandToRun.isNotEmpty()) {
                putExtra("PENDING_COMMAND", commandToRun)
            }
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            WAKE_NOTIF_ID,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val wakeNotification = NotificationCompat.Builder(this, WAKE_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Agal • Wake Word Triggered")
            .setContentText("Listening to you...")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.notify(WAKE_NOTIF_ID, wakeNotification)

        try {
            startActivity(launchIntent)
        } catch (e: Exception) {
            Log.w(TAG, "Direct startActivity fallback: ${e.localizedMessage}")
        }

        // 4. Voice response and command execution
        if (commandToRun.isNotEmpty()) {
            serviceScope.launch {
                val result = commandEngine?.execute(commandToRun)
                val reply = result?.spokenReply ?: "Executing command."
                voiceSpeaker?.speak(reply)
            }
        } else {
            voiceSpeaker?.speak("I'm listening.")
        }
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Agal:BackgroundVoiceLock")
        wakeLock?.acquire()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java) ?: return

            // 1. Ongoing service channel (silent)
            val serviceChannel = NotificationChannel(
                SERVICE_CHANNEL_ID,
                "Agal Offline Voice Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps offline wake-word detector active on lock screen"
                setShowBadge(false)
            }
            manager.createNotificationChannel(serviceChannel)

            // 2. High-priority Wake channel (for lock screen full screen presentation)
            val wakeChannel = NotificationChannel(
                WAKE_CHANNEL_ID,
                "Agal Wake Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Wakes up screen when Hey Agal or Agal is detected"
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(wakeChannel)
        }
    }

    private fun buildOngoingNotification(contentText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, SERVICE_CHANNEL_ID)
            .setContentTitle("Agal • Offline Assistant Active")
            .setContentText(contentText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.getBooleanExtra("RESTART_LISTENING", false) == true) {
            voskRecognizer?.startListening()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        porcupineEngine?.release()
        voskRecognizer?.release()
        voiceSpeaker?.shutdown()
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

