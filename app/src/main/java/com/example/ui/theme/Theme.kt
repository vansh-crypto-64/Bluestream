package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkBlueColorScheme = darkColorScheme(
    primary = BlueStreamPrimaryLight,
    onPrimary = BlueStreamOnPrimaryDark,
    primaryContainer = BlueStreamPrimaryContainer,
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = BlueStreamSecondary,
    onSecondary = BlueStreamOnSecondary,
    secondaryContainer = BlueStreamSecondaryContainer,
    onSecondaryContainer = Color(0xFFE0F2FE),
    tertiary = BlueStreamTertiary,
    onTertiary = Color(0xFF1E1B4B),
    tertiaryContainer = BlueStreamTertiaryContainer,
    onTertiaryContainer = Color(0xFFE0E7FF),
    background = BlueStreamDarkBackground,
    onBackground = BlueStreamDarkText,
    surface = BlueStreamDarkSurface,
    onSurface = BlueStreamDarkText,
    surfaceVariant = BlueStreamDarkSurfaceVariant,
    onSurfaceVariant = BlueStreamDarkTextMuted,
    surfaceContainer = BlueStreamDarkSurfaceContainer,
    surfaceContainerHigh = BlueStreamDarkSurfaceContainerHigh,
    outline = BlueStreamDarkOutline,
    outlineVariant = Color(0xFF1A2E57),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A)
)

private val LightBlueColorScheme = lightColorScheme(
    primary = BlueStreamPrimary,
    onPrimary = BlueStreamOnPrimaryLight,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = Color(0xFF0284C7),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0C4A6E),
    tertiary = Color(0xFF4F46E5),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE0E7FF),
    onTertiaryContainer = Color(0xFF312E81),
    background = BlueStreamLightBackground,
    onBackground = BlueStreamLightText,
    surface = BlueStreamLightSurface,
    onSurface = BlueStreamLightText,
    surfaceVariant = BlueStreamLightSurfaceVariant,
    onSurfaceVariant = BlueStreamLightTextMuted,
    surfaceContainer = BlueStreamLightSurfaceContainer,
    surfaceContainerHigh = BlueStreamLightSurfaceContainerHigh,
    outline = BlueStreamLightOutline,
    outlineVariant = Color(0xFFD6E4FF),
    error = Color(0xFFDC2626),
    onError = Color(0xFFFFFFFF)
)

val BlueStreamShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun BlueStreamTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkBlueColorScheme else LightBlueColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = BlueStreamTypography,
        shapes = BlueStreamShapes,
        content = content
    )
}
