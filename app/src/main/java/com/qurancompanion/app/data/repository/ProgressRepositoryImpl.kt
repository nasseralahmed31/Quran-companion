package com.qurancompanion.app.data.repository

import com.qurancompanion.app.data.local.MemorizationDao
import com.qurancompanion.app.data.local.ReviewDao
import com.qurancompanion.app.domain.model.ProgressSummary
import com.qurancompanion.app.domain.repository.ProgressRepository
import com.qurancompanion.app.domain.repository.SettingsRepository
import com.qurancompanion.app.domain.util.DayClock
import com.qurancompanion.app.domain.util.StreakCalculator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest

/** Home-screen numbers computed from real records in the database. */
@OptIn(ExperimentalCoroutinesApi::class)
class ProgressRepositoryImpl(
    private val dao: MemorizationDao,
    private val reviewDao: ReviewDao,
    private val settings: SettingsRepository,
) : ProgressRepository {

    private data class Counts(val ayahs: Int, val surahs: Int, val juz: Int, val pages: Int, val reviewed: Int)
    private data class Daily(val done: Int, val due: Int, val target: Int, val day: Long)

    override fun observeSummary(): Flow<ProgressSummary> {
        val today = DayClock.ticker()
        val counts = combine(
            dao.observeMemorizedCount(),
            dao.observeCompletedSurahsCount(),
            dao.observeMemorizedJuzCount(),
            dao.observeMemorizedPagesCount(),
            reviewDao.observeReviewedAyahs(),
        ) { ayahs, surahs, juz, pages, reviewed -> Counts(ayahs, surahs, juz, pages, reviewed) }

        val daily = combine(
            today.flatMapLatest { dao.observeMemorizedOnDay(it) },
            today.flatMapLatest { reviewDao.observeDueAyahs(it) },
            settings.dailyTargetAyahs,
            today,
        ) { done, due, target, day -> Daily(done, due, target, day) }

        val activeDays = combine(dao.observeEntryDays(), reviewDao.observeReviewDays()) { a, b -> (a + b).distinct() }

        return combine(counts, activeDays, daily) { c, days, d ->
            ProgressSummary(
                memorizedAyahs = c.ayahs,
                completedSurahs = c.surahs,
                memorizedJuz = c.juz,
                memorizedPages = c.pages,
                streakDays = StreakCalculator.current(days, d.day),
                todayTargetAyahs = d.target,
                todayDoneAyahs = d.done,
                dueReviewAyahs = d.due,
                reviewedAyahs = c.reviewed,
            )
        }
    }
}
