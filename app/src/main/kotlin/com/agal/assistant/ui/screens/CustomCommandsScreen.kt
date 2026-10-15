@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.agal.assistant.ui.screens

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agal.assistant.custom.CustomActionType
import com.agal.assistant.custom.CustomVoiceCommand
import com.agal.assistant.state.AssistantViewModel
import com.agal.assistant.ui.theme.*

@Composable
fun CustomCommandsScreen(
    viewModel: AssistantViewModel,
    onNavigateBack: () -> Unit
) {
    val customCommands by viewModel.customCommands.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "CUSTOM VOICE STUDIO",
                            color = TextWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            text = "Create your own words, sentences & actions",
                            color = NeonCyan,
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = TextWhite)
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Rounded.AddCircle, contentDescription = "Add", tint = NeonCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = NeonCyan,
                contentColor = DarkBackground,
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("New Custom Rule", fontWeight = FontWeight.Bold) }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                HeroInfoCard(totalRules = customCommands.size)
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "REGISTERED CUSTOM TRIGGERS",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "${customCommands.count { it.isEnabled }} Active",
                        color = EmeraldGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            items(customCommands, key = { it.id }) { command ->
                CustomCommandCard(
                    command = command,
                    onToggle = { isEnabled -> viewModel.toggleCustomCommand(command.id, isEnabled) },
                    onDelete = { viewModel.deleteCustomCommand(command.id) },
                    onTest = { viewModel.testCustomCommand(command) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    if (showCreateDialog) {
        CreateCustomCommandDialog(
            onDismiss = { showCreateDialog = false },
            onSave = { newCmd ->
                viewModel.addCustomCommand(newCmd)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun HeroInfoCard(totalRules: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        NeonCyan.copy(alpha = 0.12f),
                        NeonViolet.copy(alpha = 0.15f)
                    )
                )
            )
            .border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(NeonCyan.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoFixHigh,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Custom Words & Routines",
                        color = TextWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Say your trigger phrase anytime — even on lock screen!",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = BorderSubtle)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "💡 Tip: Tap \"Test\" to try an action instantly",
                    color = NeonAmber,
                    fontSize = 11.sp
                )
                Text(
                    text = "$totalRules Rules Saved",
                    color = TextWhite,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun CustomCommandCard(
    command: CustomVoiceCommand,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onTest: () -> Unit
) {
    val (actionIcon, actionColor) = when (command.actionType) {
        CustomActionType.TOGGLE_FLASHLIGHT -> Pair(Icons.Rounded.Highlight, NeonAmber)
        CustomActionType.SET_VOLUME -> Pair(Icons.Rounded.VolumeUp, CyanGlow)
        CustomActionType.OPEN_APP -> Pair(Icons.Rounded.Apps, Color(0xFFFF0055))
        CustomActionType.FIND_MY_PHONE -> Pair(Icons.Rounded.NotificationsActive, Color(0xFFFF4081))
        CustomActionType.BLUETOOTH_ACTION -> Pair(Icons.Rounded.Bluetooth, NeonCyan)
        CustomActionType.SPEAK_REPLY -> Pair(Icons.Rounded.RecordVoiceOver, EmeraldGreen)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
            .border(
                1.dp,
                if (command.isEnabled) BorderSubtle else BorderSubtle.copy(alpha = 0.3f),
                RoundedCornerShape(16.dp)
            )
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(actionColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = actionIcon,
                            contentDescription = null,
                            tint = actionColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = command.title.ifBlank { command.triggerPhrase },
                            color = if (command.isEnabled) TextWhite else TextMuted,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = command.actionType.displayName,
                            color = actionColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Switch(
                    checked = command.isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = NeonCyan,
                        checkedTrackColor = NeonCyan.copy(alpha = 0.3f),
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = DarkSurfaceVariant
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Trigger phrase pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurfaceVariant)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Mic,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Say: \"${command.triggerPhrase}\"",
                        color = NeonCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (command.spokenReply.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.VolumeUp,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Replies: \"${command.spokenReply}\"",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = BorderSubtle.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            // Actions row: Test button and Delete button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onTest,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Test Rule", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Delete",
                        tint = Color(0xFFFF5252).copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateCustomCommandDialog(
    onDismiss: () -> Unit,
    onSave: (CustomVoiceCommand) -> Unit
) {
    var triggerPhrase by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var selectedActionType by remember { mutableStateOf(CustomActionType.SPEAK_REPLY) }
    var actionPayload by remember { mutableStateOf("") }
    var spokenReply by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.AutoFixHigh,
                    contentDescription = null,
                    tint = NeonCyan
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "New Custom Rule",
                    color = TextWhite,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Trigger Phrase
                Text(
                    text = "TRIGGER WORD OR SENTENCE",
                    color = NeonCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                OutlinedTextField(
                    value = triggerPhrase,
                    onValueChange = { triggerPhrase = it },
                    placeholder = { Text("e.g. party time, good morning, study mode", color = TextMuted, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = BorderSubtle
                    ),
                    singleLine = true
                )

                // 2. Rule Title
                Text(
                    text = "TITLE / NICKNAME",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("e.g. Party Mode", color = TextMuted, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = BorderSubtle
                    ),
                    singleLine = true
                )

                // 3. Action Type Selector
                Text(
                    text = "PHONE ACTION TO PERFORM",
                    color = NeonAmber,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CustomActionType.values().forEach { type ->
                        val isSelected = selectedActionType == type
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedActionType = type
                                if (type == CustomActionType.TOGGLE_FLASHLIGHT) actionPayload = "ON"
                                if (type == CustomActionType.SET_VOLUME) actionPayload = "MUTE"
                                if (type == CustomActionType.OPEN_APP) actionPayload = "youtube"
                                if (type == CustomActionType.FIND_MY_PHONE) actionPayload = "ALERT"
                            },
                            label = { Text(type.displayName, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonCyan,
                                selectedLabelColor = DarkBackground,
                                containerColor = DarkSurfaceVariant,
                                labelColor = TextWhite
                            )
                        )
                    }
                }

                // Sub-options based on action
                when (selectedActionType) {
                    CustomActionType.TOGGLE_FLASHLIGHT -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = actionPayload == "ON",
                                onClick = { actionPayload = "ON" },
                                label = { Text("Turn ON Torch") }
                            )
                            FilterChip(
                                selected = actionPayload == "OFF",
                                onClick = { actionPayload = "OFF" },
                                label = { Text("Turn OFF Torch") }
                            )
                        }
                    }
                    CustomActionType.SET_VOLUME -> {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = actionPayload == "MUTE",
                                onClick = { actionPayload = "MUTE" },
                                label = { Text("Mute (0%)") }
                            )
                            FilterChip(
                                selected = actionPayload == "HALF",
                                onClick = { actionPayload = "HALF" },
                                label = { Text("Half (50%)") }
                            )
                            FilterChip(
                                selected = actionPayload == "MAX",
                                onClick = { actionPayload = "MAX" },
                                label = { Text("Max (100%)") }
                            )
                        }
                    }
                    CustomActionType.OPEN_APP -> {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = actionPayload == "youtube",
                                onClick = { actionPayload = "youtube" },
                                label = { Text("YouTube") }
                            )
                            FilterChip(
                                selected = actionPayload == "music",
                                onClick = { actionPayload = "music" },
                                label = { Text("Music") }
                            )
                            FilterChip(
                                selected = actionPayload == "camera",
                                onClick = { actionPayload = "camera" },
                                label = { Text("Camera") }
                            )
                            FilterChip(
                                selected = actionPayload == "whatsapp",
                                onClick = { actionPayload = "whatsapp" },
                                label = { Text("WhatsApp") }
                            )
                        }
                    }
                    else -> {}
                }

                // 4. Custom Voice Reply
                Text(
                    text = "AGAL VOICE REPLY (OPTIONAL)",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                OutlinedTextField(
                    value = spokenReply,
                    onValueChange = { spokenReply = it },
                    placeholder = { Text("What should Agal say back?", color = TextMuted, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = BorderSubtle
                    ),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (triggerPhrase.isNotBlank()) {
                        onSave(
                            CustomVoiceCommand(
                                triggerPhrase = triggerPhrase.trim(),
                                title = title.ifBlank { triggerPhrase.trim() },
                                actionType = selectedActionType,
                                actionPayload = actionPayload,
                                spokenReply = spokenReply.trim()
                            )
                        )
                    }
                },
                enabled = triggerPhrase.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = DarkBackground)
            ) {
                Text("Save Custom Rule", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        }
    )
}
