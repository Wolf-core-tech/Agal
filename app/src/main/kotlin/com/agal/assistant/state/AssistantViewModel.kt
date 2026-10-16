package com.agal.assistant.state

import android.app.Application
import android.content.Intent
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agal.assistant.engine.CommandEngine
import com.agal.assistant.engine.models.CommandHistoryEntry
import com.agal.assistant.service.AgalVoiceService
import com.agal.assistant.tts.AndroidVoiceSpeaker
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "AssistantViewModel"

    private val _uiState = MutableStateFlow(AssistantUiState())
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    private val commandEngine = CommandEngine(application)
    private var currentSpeakJob: kotlinx.coroutines.Job? = null

    /**
     * Broadcast receiver that bridges service events into ViewModel UI state.
     *
     * WAKE_EVENT    — service detected the wake word while screen was off;
     *                 ViewModel animates the wake response.
     * SPEECH_EVENT  — service forwarding live ASR text during command listening.
     * COMMAND_RESULT — service executed a command in the background (screen was off).
     * STOP_SPEAKING — another component wants TTS stopped.
     */
    private val serviceEventReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
            when (intent?.action) {
                AgalVoiceService.BROADCAST_WAKE_EVENT -> {
                    val keyword = intent.getStringExtra("KEYWORD") ?: "Agal"
                    val inlineCommand = intent.getStringExtra("INLINE_COMMAND") ?: ""
                    if (inlineCommand.isNotEmpty()) {
                        processSpokenCommand(inlineCommand)
                    } else {
                        onWakeWordTriggered(keyword)
                    }
                }
                AgalVoiceService.BROADCAST_SPEECH_EVENT -> {
                    val text = intent.getStringExtra("TEXT") ?: return
                    val isFinal = intent.getBooleanExtra("IS_FINAL", false)
                    _uiState.update { it.copy(lastRecognizedText = text) }
                    if (isFinal && text.isNotBlank()) {
                        processSpokenCommand(text)
                    }
                }
                AgalVoiceService.BROADCAST_COMMAND_RESULT -> {
                    // Service executed a command while screen was off — update history
                    val reply = intent.getStringExtra("SPOKEN_REPLY") ?: return
                    val title = intent.getStringExtra("DISPLAY_TITLE") ?: ""
                    val desc = intent.getStringExtra("DISPLAY_DESC") ?: ""
                    val ok = intent.getBooleanExtra("IS_SUCCESS", true)
                    val entry = CommandHistoryEntry(
                        userInput = title,
                        assistantReply = reply,
                        commandType = com.agal.assistant.engine.models.CommandType.UNKNOWN,
                        isSuccess = ok
                    )
                    _uiState.update { current ->
                        current.copy(
                            status = AssistantStatus.IDLE,
                            activeStatusMessage = reply,
                            history = listOf(entry) + current.history,
                            totalCommandsExecuted = current.totalCommandsExecuted + 1
                        )
                    }
                }
                "com.agal.assistant.STOP_SPEAKING" -> stopSpeaking(broadcast = false)
            }
        }
    }

    private val voiceSpeaker = AndroidVoiceSpeaker(application) { isSpeaking ->
        _uiState.update { it.copy(isSpeaking = isSpeaking) }
    }

    val bucketRepo = com.agal.assistant.lifestyle.BucketListRepository(application)
    private val _bucketItems = MutableStateFlow<List<com.agal.assistant.lifestyle.BucketItem>>(emptyList())
    val bucketItems: StateFlow<List<com.agal.assistant.lifestyle.BucketItem>> = _bucketItems.asStateFlow()

    val tripRepo = com.agal.assistant.lifestyle.TripRepository(application)
    private val _trips = MutableStateFlow<List<com.agal.assistant.lifestyle.TripDestination>>(emptyList())
    val trips: StateFlow<List<com.agal.assistant.lifestyle.TripDestination>> = _trips.asStateFlow()

    val financeRepo = com.agal.assistant.lifestyle.FinanceRepository(application)
    private val _transactions = MutableStateFlow<List<com.agal.assistant.lifestyle.FinanceTransaction>>(emptyList())
    val transactions: StateFlow<List<com.agal.assistant.lifestyle.FinanceTransaction>> = _transactions.asStateFlow()

    val credentialRepo = com.agal.assistant.lifestyle.CredentialVaultRepository(application)
    private val _credentials = MutableStateFlow<List<com.agal.assistant.lifestyle.SavedCredential>>(emptyList())
    val credentials: StateFlow<List<com.agal.assistant.lifestyle.SavedCredential>> = _credentials.asStateFlow()

    private val settingsRepo = com.agal.assistant.settings.AssistantSettingsRepository(application)

    init {
        val savedWakeWord = settingsRepo.getWakeWord()
        val savedReply = settingsRepo.getListeningReply()
        val savedLang = settingsRepo.getLanguage()
        _uiState.update {
            it.copy(
                activeWakeWord = savedWakeWord,
                listeningReply = savedReply,
                assistantLanguage = savedLang,
                activeStatusMessage = "Say \"$savedWakeWord\" or \"Agal\""
            )
        }

        // Subscribe to service broadcast events
        val filter = android.content.IntentFilter().apply {
            addAction(AgalVoiceService.BROADCAST_WAKE_EVENT)
            addAction(AgalVoiceService.BROADCAST_SPEECH_EVENT)
            addAction(AgalVoiceService.BROADCAST_COMMAND_RESULT)
            addAction("com.agal.assistant.STOP_SPEAKING")
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            application.registerReceiver(serviceEventReceiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED)
        } else {
            application.registerReceiver(serviceEventReceiver, filter)
        }

        voiceSpeaker.init()
        loadLifestyleData()
        startWakeWordListening()
    }

    fun startWakeWordListening() {
        porcupineEngine.start()
        _uiState.update {
            it.copy(
                status = AssistantStatus.LISTENING_WAKE_WORD,
                activeStatusMessage = "Listening for \"${it.activeWakeWord}\" or \"Agal\"..."
            )
        }
    }

    fun startCommandListening() {
        porcupineEngine.stop()
        voskRecognizer.startListening()
        _uiState.update {
            it.copy(
                status = AssistantStatus.LISTENING_COMMAND,
                activeStatusMessage = "Listening to your command..."
            )
        }
    }

    fun onWakeWordTriggered(keyword: String) {
        vibratePhone()
        porcupineEngine.stop()

        val replyToSpeak = _uiState.value.listeningReply.ifBlank { "Yes Venkat" }

        _uiState.update {
            it.copy(
                status = AssistantStatus.WAKE_WORD_TRIGGERED,
                activeStatusMessage = replyToSpeak
            )
        }

        currentSpeakJob?.cancel()
        currentSpeakJob = viewModelScope.launch {
            voiceSpeaker.speak(replyToSpeak)
            delay(1200)
            startCommandListening()
        }
    }

    fun stopSpeaking(broadcast: Boolean = true) {
        Log.i(TAG, "stopSpeaking invoked — stopping TTS and cancelling coroutines")
        currentSpeakJob?.cancel()
        currentSpeakJob = null
        voiceSpeaker.stop()
        voskRecognizer.stopListening()

        if (broadcast) {
            try {
                val stopIntent = android.content.Intent("com.agal.assistant.STOP_SPEAKING").apply {
                    setPackage(getApplication<Application>().packageName)
                }
                getApplication<Application>().sendBroadcast(stopIntent)
            } catch (e: Exception) {
                Log.e(TAG, "Error broadcasting stop: ${e.localizedMessage}")
            }
        }

        _uiState.update {
            it.copy(
                isSpeaking = false,
                status = if (it.isContinuousListening) AssistantStatus.LISTENING_WAKE_WORD else AssistantStatus.IDLE,
                activeStatusMessage = if (it.isContinuousListening) "Stopped. Listening for \"${it.activeWakeWord}\" or \"Agal\"..." else "Stopped. Ready."
            )
        }

        if (_uiState.value.isContinuousListening) {
            startWakeWordListening()
        }
    }

    fun processSpokenCommand(commandText: String) {
        val trimmed = commandText.trim()
        if (trimmed.isEmpty()) return

        val lower = trimmed.lowercase()
        if (lower == "stop" || lower == "stop speaking" || lower == "be quiet" ||
            lower == "shut up" || lower == "quiet" || lower == "stop agal" ||
            lower == "agal stop" || lower == "cancel") {
            stopSpeaking()
            return
        }

        voskRecognizer.stopListening()
        _uiState.update {
            it.copy(
                status = AssistantStatus.PROCESSING,
                activeStatusMessage = "Processing \"$trimmed\" offline...",
                lastRecognizedText = trimmed
            )
        }

        currentSpeakJob?.cancel()
        currentSpeakJob = viewModelScope.launch {
            val result = commandEngine.execute(trimmed)

            val entry = CommandHistoryEntry(
                userInput = trimmed,
                assistantReply = result.spokenReply,
                commandType = result.type,
                isSuccess = result.isSuccess
            )

            _uiState.update { current ->
                current.copy(
                    status = AssistantStatus.SPEAKING_REPLY,
                    activeStatusMessage = result.spokenReply,
                    history = listOf(entry) + current.history,
                    totalCommandsExecuted = current.totalCommandsExecuted + 1
                )
            }

            voiceSpeaker.speak(result.spokenReply)
            loadLifestyleData()

            delay(3000)
            if (_uiState.value.isContinuousListening) {
                startWakeWordListening()
            } else {
                _uiState.update {
                    it.copy(
                        status = AssistantStatus.IDLE,
                        activeStatusMessage = "Ready. Say \"${it.activeWakeWord}\" or tap mic."
                    )
                }
            }
        }
    }

    fun simulateSpokenPhrase(phrase: String) {
        val lower = phrase.lowercase().trim()

        if (lower == "stop" || lower == "stop speaking" || lower == "shut up" || lower == "be quiet" || lower == "stop agal") {
            stopSpeaking()
            return
        }

        val wakeWord = _uiState.value.activeWakeWord.lowercase().trim()
        val isWake = lower == wakeWord || lower == "hey agal" || lower == "agal" ||
                lower.startsWith("$wakeWord ") || lower.startsWith("hey agal ") || lower.startsWith("agal ")

        if (isWake) {
            val commandPart = when {
                lower.startsWith("$wakeWord ") -> lower.removePrefix("$wakeWord ").trim()
                lower.startsWith("hey agal ") -> lower.removePrefix("hey agal ").trim()
                lower.startsWith("agal ") -> lower.removePrefix("agal ").trim()
                else -> ""
            }
            if (commandPart.isNotEmpty()) {
                processSpokenCommand(commandPart)
            } else {
                onWakeWordTriggered(_uiState.value.activeWakeWord)
            }
        } else {
            processSpokenCommand(phrase)
        }
    }

    fun clearAllPredefinedData() {
        bucketRepo.clearAll()
        tripRepo.clearAll()
        financeRepo.clearAll()
        loadLifestyleData()
    }

    fun setCustomWakeWord(name: String) {
        val clean = name.trim().ifEmpty { "Hey Agal" }
        porcupineEngine.currentWakeWord = clean
        settingsRepo.setWakeWord(clean)
        _uiState.update { it.copy(activeWakeWord = clean, activeStatusMessage = "Say \"$clean\" or tap to speak") }
    }

    fun setCustomListeningReply(reply: String) {
        val clean = reply.trim().ifEmpty { "I'm listening." }
        settingsRepo.setListeningReply(clean)
        _uiState.update { it.copy(listeningReply = clean) }
    }

    fun setLanguage(lang: String) {
        settingsRepo.setLanguage(lang)
        _uiState.update { it.copy(assistantLanguage = lang) }
    }

    fun setContinuousListening(enabled: Boolean) {
        _uiState.update { it.copy(isContinuousListening = enabled) }
        if (enabled) {
            startWakeWordListening()
        } else {
            porcupineEngine.stop()
            voskRecognizer.stopListening()
            _uiState.update {
                it.copy(
                    status = AssistantStatus.IDLE,
                    activeStatusMessage = "Paused. Tap mic to speak."
                )
            }
        }
    }

    fun setMuted(muted: Boolean) {
        voiceSpeaker.isMuted = muted
        _uiState.update { it.copy(isMuted = muted) }
        if (muted) voiceSpeaker.stop()
    }

    fun setSpeechRate(rate: Float) {
        voiceSpeaker.speechRate = rate
        _uiState.update { it.copy(speechRate = rate) }
    }

    fun setPitch(pitch: Float) {
        voiceSpeaker.pitch = pitch
        _uiState.update { it.copy(pitch = pitch) }
    }

    fun speakSample() {
        voiceSpeaker.speak("Hello! I am Agal, speaking back to you completely offline.")
    }

    fun loadLifestyleData() {
        _bucketItems.value = bucketRepo.getItems()
        _trips.value = tripRepo.getTrips()
        _transactions.value = financeRepo.getTransactions()
        _credentials.value = credentialRepo.getCredentials()
    }

    // Bucket list methods
    fun addBucketItem(text: String) {
        val (_, list) = bucketRepo.addItem(text)
        _bucketItems.value = list
    }

    fun toggleBucketItem(id: String) {
        _bucketItems.value = bucketRepo.toggleComplete(id)
    }

    fun deleteBucketItem(id: String) {
        _bucketItems.value = bucketRepo.deleteItem(id)
    }

    // Trip methods
    fun addTrip(destination: String, expense: Double? = null) {
        val (_, list) = tripRepo.addTrip(destination, expense)
        _trips.value = list
    }

    fun deleteTrip(id: String) {
        _trips.value = tripRepo.deleteTrip(id)
    }

    // Finance methods
    fun addFinanceTransaction(title: String, amount: Double, category: String, isIncome: Boolean) {
        val type = if (isIncome) com.agal.assistant.lifestyle.FinanceType.INCOME else com.agal.assistant.lifestyle.FinanceType.EXPENSE
        val (_, list) = financeRepo.addTransaction(title, amount, category, type)
        _transactions.value = list
    }

    fun deleteFinanceTransaction(id: String) {
        _transactions.value = financeRepo.deleteTransaction(id)
    }

    // Credential Vault methods
    fun saveCredential(accountName: String, userIdOrEmail: String, pass: String, notes: String = "") {
        _credentials.value = credentialRepo.saveCredential(accountName, userIdOrEmail, pass, notes)
    }

    fun deleteCredential(id: String) {
        _credentials.value = credentialRepo.deleteCredential(id)
    }

    fun clearHistory() {
        _uiState.update { it.copy(history = emptyList()) }
    }

    private fun vibratePhone() {
        try {
            val vibrator = getApplication<Application>().getSystemService(Vibrator::class.java)
            if (vibrator?.hasVibrator() == true) {
                vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Vibrate error: ${e.localizedMessage}")
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            getApplication<Application>().unregisterReceiver(stopBroadcastReceiver)
        } catch (e: Exception) {
            // Already unregistered
        }
        porcupineEngine.release()
        voskRecognizer.release()
        voiceSpeaker.shutdown()
    }
}
