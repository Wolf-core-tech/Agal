package com.agal.assistant

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.agal.assistant.service.AgalVoiceService
import com.agal.assistant.state.AssistantViewModel
import com.agal.assistant.ui.screens.*
import com.agal.assistant.ui.theme.AgalTheme
import com.agal.assistant.ui.theme.DarkBackground

enum class Screen {
    MAIN,
    SETTINGS,
    GUIDE,
    PRIVACY,
    CONFIGURATOR,
    PROFILE,
    LIFESTYLE
}

class MainActivity : ComponentActivity() {

    private val viewModel: AssistantViewModel by viewModels()

    private val requestAudioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startAgalVoiceService()
            viewModel.startWakeWordListening()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupLockScreenDisplay()
        checkAndRequestPermissions()

        setContent {
            AgalTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    var currentScreen by remember { mutableStateOf(Screen.MAIN) }

                    Crossfade(targetState = currentScreen, label = "screen_transition") { screen ->
                        when (screen) {
                            Screen.MAIN -> MainAssistantScreen(
                                viewModel = viewModel,
                                onNavigateToSettings = { currentScreen = Screen.SETTINGS },
                                onNavigateToGuide = { currentScreen = Screen.GUIDE },
                                onNavigateToPrivacy = { currentScreen = Screen.PRIVACY },
                                onNavigateToConfigurator = { currentScreen = Screen.CONFIGURATOR },
                                onNavigateToProfile = { currentScreen = Screen.PROFILE },
                                onNavigateToLifestyle = { currentScreen = Screen.LIFESTYLE }
                            )
                            Screen.PROFILE -> ProfileVaultScreen(
                                viewModel = viewModel,
                                onNavigateBack = { currentScreen = Screen.MAIN }
                            )
                            Screen.LIFESTYLE -> LifestyleHubScreen(
                                viewModel = viewModel,
                                onNavigateBack = { currentScreen = Screen.MAIN }
                            )
                            Screen.SETTINGS -> SettingsScreen(
                                viewModel = viewModel,
                                onNavigateBack = { currentScreen = Screen.MAIN }
                            )
                            Screen.GUIDE -> CommandsGuideScreen(
                                viewModel = viewModel,
                                onNavigateBack = { currentScreen = Screen.MAIN }
                            )
                            Screen.PRIVACY -> PrivacyVaultScreen(
                                viewModel = viewModel,
                                onNavigateBack = { currentScreen = Screen.MAIN }
                            )
                            Screen.CONFIGURATOR -> ConfiguratorScreen(
                                viewModel = viewModel,
                                onNavigateBack = { currentScreen = Screen.MAIN }
                            )
                        }
                    }
                }
            }
        }

        handleWakeIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleWakeIntent(intent)
    }

    private fun handleWakeIntent(intent: Intent?) {
        if (intent?.getBooleanExtra("WAKE_TRIGGERED", false) == true) {
            val keyword = intent.getStringExtra("KEYWORD") ?: "Hey Agal"
            viewModel.simulateSpokenPhrase(keyword)
        }
    }

    private fun setupLockScreenDisplay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }

    private fun startAgalVoiceService() {
        val serviceIntent = Intent(this, AgalVoiceService::class.java)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } catch (e: Exception) {
            // Service start exception
        }
    }

    private fun checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            startAgalVoiceService()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                registerForActivityResult(ActivityResultContracts.RequestPermission()) {}.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
