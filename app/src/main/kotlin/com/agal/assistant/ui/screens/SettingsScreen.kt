@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.agal.assistant.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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

    val presets = listOf("Hey Agal", "Agal", "Hey Assistant", "Jarvis", "Nova")

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Assistant Settings",
                        color = TextWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Rounded.ArrowBackIosNew, contentDescription = "Back", tint = TextWhite)
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
                // SECTION 1: CUSTOM WAKE WORD
                SectionHeader("WAKE WORD ACTIVATION", Icons.Rounded.RecordVoiceOver, NeonCyan)
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.85f)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NeonCyan.copy(alpha = 0.2f)))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Choose or customize the name you say to activate Agal without pressing any button.",
                            color = TextMuted,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Presets Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            presets.forEach { preset ->
                                val isSelected = uiState.activeWakeWord == preset
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setCustomWakeWord(preset) },
                                    label = { Text(preset, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NeonCyan.copy(alpha = 0.25f),
                                        selectedLabelColor = NeonCyan,
                                        containerColor = DarkSurfaceVariant,
                                        labelColor = TextMuted
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = if (isSelected) NeonCyan else Color.Transparent
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Custom Input
                        OutlinedTextField(
                            value = customWordInput,
                            onValueChange = {
                                customWordInput = it
                                viewModel.setCustomWakeWord(it)
                            },
                            label = { Text("Custom Wake Phrase", color = NeonCyan) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = BorderSubtle,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedContainerColor = DarkSurfaceVariant,
                                unfocusedContainerColor = DarkSurfaceVariant
                            ),
                            trailingIcon = {
                                IconButton(onClick = { viewModel.setCustomWakeWord(customWordInput) }) {
                                    Icon(Icons.Rounded.Check, contentDescription = "Save", tint = NeonCyan)
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedButton(
                            onClick = { viewModel.simulateSpokenPhrase(uiState.activeWakeWord) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(NeonCyan))
                        ) {
                            Icon(Icons.Rounded.Hearing, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Test Wake Word (\"${uiState.activeWakeWord}\")", color = NeonCyan)
                        }
                    }
                }
            }

            item {
                // SECTION 2: OFFLINE VOICE REPLIES
                SectionHeader("OFFLINE VOICE REPLIES (TEXT-TO-SPEECH)", Icons.Rounded.VolumeUp, NeonViolet)
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.85f)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NeonViolet.copy(alpha = 0.2f)))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Mute Voice Replies", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Switch(
                                checked = uiState.isMuted,
                                onCheckedChange = { viewModel.setMuted(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = NeonViolet, checkedTrackColor = NeonViolet.copy(alpha = 0.3f))
                            )
                        }

                        HorizontalDivider(color = BorderSubtle, modifier = Modifier.padding(vertical = 12.dp))

                        // Speech Rate
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Speech Speed", color = TextMuted, fontSize = 13.sp)
                            Text("${String.format("%.1f", uiState.speechRate)}x", color = NeonViolet, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = uiState.speechRate,
                            onValueChange = { viewModel.setSpeechRate(it) },
                            valueRange = 0.5f..2.0f,
                            steps = 6,
                            colors = SliderDefaults.colors(thumbColor = NeonViolet, activeTrackColor = NeonViolet)
                        )

                        // Pitch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Voice Pitch", color = TextMuted, fontSize = 13.sp)
                            Text(String.format("%.1f", uiState.pitch), color = NeonViolet, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = uiState.pitch,
                            onValueChange = { viewModel.setPitch(it) },
                            valueRange = 0.5f..1.5f,
                            steps = 5,
                            colors = SliderDefaults.colors(thumbColor = NeonViolet, activeTrackColor = NeonViolet)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { viewModel.speakSample() },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonViolet.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = NeonViolet)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Hear Sample Voice", color = NeonViolet, fontWeight = FontWeight.Bold)
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
                    colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.85f)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(EmeraldGreen.copy(alpha = 0.2f)))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Continuous Wake Word Listening", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
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
