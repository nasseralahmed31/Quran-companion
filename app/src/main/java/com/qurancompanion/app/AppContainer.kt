package com.qurancompanion.app

import android.content.Context
import com.qurancompanion.app.data.local.QuranDatabase
import com.qurancompanion.app.data.quran.QuranImporter
import com.qurancompanion.app.data.repository.AchievementEvaluator
import com.qurancompanion.app.data.repository.AyahProgressUpdater
import com.qurancompanion.app.data.repository.BackupRepositoryImpl
import com.qurancompanion.app.data.repository.GoalRepositoryImpl
import com.qurancompanion.app.data.repository.MemorizationRepositoryImpl
import com.qurancompanion.app.data.repository.ProgressRepositoryImpl
import com.qurancompanion.app.data.repository.QuizRepositoryImpl
import com.qurancompanion.app.data.repository.QuranRepositoryImpl
import com.qurancompanion.app.data.repository.ReviewRepositoryImpl
import com.qurancompanion.app.data.repository.SimilarityRepositoryImpl
import com.qurancompanion.app.data.repository.StatsRepositoryImpl
import com.qurancompanion.app.data.repository.WirdRepositoryImpl
import com.qurancompanion.app.data.settings.DataStoreSettingsRepository
import com.qurancompanion.app.domain.repository.BackupRepository
import com.qurancompanion.app.domain.repository.GoalRepository
import com.qurancompanion.app.domain.repository.MemorizationRepository
import com.qurancompanion.app.domain.repository.ProgressRepository
import com.qurancompanion.app.domain.repository.QuizRepository
import com.qurancompanion.app.domain.repository.QuranRepository
import com.qurancompanion.app.domain.repository.ReviewRepository
import com.qurancompanion.app.domain.repository.SettingsRepository
import com.qurancompanion.app.domain.repository.SimilarityRepository
import com.qurancompanion.app.domain.repository.StatsRepository
import com.qurancompanion.app.domain.repository.WirdRepository
import com.qurancompanion.app.notifications.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Simple manual dependency container. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val database: QuranDatabase by lazy { QuranDatabase.create(appContext) }

    private val progressUpdater by lazy { AyahProgressUpdater(database.memorizationDao(), database.quranDao()) }
    private val achievementEvaluator by lazy { AchievementEvaluator(database.memorizationDao(), database.reviewDao()) }

    val settingsRepository: SettingsRepository by lazy { DataStoreSettingsRepository(appContext) }

    val quranRepository: QuranRepository by lazy {
        QuranRepositoryImpl(database.quranDao(), QuranImporter(appContext, database))
    }

    val memorizationRepository: MemorizationRepository by lazy {
        MemorizationRepositoryImpl(database, progressUpdater, achievementEvaluator)
    }

    val reviewRepository: ReviewRepository by lazy {
        ReviewRepositoryImpl(database, progressUpdater, achievementEvaluator, settingsRepository)
    }

    val progressRepository: ProgressRepository by lazy {
        ProgressRepositoryImpl(database.memorizationDao(), database.reviewDao(), settingsRepository)
    }

    val goalRepository: GoalRepository by lazy {
        GoalRepositoryImpl(database.planningDao(), database.memorizationDao())
    }

    val similarityRepository: SimilarityRepository by lazy { SimilarityRepositoryImpl(database) }

    val quizRepository: QuizRepository by lazy { QuizRepositoryImpl(database, reviewRepository, similarityRepository) }

    val statsRepository: StatsRepository by lazy { StatsRepositoryImpl(database, progressRepository) }

    val backupRepository: BackupRepository by lazy {
        BackupRepositoryImpl(database, settingsRepository, progressUpdater)
    }

    val wirdRepository: WirdRepository by lazy {
        WirdRepositoryImpl(database, settingsRepository, progressRepository)
    }

    val reminderScheduler: ReminderScheduler by lazy { ReminderScheduler(appContext) }
}
