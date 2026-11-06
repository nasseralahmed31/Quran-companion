package com.qurancompanion.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Emerald400,
    onPrimary = Color(0xFF00382A),
    primaryContainer = Emerald800,
    onPrimaryContainer = Color(0xFFB7F5DC),
    secondary = Gold,
    onSecondary = Color(0xFF3A2D00),
    background = NightBackground,
    onBackground = Color(0xFFE6EBE8),
    surface = NightSurface,
    onSurface = Color(0xFFE6EBE8),
    surfaceVariant = NightSurfaceVariant,
    onSurfaceVariant = Color(0xFFB4C2BC),
    outline = Color(0xFF3B4A44),
)

private val LightColors = lightColorScheme(
    primary = Emerald700,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCDF5E4),
    onPrimaryContainer = Emerald900,
    secondary = GoldDark,
    onSecondary = Color.White,
    background = Color(0xFFF5F8F6),
    onBackground = Color(0xFF111715),
    surface = Color.White,
    onSurface = Color(0xFF111715),
    surfaceVariant = Color(0xFFE3ECE7),
    onSurfaceVariant = Color(0xFF45524C),
    outline = Color(0xFFB4C2BC),
)

@Composable
fun QuranCompanionTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
