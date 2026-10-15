# Agal - 100% Offline Voice Assistant 🛡️⚡

A native Android application built with **Kotlin**, **Jetpack Compose**, **Picovoice Porcupine**, and **Vosk**, delivering privacy-first, on-device voice interactions with zero cloud dependency.

---

## 🔄 How the App Works (4-Step Pipeline)

```
┌────────────────────────┐      ┌────────────────────────┐
│ 1. Microphone Listens  │ ───► │ 2. Recognize Command   │
│ Offline Wake-Word      │      │ “Open my music app”    │
│ (Picovoice Porcupine)  │      │ (Vosk Offline ASR)     │
└────────────────────────┘      └────────────────────────┘
                                             │
                                             ▼
┌────────────────────────┐      ┌────────────────────────┐
│ 4. Perform & Reply     │ ◄─── │ 3. Process Locally     │
│ “Opening your music”   │      │ Match Kotlin Rules     │
│ (Android TextToSpeech) │      │ or Query Local AI      │
└────────────────────────┘      └────────────────────────┘
```

1. **Microphone Listens (Offline Wake-Word Detector)**:
   - Uses **Picovoice Porcupine** (`PorcupineWakeWordEngine.kt`).
   - Continuously samples audio stream in volatile RAM (zero cloud packets).
   - Detects your custom activation phrase (**“Hey Agal”**, **“Agal”**, or any custom name set in Settings).

2. **Recognize Your Command (“Open my music app”)**:
   - Uses **Vosk Offline Speech Recognition** (`VoskSpeechRecognizer.kt`).
   - Converts speech into text locally on your device hardware with low latency (<150ms).
   - Real-time partial results stream directly into the UI transcript.

3. **Process Locally (Match Command or Query Local AI)**:
   - **Kotlin Rules Engine** (`CommandEngine.kt`):
     - `MusicCommandRule`: `"Open my music app"`, `"Play music"`.
     - `YouTubeCommandRule`: `"Open YouTube"`, `"Search YouTube for [query]"`.
     - `BluetoothCommandRule`: `"Turn on Bluetooth"`, `"Bluetooth settings"`.
     - `FlashlightCommandRule`: Hardware rear LED torch toggle via `CameraManager`.
     - `CameraCommandRule`: Opens camera app.
     - `SystemSettingsRule`: Opens device system settings.
     - `TimeDateRule`: Speaks current time & date.
     - `BatteryRule`: Reads device battery level.
     - `JokeRule`: Offline humor bank.
   - **Local AI (llama.cpp Bridge)** (`LocalAiBridge.kt`):
     - If no direct deterministic rule matches, the query is processed by an on-device language model without contacting external servers.

4. **Perform Action and Reply (“Opening your music app.”)**:
   - **Action**: Launches `MediaStore.INTENT_ACTION_MUSIC_PLAYER` or media player intent.
   - **Voice Reply**: Android native `TextToSpeech` (`AndroidVoiceSpeaker.kt`) speaks back to you offline.
   - Pulsing glowing orb visualizes speech frequencies with dynamic equalizer waves.
   - Entry is stored in the local on-device Privacy Audit log with 1-tap **Wipe Logs**.

---

## 🛠️ Tech Stack

| Layer | Technology |
| :--- | :--- |
| **Language & Architecture** | 100% Kotlin, MVVM with StateFlow & Coroutines |
| **User Interface** | Jetpack Compose + Material 3 (Cyber Obsidian & Neon Cyan Theme) |
| **Wake Word Engine** | Picovoice Porcupine Android SDK (`ai.picovoice:porcupine-android`) |
| **Offline Speech-to-Text** | Vosk Android SDK (`com.alphacephei:vosk-android`) |
| **Voice Replies** | Native Android `TextToSpeech` (`android.speech.tts.TextToSpeech`) |
| **Command Engine** | Kotlin Rules Pattern |
| **Local AI Bridge** | On-device llama.cpp / Local LLM integration |
| **Build System** | Gradle 9.3.1 with Android Gradle Plugin 8.5.2 |

---

## 📂 Project Structure

```
c:\Lucifer\Saran\Agal\
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── kotlin/com/agal/assistant/
│   │   │   ├── AgalApp.kt
│   │   │   ├── MainActivity.kt
│   │   │   ├── wakeword/PorcupineWakeWordEngine.kt
│   │   │   ├── asr/VoskSpeechRecognizer.kt
│   │   │   ├── tts/AndroidVoiceSpeaker.kt
│   │   │   ├── engine/
│   │   │   │   ├── CommandEngine.kt
│   │   │   │   ├── CommandRule.kt
│   │   │   │   └── rules/
│   │   │   │       ├── MusicCommandRule.kt
│   │   │   │       ├── YouTubeCommandRule.kt
│   │   │   │       ├── BluetoothCommandRule.kt
│   │   │   │       ├── FlashlightCommandRule.kt
│   │   │   │       ├── CameraCommandRule.kt
│   │   │   │       ├── SystemSettingsRule.kt
│   │   │   │       ├── TimeDateRule.kt
│   │   │   │       ├── BatteryRule.kt
│   │   │   │       └── JokeRule.kt
│   │   │   ├── ai/LocalAiBridge.kt
│   │   │   ├── state/
│   │   │   │   ├── AssistantUiState.kt
│   │   │   │   └── AssistantViewModel.kt
│   │   │   └── ui/
│   │   │       ├── theme/{Color.kt, Theme.kt, Type.kt}
│   │   │       ├── components/{GlowingVoiceOrb.kt, QuickActionChip.kt, ConversationBubble.kt, PrivacyShieldBadge.kt}
│   │   │       └── screens/{MainAssistantScreen.kt, SettingsScreen.kt, CommandsGuideScreen.kt, PrivacyVaultScreen.kt}
│   │   └── res/
├── build.gradle.kts
├── settings.gradle.kts
└── gradlew.bat
```

---

## 🚀 Building & Running

### 1. In Android Studio:
1. Open **Android Studio**.
2. Select **Open** and choose `C:\Lucifer\Saran\Agal`.
3. Click **Sync Project with Gradle Files**.
4. Select your connected device or emulator (`samsung_a15`) and click **Run (Shift + F10)**.

### 2. From Command Line (Gradle):
```bash
# Build the Debug APK
.\gradlew.bat assembleDebug

# Install on connected device or emulator
.\gradlew.bat installDebug
```

The compiled APK is located at:
`app\build\outputs\apk\debug\app-debug.apk`
