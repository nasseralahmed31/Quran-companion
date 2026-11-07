package com.qurancompanion.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qurancompanion.app.domain.model.AppSettings
import com.qurancompanion.app.ui.navigation.AppRoot
import com.qurancompanion.app.ui.theme.QuranCompanionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as QuranCompanionApp).container
        setContent {
            val settings by container.settingsRepository.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
            QuranCompanionTheme(themeMode = settings.themeMode, colorTheme = settings.colorTheme) {
                // The app is Arabic-first: force RTL regardless of device language.
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    AppRoot()
                }
            }
        }
    }
}
