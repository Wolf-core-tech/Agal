@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.agal.assistant.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agal.assistant.R
import com.agal.assistant.state.AssistantStatus
import com.agal.assistant.state.AssistantUiState
import com.agal.assistant.state.AssistantViewModel
import com.agal.assistant.ui.components.*
import com.agal.assistant.ui.theme.*

@Composable
fun MainAssistantScreen(
    viewModel: AssistantViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToGuide: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToConfigurator: () -> Unit,
    onNavigateToCustomStudio: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = "Agal Logo",
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AGAL",
                            color = TextWhite,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToCustomStudio) {
                        Icon(Icons.Rounded.AutoFixHigh, contentDescription = "Custom Studio", tint = NeonViolet)
                    }
                    PrivacyShieldBadge(onClick = onNavigateToPrivacy)
                    IconButton(onClick = onNavigateToConfigurator) {
                        Icon(Icons.Rounded.Tune, contentDescription = "Configurator", tint = NeonCyan)
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = TextMuted)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        bottomBar = {
            BottomControlBar(
                uiState = uiState,
                inputText = inputText,
                onInputTextChanged = { inputText = it },
                onSendText = {
                    if (inputText.isNotBlank()) {
                        viewModel.simulateSpokenPhrase(inputText)
                        inputText = ""
                    }
                },
                onMicTap = {
                    if (uiState.status == AssistantStatus.LISTENING_COMMAND || uiState.status == AssistantStatus.LISTENING_WAKE_WORD) {
                        viewModel.setContinuousListening(false)
                    } else {
                        viewModel.startCommandListening()
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(14.dp))

                // Pulsing Voice Orb
                GlowingVoiceOrb(
                    status = uiState.status,
                    isSpeaking = uiState.isSpeaking,
                    onTap = {
                        if (uiState.status == AssistantStatus.IDLE) {
                            viewModel.startWakeWordListening()
                        } else {
                            viewModel.startCommandListening()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Active Message Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurface.copy(alpha = 0.85f))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = uiState.activeStatusMessage,
                            color = TextWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (uiState.lastRecognizedText.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "“${uiState.lastRecognizedText}”",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Custom Rules Studio Shortcut Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    DarkSurfaceVariant,
                                    NeonViolet.copy(alpha = 0.2f)
                                )
                            )
                        )
                        .border(1.dp, NeonViolet.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                        .clickable(onClick = onNavigateToCustomStudio)
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(NeonViolet.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoFixHigh,
                                    contentDescription = null,
                                    tint = NeonViolet,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Custom Words & Sentences",
                                    color = TextWhite,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Create custom phrases & assign phone actions",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = NeonViolet
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Quick Prompts Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "QUICK VOICE PROMPTS",
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    TextButton(onClick = onNavigateToGuide) {
                        Text(
                            text = "View All (12+)",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Horizontal Quick Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionChip(
                        label = "Open YouTube",
                        icon = Icons.Rounded.PlayArrow,
                        accentColor = Color(0xFFFF0055),
                        onClick = { viewModel.simulateSpokenPhrase("Open YouTube") }
                    )
                    QuickActionChip(
                        label = "Turn on Bluetooth",
                        icon = Icons.Rounded.Bluetooth,
                        accentColor = NeonCyan,
                        onClick = { viewModel.simulateSpokenPhrase("Turn on Bluetooth") }
                    )
                    QuickActionChip(
                        label = "Turn on Flashlight",
                        icon = Icons.Rounded.Highlight,
                        accentColor = NeonAmber,
                        onClick = { viewModel.simulateSpokenPhrase("Turn on Flashlight") }
                    )
                    QuickActionChip(
                        label = "What time is it?",
                        icon = Icons.Rounded.AccessTime,
                        accentColor = EmeraldGreen,
                        onClick = { viewModel.simulateSpokenPhrase("What time is it") }
                    )
                    QuickActionChip(
                        label = "Battery status",
                        icon = Icons.Rounded.BatteryChargingFull,
                        accentColor = CyanGlow,
                        onClick = { viewModel.simulateSpokenPhrase("Battery status") }
                    )
                    QuickActionChip(
                        label = "Tell me a joke",
                        icon = Icons.Rounded.SentimentSatisfied,
                        accentColor = NeonViolet,
                        onClick = { viewModel.simulateSpokenPhrase("Tell me a joke") }
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                if (uiState.history.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "RECENT CONVERSATIONS",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                        TextButton(onClick = { viewModel.clearHistory() }) {
                            Text(text = "Clear", color = TextMuted, fontSize = 11.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            items(uiState.history) { entry ->
                ConversationBubble(
                    entry = entry,
                    onReplayAudio = { text -> viewModel.processSpokenCommand(text) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun BottomControlBar(
    uiState: AssistantUiState,
    inputText: String,
    onInputTextChanged: (String) -> Unit,
    onSendText: () -> Unit,
    onMicTap: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkBackground)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // Wake Word Pill
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurfaceVariant)
                .border(1.dp, NeonCyan.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.RecordVoiceOver,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Wake Word: \"${uiState.activeWakeWord}\"",
                    color = NeonCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (uiState.isContinuousListening) "• ACTIVE" else "• PAUSED",
                    color = if (uiState.isContinuousListening) EmeraldGreen else TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Input Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            TextField(
                value = inputText,
                onValueChange = onInputTextChanged,
                placeholder = {
                    Text(
                        text = "Type command or say \"${uiState.activeWakeWord}\"...",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp)),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = DarkSurfaceVariant,
                    unfocusedContainerColor = DarkSurfaceVariant,
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    cursorColor = NeonCyan,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSendText() }),
                trailingIcon = {
                    IconButton(onClick = onSendText) {
                        Icon(Icons.Rounded.Send, contentDescription = "Send", tint = NeonCyan)
                    }
                },
                singleLine = true
            )

            Spacer(modifier = Modifier.width(10.dp))

            val isListening = uiState.status == AssistantStatus.LISTENING_COMMAND ||
                    uiState.status == AssistantStatus.LISTENING_WAKE_WORD

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            if (isListening) listOf(Color(0xFFFF0055), Color(0xFFFF5252))
                            else listOf(NeonCyan, CyanGlow)
                        )
                    )
                    .clickable(onClick = onMicTap),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Rounded.Stop else Icons.Rounded.Mic,
                    contentDescription = "Mic",
                    tint = DarkBackground,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}
