package com.qurancompanion.app

import android.app.Application
import com.qurancompanion.app.domain.model.QuranLoadResult
import kotlinx.coroutines.launch

class QuranCompanionApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Import/validate the bundled Quran data, then build the similar-ayah index, in the background.
        container.applicationScope.launch {
            if (container.quranRepository.ensureLoaded() == QuranLoadResult.Ready) {
                container.similarityRepository.ensureIndexed()
            }
        }
    }
}
