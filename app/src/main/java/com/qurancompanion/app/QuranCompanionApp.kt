package com.qurancompanion.app

import android.app.Application
import com.qurancompanion.app.domain.model.QuranLoadResult
import com.qurancompanion.app.domain.util.DayClock
import com.qurancompanion.app.notifications.NotificationHelper
import kotlinx.coroutines.CancellationException
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
            // Import/validate the bundled Quran data, then tidy older records and build the similar-ayah index.
            if (container.quranRepository.ensureLoaded() == QuranLoadResult.Ready) {
                try {
                    container.memorizationRepository.consolidate()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Merging old records is a tidy-up; the app works without it.
                }
                container.similarityRepository.ensureIndexed()
            }
        }
        // At start and at every new day: if review days were missed, move the schedule forward.
        container.applicationScope.launch {
            DayClock.ticker().collect {
                try {
                    container.reviewRepository.rollover()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Try again at the next day change or app start.
                }
            }
        }
    }
}
