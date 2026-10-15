package com.agal.assistant.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// White, Electric Blue, and 20% Deep Black Precision Color Scheme
private val AgalColorScheme = lightColorScheme(
    primary = ElectricBlue,
    secondary = DeepBlack,
    tertiary = NeonCyan,
    background = AppOffWhite,
    surface = AppLightSurface,
    surfaceVariant = AppLightSurfaceVariant,
    onPrimary = TextWhite,
    onSecondary = TextWhite,
    onBackground = TextDark,
    onSurface = TextDark,
    error = ErrorRed
)

@Composable
fun AgalTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AgalColorScheme,
        typography = Typography,
        content = content
    )
}
