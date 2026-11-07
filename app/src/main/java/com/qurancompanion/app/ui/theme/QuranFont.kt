package com.qurancompanion.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily

/**
 * Optional Quran font: put a TTF at app/src/main/assets/fonts/uthmani.ttf
 * (scripts/download_quran_data.sh fetches Amiri Quran). Without it the system Arabic font is used.
 */
@Composable
fun rememberQuranFontFamily(): FontFamily? {
    val context = LocalContext.current
    return remember(context) {
        val present = runCatching { context.assets.list("fonts")?.contains("uthmani.ttf") == true }.getOrDefault(false)
        if (present) FontFamily(Font(path = "fonts/uthmani.ttf", assetManager = context.assets)) else null
    }
}
