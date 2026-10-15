package com.agal.assistant.state

import android.app.Application
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agal.assistant.asr.VoskSpeechRecognizer
import com.agal.assistant.engine.CommandEngine
import com.agal.assistant.engine.models.CommandHistoryEntry
import com.agal.assistant.tts.AndroidVoiceSpeaker
import com.agal.assistant.wakeword.PorcupineWakeWordEngine
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

    private val voiceSpeaker = AndroidVoiceSpeaker(application) { isSpeaking ->
        _uiState.update { it.copy(isSpeaking = isSpeaking) }
    }

    private val porcupineEngine = PorcupineWakeWordEngine(application) { detectedKeyword ->
        onWakeWordTriggered(detectedKeyword)
    }

    private val voskRecognizer = VoskSpeechRecognizer(application) { text, isFinal ->
        _uiState.update { it.copy(lastRecognizedText = text) }
        if (isFinal && text.isNotBlank()) {
            processSpokenCommand(text)
        }
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
                activeStatusMessage = "Say \"$savedWakeWord\" or tap to speak"
            )
        }
        porcupineEngine.currentWakeWord = savedWakeWord

        voiceSpeaker.init()
        porcupineEngine.init()
        loadLifestyleData()
        voskRecognizer.initModel { isReady ->
            Log.i(TAG, "Vosk model initialization result: $isReady")
        }

        // Default start wake-word listening
        startWakeWordListening()
    }

    fun startWakeWordListening() {
        porcupineEngine.start()
        _uiState.update {
            it.copy(
                status = AssistantStatus.LISTENING_WAKE_WORD,
                activeStatusMessage = "Listening for \"${it.activeWakeWord}\"..."
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

    private fun onWakeWordTriggered(keyword: String) {
        vibratePhone()
        porcupineEngine.stop()

        val replyToSpeak = _uiState.value.listeningReply.ifBlank { "I'm listening." }

        _uiState.update {
            it.copy(
                status = AssistantStatus.WAKE_WORD_TRIGGERED,
                activeStatusMessage = replyToSpeak
            )
        }

        viewModelScope.launch {
            voiceSpeaker.speak(replyToSpeak)
            delay(1200)
            startCommandListening()
        }
    }

    fun processSpokenCommand(commandText: String) {
        val trimmed = commandText.trim()
        if (trimmed.isEmpty()) return

        voskRecognizer.stopListening()
        _uiState.update {
            it.copy(
                status = AssistantStatus.PROCESSING,
                activeStatusMessage = "Processing \"$trimmed\" offline...",
                lastRecognizedText = trimmed
            )
        }

        viewModelScope.launch {
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
                        activeStatusMessage = "Ready. Tap mic to speak."
                    )
                }
            }
        }
    }

    fun simulateSpokenPhrase(phrase: String) {
        val wakeWord = _uiState.value.activeWakeWord.lowercase()
        val lower = phrase.lowercase()

        if (lower.contains(wakeWord) || lower.startsWith("hey agal") || lower.startsWith("agal")) {
            val afterWake = phrase.substringAfter(wakeWord, "").trim()
            if (afterWake.isNotEmpty()) {
                processSpokenCommand(afterWake)
            } else {
                onWakeWordTriggered(_uiState.value.activeWakeWord)
            }
        } else {
            processSpokenCommand(phrase)
        }
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
        porcupineEngine.release()
        voskRecognizer.release()
        voiceSpeaker.shutdown()
    }
}
