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
import com.agal.assistant.settings.AssistantSettingsRepository
import com.agal.assistant.tts.AndroidVoiceSpeaker
import com.agal.assistant.wakeword.PorcupineWakeWordEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

    /** True when the UI (MainActivity) is in the foreground and handling commands */
    private var isUiActive: Boolean = false

    /** True once the wake word fires — Vosk switches to open-vocabulary command mode */
    private var isCommandListeningMode: Boolean = false

    companion object {
        const val ACTION_STOP_SPEAKING = "com.agal.assistant.ACTION_STOP_SPEAKING"
        const val ACTION_RESTART_LISTENING = "com.agal.assistant.RESTART_LISTENING"
        const val ACTION_UI_ACTIVE = "com.agal.assistant.UI_ACTIVE"
        const val ACTION_UI_INACTIVE = "com.agal.assistant.UI_INACTIVE"
        const val BROADCAST_WAKE_EVENT = "com.agal.assistant.WAKE_EVENT"
        const val BROADCAST_SPEECH_EVENT = "com.agal.assistant.SPEECH_EVENT"
        const val BROADCAST_COMMAND_RESULT = "com.agal.assistant.COMMAND_RESULT"

        private const val WAKE_COOLDOWN_MS = 3000L
        private const val COMMAND_LISTEN_TIMEOUT_MS = 8000L
    }

    private val controlReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "com.agal.assistant.STOP_SPEAKING" -> voiceSpeaker?.stop()
                ACTION_UI_ACTIVE -> { isUiActive = true; Log.d(TAG, "UI active") }
                ACTION_UI_INACTIVE -> { isUiActive = false; Log.d(TAG, "UI inactive") }
                ACTION_RESTART_LISTENING -> returnToStandby()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()

        val notification = buildOngoingNotification("Listening for \"Agal\"...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIF_ID, notification)
        }

        val filter = android.content.IntentFilter().apply {
            addAction("com.agal.assistant.STOP_SPEAKING")
            addAction(ACTION_UI_ACTIVE)
            addAction(ACTION_UI_INACTIVE)
            addAction(ACTION_RESTART_LISTENING)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(controlReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(controlReceiver, filter)
        }

        acquireWakeLock()
        initEngines()
    }

    private fun initEngines() {
        commandEngine = CommandEngine(this)
        voiceSpeaker = AndroidVoiceSpeaker(this).apply { init() }

        // Porcupine: hardware-accelerated wake word (only if AccessKey is configured)
        porcupineEngine = PorcupineWakeWordEngine(this) { detectedKeyword ->
            Log.i(TAG, "Porcupine detected wake word: $detectedKeyword")
            onWakeWordDetected(detectedKeyword, "")
        }
        porcupineEngine?.init()
        porcupineEngine?.start()

        // Vosk: THE single mic owner — handles standby (wake-word check) AND command listening
        voskRecognizer = VoskSpeechRecognizer(this) { text, isFinal ->
            handleVoskResult(text, isFinal)
        }
        voskRecognizer?.initModel { isReady ->
            Log.i(TAG, "Vosk model ready: $isReady")
            if (isReady) voskRecognizer?.startListeningStandby()
        }
    }

    /**
     * Routes Vosk results based on the current listening mode.
     * STANDBY: check for wake word only — ignore everything else (no false triggers)
     * COMMAND: forward speech to UI, or execute locally if UI is not visible
     */
    private fun handleVoskResult(text: String, isFinal: Boolean) {
        val lower = text.lowercase().trim()
        if (lower.isEmpty()) return

        if (isCommandListeningMode) {
            if (isUiActive) {
                sendBroadcast(Intent(BROADCAST_SPEECH_EVENT).apply {
                    putExtra("TEXT", text)
                    putExtra("IS_FINAL", isFinal)
                    setPackage(packageName)
                })
            }
            if (isFinal && !isUiActive && lower.isNotBlank()) {
                isCommandListeningMode = false
                voskRecognizer?.stopListening()
                executeCommandInService(text)
            }
        } else {
            if (!isWakeWordMatch(lower)) return

            val now = System.currentTimeMillis()
            if (now - lastWakeTime < WAKE_COOLDOWN_MS) {
                Log.d(TAG, "Wake word suppressed — cooldown active")
                return
            }
            lastWakeTime = now

            val inlineCommand = extractInlineCommand(lower)
            Log.i(TAG, "Wake word detected: '$lower' | inline='$inlineCommand'")
            onWakeWordDetected("Agal", inlineCommand)
        }
    }

    private fun isWakeWordMatch(lower: String): Boolean {
        val customWake = com.agal.assistant.settings.AssistantSettingsRepository(this).getWakeWord().lowercase().trim()
        return lower == "agal" || lower == "hey agal" || lower == customWake ||
                lower.startsWith("agal ") || lower.startsWith("hey agal ") ||
                (customWake.isNotEmpty() && lower.startsWith("$customWake "))
    }

    private fun extractInlineCommand(lower: String): String {
        val customWake = com.agal.assistant.settings.AssistantSettingsRepository(this).getWakeWord().lowercase().trim()
        return when {
            lower.startsWith("hey agal ") -> lower.removePrefix("hey agal ").trim()
            lower.startsWith("agal ") -> lower.removePrefix("agal ").trim()
            customWake.isNotEmpty() && lower.startsWith("$customWake ") -> lower.removePrefix("$customWake ").trim()
            else -> ""
        }
    }

    /**
     * Central wake-word response handler.
     * If UI is active: broadcast the event and let the ViewModel animate + listen.
     * If screen is off: wake screen, speak acknowledgement, then listen for command.
     */
    private fun onWakeWordDetected(keyword: String, inlineCommand: String) {
        vibrateWake()

        if (isUiActive) {
            sendBroadcast(Intent(BROADCAST_WAKE_EVENT).apply {
                putExtra("KEYWORD", keyword)
                putExtra("INLINE_COMMAND", inlineCommand)
                setPackage(packageName)
            })
            return
        }

        wakeScreen()
        showWakeNotification(inlineCommand)
        tryLaunchMainActivity(keyword, inlineCommand)

        val reply = com.agal.assistant.settings.AssistantSettingsRepository(this).getListeningReply()
        if (inlineCommand.isNotEmpty()) {
            serviceScope.launch { executeCommandInService(inlineCommand) }
        } else {
            serviceScope.launch {
                voiceSpeaker?.speak(reply)
                delay(1500)
                switchToCommandListeningMode()
            }
        }
    }

    private fun switchToCommandListeningMode() {
        isCommandListeningMode = true
        voskRecognizer?.stopListening()
        voskRecognizer?.startListeningCommand()
        updateNotificationText("Listening for your command...")
        Log.i(TAG, "COMMAND LISTENING mode")
        serviceScope.launch {
            delay(COMMAND_LISTEN_TIMEOUT_MS)
            if (isCommandListeningMode) returnToStandby()
        }
    }

    private fun executeCommandInService(command: String) {
        serviceScope.launch {
            Log.i(TAG, "Service executing: '$command'")
            val result = commandEngine?.execute(command)
            val reply = result?.spokenReply ?: "Done."
            voiceSpeaker?.speak(reply)
            sendBroadcast(Intent(BROADCAST_COMMAND_RESULT).apply {
                putExtra("SPOKEN_REPLY", reply)
                putExtra("IS_SUCCESS", result?.isSuccess ?: false)
                putExtra("DISPLAY_TITLE", result?.displayTitle ?: "")
                putExtra("DISPLAY_DESC", result?.displayDescription ?: "")
                setPackage(packageName)
            })
            delay(2000)
            returnToStandby()
        }
    }

    private fun returnToStandby() {
        isCommandListeningMode = false
        voskRecognizer?.stopListening()
        voskRecognizer?.startListeningStandby()
        porcupineEngine?.start()
        updateNotificationText("Listening for \"Agal\"...")
        Log.i(TAG, "Back in STANDBY mode")
    }

    private fun wakeScreen() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        @Suppress("DEPRECATION")
        powerManager?.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
            "Agal:ScreenWakeLock"
        )?.acquire(6000)
    }

    private fun vibrateWake() {
        try {
            val vibrator = getSystemService(Vibrator::class.java)
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val timings = longArrayOf(0, 80, 60, 120)
                    val amplitudes = intArrayOf(0, 180, 0, 255)
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(200)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Vibrate error: ${e.localizedMessage}")
        }
    }

    private fun tryLaunchMainActivity(keyword: String, pendingCommand: String) {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
            putExtra("WAKE_TRIGGERED", true)
            putExtra("KEYWORD", keyword)
            if (pendingCommand.isNotEmpty()) putExtra("PENDING_COMMAND", pendingCommand)
        }
        try { startActivity(launchIntent) } catch (e: Exception) {
            Log.w(TAG, "Could not launch MainActivity: ${e.localizedMessage}")
        }
    }

    private fun showWakeNotification(inlineCommand: String) {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("WAKE_TRIGGERED", true)
        }
        val pi = PendingIntent.getActivity(this, WAKE_NOTIF_ID, launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val reply = com.agal.assistant.settings.AssistantSettingsRepository(this).getListeningReply()
        val notif = NotificationCompat.Builder(this, WAKE_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Agal")
            .setContentText(if (inlineCommand.isNotEmpty()) "Executing: $inlineCommand" else reply)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(pi, true)
            .setAutoCancel(true)
            .build()
        getSystemService(NotificationManager::class.java)?.notify(WAKE_NOTIF_ID, notif)
    }

    private fun updateNotificationText(text: String) {
        getSystemService(NotificationManager::class.java)?.notify(NOTIF_ID, buildOngoingNotification(text))
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        // 4-hour timeout -- service restarts via START_STICKY and re-acquires
        wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Agal:BackgroundVoiceLock")
        wakeLock?.acquire(4 * 60 * 60 * 1000L)
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

        val stopIntent = Intent(this, AgalVoiceService::class.java).apply {
            action = ACTION_STOP_SPEAKING
        }
        val stopPending = PendingIntent.getService(
            this,
            1099,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, SERVICE_CHANNEL_ID)
            .setContentTitle("Agal • Offline Assistant Active")
            .setContentText(contentText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "STOP AGAL", stopPending)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_SPEAKING -> {
                voiceSpeaker?.stop()
                returnToStandby()
            }
            ACTION_RESTART_LISTENING -> returnToStandby()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(controlReceiver) } catch (_: Exception) {}
        porcupineEngine?.release()
        voskRecognizer?.release()
        voiceSpeaker?.shutdown()
        if (wakeLock?.isHeld == true) wakeLock?.release()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

