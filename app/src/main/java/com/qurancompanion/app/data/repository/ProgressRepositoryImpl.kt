package com.qurancompanion.app.data.repository

import com.qurancompanion.app.data.local.MemorizationDao
import com.qurancompanion.app.domain.model.ProgressSummary
import com.qurancompanion.app.domain.repository.ProgressRepository
import com.qurancompanion.app.domain.repository.SettingsRepository
import com.qurancompanion.app.domain.util.EpochDay
import com.qurancompanion.app.domain.util.StreakCalculator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow

/** Home-screen numbers computed from real records in the database. */
@OptIn(ExperimentalCoroutinesApi::class)
class ProgressRepositoryImpl(
    private val dao: MemorizationDao,
    private val settings: SettingsRepository,
) : ProgressRepository {

    private data class Counts(val ayahs: Int, val surahs: Int, val juz: Int, val pages: Int)

    override fun observeSummary(): Flow<ProgressSummary> {
        val today = dayTicker()
        val counts = combine(
            dao.observeMemorizedCount(),
            dao.observeCompletedSurahsCount(),
            dao.observeMemorizedJuzCount(),
            dao.observeMemorizedPagesCount(),
        ) { ayahs, surahs, juz, pages -> Counts(ayahs, surahs, juz, pages) }
        val todayDone = today.flatMapLatest { dao.observeMemorizedOnDay(it) }

        return combine(counts, dao.observeEntryDays(), todayDone, settings.dailyTargetAyahs, today) {
                c, days, done, target, day ->
            ProgressSummary(
                memorizedAyahs = c.ayahs,
                completedSurahs = c.surahs,
                memorizedJuz = c.juz,
                memorizedPages = c.pages,
                streakDays = StreakCalculator.current(days, day),
                todayTargetAyahs = target,
                todayDoneAyahs = done,
                dueReviewAyahs = 0, // computed by the review scheduler in phase 4
            )
        }
    }

    /** Emits the current day, re-checking every minute so "today" rolls over at midnight. */
    private fun dayTicker(): Flow<Long> = flow {
        while (true) {
            emit(EpochDay.today())
            delay(60_000)
        }
    }.distinctUntilChanged()
}
