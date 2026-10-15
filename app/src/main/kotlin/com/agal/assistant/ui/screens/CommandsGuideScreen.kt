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
import androidx.compose.runtime.Composable
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

data class CommandItem(val phrase: String, val description: String)
data class CategoryData(val title: String, val icon: ImageVector, val color: Color, val commands: List<CommandItem>)

@Composable
fun CommandsGuideScreen(
    viewModel: AssistantViewModel,
    onNavigateBack: () -> Unit
) {
    val categories = listOf(
        CategoryData(
            title = "Apps & Media",
            icon = Icons.Rounded.Apps,
            color = Color(0xFFFF0055),
            commands = listOf(
                CommandItem("Open YouTube", "Launches the native YouTube application with query search support"),
                CommandItem("Open Camera", "Launches camera ready to capture pictures"),
                CommandItem("Open Settings", "Opens device system settings")
            )
        ),
        CategoryData(
            title = "Hardware Controls",
            icon = Icons.Rounded.Tune,
            color = NeonCyan,
            commands = listOf(
                CommandItem("Turn on Bluetooth", "Opens Bluetooth system controls to activate Bluetooth"),
                CommandItem("Turn off Bluetooth", "Opens Bluetooth controls to disconnect or manage devices"),
                CommandItem("Turn on Flashlight", "Activates physical rear camera LED torch via CameraManager"),
                CommandItem("Turn off Flashlight", "Turns off hardware LED torch")
            )
        ),
        CategoryData(
            title = "Daily Utilities",
            icon = Icons.Rounded.AccessTime,
            color = EmeraldGreen,
            commands = listOf(
                CommandItem("What time is it?", "Speaks current local time with offline TTS"),
                CommandItem("What is today's date?", "Replies with full day, month, and year"),
                CommandItem("Battery status", "Reads on-device battery percentage via BatteryManager")
            )
        ),
        CategoryData(
            title = "Smart Interaction & AI",
            icon = Icons.Rounded.Psychology,
            color = NeonViolet,
            commands = listOf(
                CommandItem("Tell me a joke", "Speaks a witty tech joke from the offline humor bank"),
                CommandItem("Who are you?", "Explains Agal's zero-knowledge on-device architecture"),
                CommandItem("Help", "Summarizes supported voice commands")
            )
        )
    )

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Offline Voice Commands",
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
            categories.forEach { category ->
                item {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(category.color.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = category.icon,
                                    contentDescription = null,
                                    tint = category.color,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = category.title,
                                color = category.color,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        category.commands.forEach { cmd ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.7f)),
                                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "“${cmd.phrase}”",
                                            color = TextWhite,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = cmd.description,
                                            color = TextMuted,
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Button(
                                        onClick = {
                                            onNavigateBack()
                                            viewModel.simulateSpokenPhrase(cmd.phrase)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = category.color.copy(alpha = 0.18f)),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = category.color, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Try", color = category.color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(30.dp)) }
        }
    }
}
