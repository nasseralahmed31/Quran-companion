package com.qurancompanion.app

import android.content.Context
import com.qurancompanion.app.data.local.QuranDatabase
import com.qurancompanion.app.data.quran.QuranImporter
import com.qurancompanion.app.data.repository.LocalProgressRepository
import com.qurancompanion.app.data.repository.QuranRepositoryImpl
import com.qurancompanion.app.domain.repository.ProgressRepository
import com.qurancompanion.app.domain.repository.QuranRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Simple manual dependency container. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val database: QuranDatabase by lazy { QuranDatabase.create(appContext) }

    val quranRepository: QuranRepository by lazy {
        QuranRepositoryImpl(database.quranDao(), QuranImporter(appContext, database))
    }

    // Replaced by a Room-backed implementation in phase 3 (memorization records).
    val progressRepository: ProgressRepository = LocalProgressRepository()
}
