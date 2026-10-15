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
import androidx.compose.material.icons.automirrored.rounded.Send
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
    onNavigateToProfile: () -> Unit,
    onNavigateToLifestyle: () -> Unit
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
                            color = TextDark,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToLifestyle) {
                        Icon(Icons.Rounded.Luggage, contentDescription = "Trips & Finances", tint = NeonCyan)
                    }
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Rounded.AccountCircle, contentDescription = "Profile & Vault", tint = NeonAmber)
                    }
                    PrivacyShieldBadge(onClick = onNavigateToPrivacy)
                    IconButton(onClick = onNavigateToConfigurator) {
                        Icon(Icons.Rounded.Tune, contentDescription = "Configurator", tint = TextMuted)
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

                // Pulsing Voice Orb (Activates Hands-Free on 'Hey Agal')
                GlowingVoiceOrb(
                    status = uiState.status,
                    isSpeaking = uiState.isSpeaking,
                    onTap = {}
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
                            color = TextDark,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (uiState.lastRecognizedText.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "“${uiState.lastRecognizedText}”",
                                color = ElectricBlue,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scheduled Reminders Shortcut Card (Electric Blue & White)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    AppLightSurface,
                                    ElectricBlue.copy(alpha = 0.08f)
                                )
                            )
                        )
                        .border(1.dp, ElectricBlue.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                        .clickable { viewModel.simulateSpokenPhrase("Remember me to take my break in 10 minutes") }
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
                                    .background(ElectricBlue.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Alarm,
                                    contentDescription = null,
                                    tint = ElectricBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Smart Timed Reminders",
                                    color = TextDark,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Say \"Hey Agal, remember me to [task] in 10 minutes\"",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = ElectricBlue
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // HOME PAGE BUCKET LIST SECTION
                val bucketList by viewModel.bucketItems.collectAsState()
                var showAddGoalDialog by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.FormatListNumbered,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "MY BUCKET LIST (${bucketList.size})",
                            color = ElectricBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                    }
                    TextButton(onClick = { showAddGoalDialog = true }) {
                        Text("+ Add Goal", color = ElectricBlue, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (bucketList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(DarkSurfaceVariant)
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Say \"Add [goal] to bucket list\" to plan your dreams!",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        bucketList.forEachIndexed { index, item ->
                            val itemNumber = index + 1
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(DarkSurface.copy(alpha = 0.85f))
                                    .border(
                                        1.dp,
                                        if (item.isCompleted) EmeraldGreen.copy(alpha = 0.4f) else BorderSubtle,
                                        RoundedCornerShape(14.dp)
                                    )
                                    .clickable { viewModel.toggleBucketItem(item.id) }
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (item.isCompleted) EmeraldGreen.copy(alpha = 0.25f)
                                                    else NeonCyan.copy(alpha = 0.15f)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "$itemNumber",
                                                color = if (item.isCompleted) EmeraldGreen else NeonCyan,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = item.text,
                                                color = if (item.isCompleted) TextMuted else TextDark,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                style = if (item.isCompleted) androidx.compose.ui.text.TextStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough) else androidx.compose.ui.text.TextStyle()
                                            )
                                            if (item.easeAnalysis.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(3.dp))
                                                Text(
                                                    text = "💡 Ease: ${item.easeAnalysis}",
                                                    color = NeonAmber,
                                                    fontSize = 11.sp,
                                                    lineHeight = 15.sp
                                                )
                                            }
                                        }
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteBucketItem(item.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Delete",
                                            tint = TextMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (showAddGoalDialog) {
                    var goalInput by remember { mutableStateOf("") }
                    AlertDialog(
                        onDismissRequest = { showAddGoalDialog = false },
                        title = { Text("Add to Bucket List", color = TextDark, fontWeight = FontWeight.Bold) },
                        text = {
                            OutlinedTextField(
                                value = goalInput,
                                onValueChange = { goalInput = it },
                                label = { Text("Goal or Dream (e.g. Visit Japan, Read 10 books)", color = TextMuted) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextDark,
                                    unfocusedTextColor = TextDark,
                                    focusedBorderColor = ElectricBlue,
                                    unfocusedBorderColor = BorderLight
                                )
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (goalInput.isNotBlank()) {
                                        viewModel.addBucketItem(goalInput)
                                        showAddGoalDialog = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                            ) {
                                Text("Add Goal", color = TextWhite, fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showAddGoalDialog = false }) { Text("Cancel", color = TextMuted) }
                        },
                        containerColor = AppLightSurface
                    )
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
                    focusedContainerColor = AppLightSurfaceVariant,
                    unfocusedContainerColor = AppLightSurfaceVariant,
                    focusedTextColor = TextDark,
                    unfocusedTextColor = TextDark,
                    cursorColor = ElectricBlue,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSendText() }),
                trailingIcon = {
                    IconButton(onClick = onSendText) {
                        Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send", tint = ElectricBlue)
                    }
                },
                singleLine = true
            )

            Spacer(modifier = Modifier.width(10.dp))

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(DeepBlack)
                    .border(1.5.dp, ElectricBlue, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.RecordVoiceOver,
                    contentDescription = "Voice Active",
                    tint = ElectricBlue,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
