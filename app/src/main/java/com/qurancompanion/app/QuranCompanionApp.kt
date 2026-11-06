package com.qurancompanion.app

import android.app.Application
import kotlinx.coroutines.launch

class QuranCompanionApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Import/validate the bundled Quran data in the background on startup.
        container.applicationScope.launch { container.quranRepository.ensureLoaded() }
    }
}
