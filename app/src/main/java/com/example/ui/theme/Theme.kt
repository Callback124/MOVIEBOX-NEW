package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MovieBoxColorScheme = darkColorScheme(
    primary = MovieBoxRed,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3B0005),
    onPrimaryContainer = Color(0xFFFFDADA),
    secondary = MovieBoxGold,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF3D2C00),
    onSecondaryContainer = Color(0xFFFFE088),
    tertiary = MovieBoxPurple,
    onTertiary = Color.White,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkSurfaceBorder,
    outlineVariant = Color(0xFF1F1E2C)
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MovieBoxColorScheme,
        typography = Typography,
        content = content
    )
}
