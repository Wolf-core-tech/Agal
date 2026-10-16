@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.agal.assistant.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agal.assistant.state.AssistantViewModel
import com.agal.assistant.ui.theme.*

@Composable
fun SettingsScreen(
    viewModel: AssistantViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var customWordInput by remember { mutableStateOf(uiState.activeWakeWord) }
    var customReplyInput by remember { mutableStateOf(uiState.listeningReply) }

    val wakeWordPresets = listOf("Hey Agal", "Agal", "வணக்கம் அகல்", "அகல்", "Hey Assistant", "Jarvis")
    val replyPresetsEn = listOf("Yes Venkat", "Yes Venkat, I'm listening.", "Yes, I'm here.", "How can I help you, Venkat?", "I'm listening.")
    val replyPresetsTa = listOf("சொல்லுங்க, கேட்கிறேன்.", "நான் தயாராக இருக்கிறேன்.", "உங்களுக்கு என்ன வேண்டும்?")

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Assistant Settings",
                        color = TextDark,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
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
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                // SECTION 1: CUSTOM WAKE WORD (PHRASE)
                SectionHeader("WAKE WORD ACTIVATION (ENGLISH & TAMIL)", Icons.Rounded.RecordVoiceOver, ElectricBlue)
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AppLightSurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderLight))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Choose or type the name you say to activate Agal (English or Tamil).",
                            color = TextMuted,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Presets Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            wakeWordPresets.forEach { preset ->
                                val isSelected = uiState.activeWakeWord == preset
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        viewModel.setCustomWakeWord(preset)
                                        customWordInput = preset
                                    },
                                    label = { Text(preset, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ElectricBlue.copy(alpha = 0.15f),
                                        selectedLabelColor = ElectricBlue,
                                        containerColor = AppLightSurfaceVariant,
                                        labelColor = TextMuted
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = if (isSelected) ElectricBlue else Color.Transparent
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Custom Input
                        OutlinedTextField(
                            value = customWordInput,
                            onValueChange = { customWordInput = it },
                            label = { Text("Custom Wake Phrase", color = ElectricBlue) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricBlue,
                                unfocusedBorderColor = BorderLight,
                                focusedTextColor = TextDark,
                                unfocusedTextColor = TextDark,
                                focusedContainerColor = AppLightSurfaceVariant,
                                unfocusedContainerColor = AppLightSurfaceVariant
                            ),
                            trailingIcon = {
                                IconButton(onClick = { viewModel.setCustomWakeWord(customWordInput) }) {
                                    Icon(Icons.Rounded.Check, contentDescription = "Save", tint = ElectricBlue)
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedButton(
                            onClick = { viewModel.simulateSpokenPhrase(uiState.activeWakeWord) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = androidx.compose.ui.graphics.SolidColor(ElectricBlue))
                        ) {
                            Icon(Icons.Rounded.Hearing, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Test Wake Word (\"${uiState.activeWakeWord}\")", color = ElectricBlue)
                        }
                    }
                }
            }

            item {
                // SECTION: CUSTOM LISTENING RESPONSE (INSTEAD OF "I'M LISTENING")
                SectionHeader("WAKE RESPONSE WORD (ENGLISH & தமிழ்)", Icons.Rounded.SpatialAudio, NeonAmber)
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AppLightSurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderLight))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Customize what Agal speaks back when awoken (instead of \"I'm listening\"). Supports both English and Tamil.",
                            color = TextMuted,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // English Presets
                        Text(text = "ENGLISH PRESETS", color = ElectricBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            replyPresetsEn.forEach { preset ->
                                val isSelected = uiState.listeningReply == preset
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        viewModel.setCustomListeningReply(preset)
                                        customReplyInput = preset
                                    },
                                    label = { Text(preset, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ElectricBlue.copy(alpha = 0.15f),
                                        selectedLabelColor = ElectricBlue,
                                        containerColor = AppLightSurfaceVariant,
                                        labelColor = TextMuted
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Tamil Presets
                        Text(text = "தமிழ் மொழியில் (TAMIL PRESETS)", color = NeonAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            replyPresetsTa.forEach { preset ->
                                val isSelected = uiState.listeningReply == preset
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        viewModel.setCustomListeningReply(preset)
                                        customReplyInput = preset
                                    },
                                    label = { Text(preset, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NeonAmber.copy(alpha = 0.2f),
                                        selectedLabelColor = NeonAmber,
                                        containerColor = AppLightSurfaceVariant,
                                        labelColor = TextMuted
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Custom Reply Input
                        OutlinedTextField(
                            value = customReplyInput,
                            onValueChange = { customReplyInput = it },
                            label = { Text("Custom Reply (e.g. சொல்லுங்க or Yes boss)", color = ElectricBlue) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricBlue,
                                unfocusedBorderColor = BorderLight,
                                focusedTextColor = TextDark,
                                unfocusedTextColor = TextDark,
                                focusedContainerColor = AppLightSurfaceVariant,
                                unfocusedContainerColor = AppLightSurfaceVariant
                            ),
                            trailingIcon = {
                                IconButton(onClick = { viewModel.setCustomListeningReply(customReplyInput) }) {
                                    Icon(Icons.Rounded.Check, contentDescription = "Save", tint = ElectricBlue)
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedButton(
                            onClick = { viewModel.speakSample() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = androidx.compose.ui.graphics.SolidColor(ElectricBlue))
                        ) {
                            Icon(Icons.Rounded.VolumeUp, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Hear Current Reply (\"${uiState.listeningReply}\")", color = ElectricBlue)
                        }
                    }
                }
            }

            item {
                // SECTION: SMART REMINDERS
                SectionHeader("TIMED REMINDERS & ALARMS", Icons.Rounded.Alarm, ElectricBlue)
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AppLightSurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderLight))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ElectricBlue.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Alarm,
                                    contentDescription = null,
                                    tint = ElectricBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Hands-Free Voice Reminders",
                                    color = TextDark,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "AlarmManager-backed exact timed alarms & notifications",
                                    color = TextMuted,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Say: \"Remember me to attend meeting in 15 minutes\" or \"Remind me to call Rahul at 6 PM\". Agal will alert you on time even when screen is off.",
                            color = TextMuted,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            item {
                // SECTION 2: OFFLINE VOICE REPLIES
                SectionHeader("OFFLINE VOICE REPLIES (NATURAL FEMALE TTS)", Icons.Rounded.VolumeUp, ElectricBlue)
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AppLightSurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderLight))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Mute Voice Replies", color = TextDark, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Switch(
                                checked = uiState.isMuted,
                                onCheckedChange = { viewModel.setMuted(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = ElectricBlue, checkedTrackColor = ElectricBlue.copy(alpha = 0.3f))
                            )
                        }

                        HorizontalDivider(color = BorderSubtle, modifier = Modifier.padding(vertical = 12.dp))

                        // Speech Rate
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Speech Speed", color = TextMuted, fontSize = 13.sp)
                            Text("${String.format("%.1f", uiState.speechRate)}x", color = ElectricBlue, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = uiState.speechRate,
                            onValueChange = { viewModel.setSpeechRate(it) },
                            valueRange = 0.5f..2.0f,
                            steps = 6,
                            colors = SliderDefaults.colors(thumbColor = ElectricBlue, activeTrackColor = ElectricBlue)
                        )

                        // Pitch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Voice Warmth & Pitch", color = TextMuted, fontSize = 13.sp)
                            Text(String.format("%.1f", uiState.pitch), color = ElectricBlue, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = uiState.pitch,
                            onValueChange = { viewModel.setPitch(it) },
                            valueRange = 0.5f..1.5f,
                            steps = 5,
                            colors = SliderDefaults.colors(thumbColor = ElectricBlue, activeTrackColor = ElectricBlue)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { viewModel.speakSample() },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = TextWhite)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Hear Natural Female Voice", color = TextWhite, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                // SECTION 3: LISTENING ENGINE & PRIVACY
                SectionHeader("LISTENING & HARDWARE ENGINE", Icons.Rounded.Hearing, EmeraldGreen)
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AppLightSurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderLight))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Continuous Wake Word Listening", color = TextDark, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text("Microphone spots wake word in background", color = TextMuted, fontSize = 12.sp)
                            }
                            Switch(
                                checked = uiState.isContinuousListening,
                                onCheckedChange = { viewModel.setContinuousListening(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = EmeraldGreen, checkedTrackColor = EmeraldGreen.copy(alpha = 0.3f))
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: ImageVector, color: Color) {
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
