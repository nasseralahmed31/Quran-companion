package com.qurancompanion.app

import android.app.Application
import com.qurancompanion.app.domain.model.QuranLoadResult
import com.qurancompanion.app.notifications.NotificationHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class QuranCompanionApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationHelper.ensureChannel(this)
        container.applicationScope.launch {
            // Keep the queued reminders in line with the saved settings.
            container.reminderScheduler.sync(container.settingsRepository.settings.first())
            // Import/validate the bundled Quran data, then build the similar-ayah index.
            if (container.quranRepository.ensureLoaded() == QuranLoadResult.Ready) {
                container.similarityRepository.ensureIndexed()
            }
        }
    }
}
