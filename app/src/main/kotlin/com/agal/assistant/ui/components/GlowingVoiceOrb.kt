package com.agal.assistant.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agal.assistant.state.AssistantStatus
import com.agal.assistant.ui.theme.*
import kotlin.math.sin

@Composable
fun GlowingVoiceOrb(
    status: AssistantStatus,
    isSpeaking: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing)
        ),
        label = "wave_offset"
    )

    val (primaryColor, secondaryColor, statusLabel) = when (status) {
        AssistantStatus.LISTENING_WAKE_WORD -> Triple(NeonCyan, CyanGlow, "LISTENING FOR WAKE WORD")
        AssistantStatus.WAKE_WORD_TRIGGERED -> Triple(NeonPink, ElectricPurple, "AWOKEN • READY")
        AssistantStatus.LISTENING_COMMAND -> Triple(EmeraldGreen, NeonCyan, "LISTENING TO COMMAND...")
        AssistantStatus.PROCESSING -> Triple(NeonAmber, Color(0xFFFF5722), "PROCESSING OFFLINE")
        AssistantStatus.SPEAKING_REPLY -> Triple(NeonViolet, ElectricPurple, "SPEAKING OFFLINE REPLY")
        AssistantStatus.ERROR -> Triple(ErrorRed, Color(0xFFFF5252), "ERROR")
        AssistantStatus.IDLE -> Triple(NeonCyan.copy(alpha = 0.7f), ElectricPurple.copy(alpha = 0.5f), "TAP OR SAY \"HEY AGAL\"")
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onTap
                ),
            contentAlignment = Alignment.Center
        ) {
            // Animated Ripples Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                val baseRadius = 55.dp.toPx()

                // Outer wave 1
                val r1 = baseRadius + (waveOffset * 35.dp.toPx())
                val alpha1 = (1f - waveOffset).coerceIn(0f, 1f) * 0.45f
                drawCircle(
                    color = primaryColor.copy(alpha = alpha1),
                    radius = r1,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )

                // Outer wave 2
                val w2 = (waveOffset + 0.5f) % 1f
                val r2 = baseRadius + (w2 * 28.dp.toPx())
                val alpha2 = (1f - w2).coerceIn(0f, 1f) * 0.35f
                drawCircle(
                    color = secondaryColor.copy(alpha = alpha2),
                    radius = r2,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }

            // Glow Aura Box
            Box(
                modifier = Modifier
                    .size((130 * pulseScale).dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.45f),
                                secondaryColor.copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Inner Orb Core
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF1E293B),
                                Color(0xFF0F172A)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSpeaking) {
                    EqualizerBars(primaryColor)
                } else {
                    val icon = when (status) {
                        AssistantStatus.LISTENING_WAKE_WORD -> Icons.Rounded.Mic
                        AssistantStatus.WAKE_WORD_TRIGGERED -> Icons.Rounded.Hearing
                        AssistantStatus.LISTENING_COMMAND -> Icons.Rounded.Mic
                        AssistantStatus.PROCESSING -> Icons.Rounded.Bolt
                        AssistantStatus.SPEAKING_REPLY -> Icons.Rounded.VolumeUp
                        AssistantStatus.ERROR -> Icons.Rounded.Warning
                        AssistantStatus.IDLE -> Icons.Rounded.MicNone
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Status Badge Pill
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(primaryColor.copy(alpha = 0.12f))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(primaryColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = statusLabel,
                    color = primaryColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
            }
        }
    }
}

@Composable
private fun EqualizerBars(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "equalizer")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing)
        ),
        label = "eq_time"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(4) { index ->
            val height = (12f + (sin(time + index * 0.8f) + 1f) * 8f).dp
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(height)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}
