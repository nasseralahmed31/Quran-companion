package com.qurancompanion.app

import android.content.Context
import com.qurancompanion.app.data.local.QuranDatabase
import com.qurancompanion.app.data.quran.QuranImporter
import com.qurancompanion.app.data.repository.MemorizationRepositoryImpl
import com.qurancompanion.app.data.repository.ProgressRepositoryImpl
import com.qurancompanion.app.data.repository.QuranRepositoryImpl
import com.qurancompanion.app.data.settings.DataStoreSettingsRepository
import com.qurancompanion.app.domain.repository.MemorizationRepository
import com.qurancompanion.app.domain.repository.ProgressRepository
import com.qurancompanion.app.domain.repository.QuranRepository
import com.qurancompanion.app.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Simple manual dependency container. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val database: QuranDatabase by lazy { QuranDatabase.create(appContext) }

    val settingsRepository: SettingsRepository by lazy { DataStoreSettingsRepository(appContext) }

    val quranRepository: QuranRepository by lazy {
        QuranRepositoryImpl(database.quranDao(), QuranImporter(appContext, database))
    }

    val memorizationRepository: MemorizationRepository by lazy { MemorizationRepositoryImpl(database) }

    val progressRepository: ProgressRepository by lazy {
        ProgressRepositoryImpl(database.memorizationDao(), settingsRepository)
    }
}
