package com.qurancompanion.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.qurancompanion.app.domain.model.ColorTheme
import com.qurancompanion.app.domain.model.ThemeMode

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

private fun darkScheme(theme: ColorTheme): ColorScheme = when (theme) {
    ColorTheme.EMERALD -> DarkColors
    ColorTheme.SAPPHIRE -> DarkColors.copy(
        primary = Color(0xFF60A5FA), onPrimary = Color(0xFF0B2A5B),
        primaryContainer = Color(0xFF1E3A8A), onPrimaryContainer = Color(0xFFDBEAFE),
    )
    ColorTheme.VIOLET -> DarkColors.copy(
        primary = Color(0xFFA78BFA), onPrimary = Color(0xFF2E1065),
        primaryContainer = Color(0xFF4C1D95), onPrimaryContainer = Color(0xFFEDE9FE),
    )
}

private fun lightScheme(theme: ColorTheme): ColorScheme = when (theme) {
    ColorTheme.EMERALD -> LightColors
    ColorTheme.SAPPHIRE -> LightColors.copy(
        primary = Color(0xFF1D4ED8), onPrimary = Color.White,
        primaryContainer = Color(0xFFDBEAFE), onPrimaryContainer = Color(0xFF1E3A8A),
    )
    ColorTheme.VIOLET -> LightColors.copy(
        primary = Color(0xFF6D28D9), onPrimary = Color.White,
        primaryContainer = Color(0xFFEDE9FE), onPrimaryContainer = Color(0xFF4C1D95),
    )
}

@Composable
fun QuranCompanionTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    colorTheme: ColorTheme = ColorTheme.EMERALD,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (dark) darkScheme(colorTheme) else lightScheme(colorTheme),
        typography = AppTypography,
        content = content,
    )
}
