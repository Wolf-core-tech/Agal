@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.agal.assistant.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agal.assistant.state.AssistantViewModel
import com.agal.assistant.ui.components.ConversationBubble
import com.agal.assistant.ui.theme.*

@Composable
fun PrivacyVaultScreen(
    viewModel: AssistantViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Privacy Vault & Diagnostics",
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
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                // HERO BANNER
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF0D2818), Color(0xFF04471C))
                            )
                        )
                        .border(1.5.dp, EmeraldGreen.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(EmeraldGreen.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.VerifiedUser,
                                contentDescription = null,
                                tint = EmeraldGreen,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "100% On-Device Privacy Guaranteed",
                            color = TextWhite,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "All voice recognition, wake-word detection, command parsing, and speech synthesis run directly on your hardware. Zero audio bytes ever leave this device.",
                            color = Color(0xFFB9F6CA),
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            item {
                // STATS GRID
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(title = "Cloud Data Sent", value = "0.00 KB", icon = Icons.Rounded.CloudOff, color = EmeraldGreen, modifier = Modifier.weight(1f))
                    MetricCard(title = "Audio Stream Leaks", value = "0 Packets", icon = Icons.Rounded.Lock, color = NeonCyan, modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(title = "Local Commands", value = "${uiState.totalCommandsExecuted}", icon = Icons.Rounded.OfflineBolt, color = NeonViolet, modifier = Modifier.weight(1f))
                    MetricCard(title = "Network Firewall", value = "OFFLINE SECURE", icon = Icons.Rounded.Security, color = NeonAmber, modifier = Modifier.weight(1f))
                }
            }

            item {
                // DATA PIPELINE
                Text(
                    text = "OFFLINE DATA PIPELINE",
                    color = NeonCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.85f)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        PipelineStep(1, "Microphone Audio Stream", "Direct PCM sampling into ephemeral RAM buffer only", Icons.Rounded.Mic)
                        PipelineArrow()
                        PipelineStep(2, "Picovoice Porcupine & Vosk", "On-device acoustic neural models recognize wake word & phonemes", Icons.Rounded.Memory)
                        PipelineArrow()
                        PipelineStep(3, "Kotlin Rules Engine", "Executes system intents on phone with zero cloud API keys", Icons.Rounded.Terminal)
                        PipelineArrow()
                        PipelineStep(4, "Android TextToSpeech", "Native on-device speech engine speaks replies offline", Icons.Rounded.VolumeUp)
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LOCAL AUDIT LOG (ON-DEVICE ONLY)",
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    if (uiState.history.isNotEmpty()) {
                        TextButton(onClick = { viewModel.clearHistory() }) {
                            Text("Wipe Logs", color = ErrorRed, fontSize = 12.sp)
                        }
                    }
                }
            }

            if (uiState.history.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(DarkSurface.copy(alpha = 0.5f))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No local command logs yet.\nSpeak a command to see it recorded locally.",
                            color = TextMuted,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(uiState.history) { entry ->
                    ConversationBubble(
                        entry = entry,
                        onReplayAudio = { text -> viewModel.processSpokenCommand(text) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(30.dp)) }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.85f)),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(color.copy(alpha = 0.25f)))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(10.dp))
            Text(value, color = color, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(title, color = TextMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun PipelineStep(step: Int, title: String, subtitle: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1E293B)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text("$step. $title", color = TextDark, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = TextMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun PipelineArrow() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Rounded.ArrowDownward, contentDescription = null, tint = TextMuted.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
    }
}
