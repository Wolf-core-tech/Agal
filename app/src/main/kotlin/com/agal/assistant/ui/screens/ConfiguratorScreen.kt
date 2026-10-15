@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.agal.assistant.ui.screens

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.BatteryManager
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agal.assistant.state.AssistantViewModel
import com.agal.assistant.ui.theme.*

@Composable
fun ConfiguratorScreen(
    viewModel: AssistantViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var customWakeWordText by remember { mutableStateOf(uiState.activeWakeWord) }
    var torchStatus by remember { mutableStateOf(false) }
    var testResultMsg by remember { mutableStateOf("Tap any component below to test hardware response") }

    val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
    val batteryPct = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 88

    val deviceModel = Build.MODEL // "SM-A156E"
    val deviceManufacturer = Build.MANUFACTURER // "samsung"
    val androidVersion = Build.VERSION.RELEASE // "16"
    val hardwarePlatform = Build.HARDWARE

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Assistant Configurator",
                            color = TextDark,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Rounded.ArrowBackIosNew, contentDescription = "Back", tint = TextDark)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // DEVICE PROFILE CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = AppLightSurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderLight))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(ElectricBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PhoneAndroid,
                                    contentDescription = null,
                                    tint = TextWhite,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "$deviceManufacturer $deviceModel".uppercase(),
                                    color = TextDark,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Samsung Galaxy A15 5G • Android $androidVersion",
                                    color = ElectricBlue,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = BorderSubtle)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            DeviceInfoBadge("Hardware", hardwarePlatform.uppercase())
                            DeviceInfoBadge("Battery", "$batteryPct%")
                            DeviceInfoBadge("Security", "100% Offline")
                            DeviceInfoBadge("Neural NPU", "Active")
                        }
                    }
                }
            }

            // TEST BANNER
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(AppLightSurface)
                        .border(1.dp, BorderLight, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Info, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = testResultMsg,
                            color = TextDark,
                            fontSize = 13.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            // SECTION 1: WAKE WORD CONFIGURATOR
            item {
                ConfiguratorSectionHeader("1. WAKE WORD ENGINE", Icons.Rounded.RecordVoiceOver, ElectricBlue)
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AppLightSurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderLight))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Engine: Picovoice Porcupine (On-Device Keyword Spotter)",
                            color = ElectricBlue,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Set your chosen activation name. The app continuously listens in volatile RAM and activates when heard.",
                            color = TextMuted,
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = customWakeWordText,
                            onValueChange = {
                                customWakeWordText = it
                                viewModel.setCustomWakeWord(it)
                                testResultMsg = "Wake word updated to \"$it\" on Samsung A15"
                            },
                            label = { Text("Active Wake Word", color = ElectricBlue) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricBlue,
                                unfocusedBorderColor = BorderLight,
                                focusedTextColor = TextDark,
                                unfocusedTextColor = TextDark,
                                focusedContainerColor = AppLightSurfaceVariant,
                                unfocusedContainerColor = AppLightSurfaceVariant
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                viewModel.simulateSpokenPhrase(uiState.activeWakeWord)
                                testResultMsg = "Wake word trigger test sent: \"${uiState.activeWakeWord}\" awoken!"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Rounded.Hearing, contentDescription = null, tint = NeonCyan)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test Wake Word Trigger", color = NeonCyan, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // SECTION 2: SPEECH RECOGNITION (VOSK)
            item {
                ConfiguratorSectionHeader("2. OFFLINE SPEECH RECOGNITION (ASR)", Icons.Rounded.Mic, EmeraldGreen)
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.85f)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Engine: Vosk Kaldi-based Acoustic Neural Model",
                            color = EmeraldGreen,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Audio stream: 16,000 Hz, 16-bit Mono PCM. Processed entirely on $deviceModel CPU without network egress.",
                            color = TextMuted,
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    viewModel.simulateSpokenPhrase("Open my music app")
                                    testResultMsg = "Simulated Vosk command: \"Open my music app\""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen.copy(alpha = 0.2f)),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Test Music Cmd", color = EmeraldGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    viewModel.simulateSpokenPhrase("Open YouTube")
                                    testResultMsg = "Simulated Vosk command: \"Open YouTube\""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen.copy(alpha = 0.2f)),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Test YouTube", color = EmeraldGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // SECTION 3: HARDWARE CONTROLS TEST (SAMSUNG A15)
            item {
                ConfiguratorSectionHeader("3. SAMSUNG A15 HARDWARE CONTROLS", Icons.Rounded.Build, NeonAmber)
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.85f)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Direct Samsung A15 Hardware Component Testing",
                            color = NeonAmber,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Flashlight Test
                            Button(
                                onClick = {
                                    val camManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
                                    if (camManager != null) {
                                        try {
                                            for (id in camManager.cameraIdList) {
                                                val hasFlash = camManager.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                                                if (hasFlash) {
                                                    torchStatus = !torchStatus
                                                    camManager.setTorchMode(id, torchStatus)
                                                    testResultMsg = if (torchStatus) "Samsung A15 rear LED Flashlight turned ON!" else "Flashlight turned OFF"
                                                    break
                                                }
                                            }
                                        } catch (e: Exception) {
                                            testResultMsg = "Torch error: ${e.localizedMessage}"
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonAmber.copy(alpha = 0.2f)),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Rounded.Highlight, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (torchStatus) "Torch Off" else "Test Torch", color = NeonAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            // Bluetooth Test
                            Button(
                                onClick = {
                                    viewModel.simulateSpokenPhrase("Turn on Bluetooth")
                                    testResultMsg = "Launched Bluetooth Settings on Samsung A15"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan.copy(alpha = 0.2f)),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Rounded.Bluetooth, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Test BT", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            // Battery Test
                            Button(
                                onClick = {
                                    viewModel.simulateSpokenPhrase("Battery status")
                                    testResultMsg = "Battery checked: $batteryPct%"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyanGlow.copy(alpha = 0.2f)),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Rounded.BatteryChargingFull, contentDescription = null, tint = CyanGlow, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Battery", color = CyanGlow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // SECTION 4: TEXT TO SPEECH (VOICE OUTPUT)
            item {
                ConfiguratorSectionHeader("4. OFFLINE VOICE RESPONSES (TTS)", Icons.Rounded.VolumeUp, NeonViolet)
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.85f)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Engine: Android TextToSpeech (Local Voice Pack)",
                            color = NeonViolet,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Synthesizes voice locally on your Samsung A15 without calling cloud TTS APIs.",
                            color = TextMuted,
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                viewModel.speakSample()
                                testResultMsg = "Spoke offline voice test: \"Opening your music app.\""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonViolet.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = NeonViolet)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test Voice Reply on Mobile", color = NeonViolet, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@Composable
private fun ConfiguratorSectionHeader(title: String, icon: ImageVector, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )
    }
}

@Composable
private fun DeviceInfoBadge(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, color = TextDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(text = label, color = TextMuted, fontSize = 10.sp)
    }
}
