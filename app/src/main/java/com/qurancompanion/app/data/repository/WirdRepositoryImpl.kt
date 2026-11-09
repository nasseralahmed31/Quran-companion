package com.qurancompanion.app.data.repository

import com.qurancompanion.app.data.local.QuranDatabase
import com.qurancompanion.app.data.local.ReadingLogEntity
import com.qurancompanion.app.domain.model.KhatmState
import com.qurancompanion.app.domain.model.QuranConstants
import com.qurancompanion.app.domain.model.WirdToday
import com.qurancompanion.app.domain.repository.ProgressRepository
import com.qurancompanion.app.domain.repository.SettingsRepository
import com.qurancompanion.app.domain.repository.WirdRepository
import com.qurancompanion.app.domain.util.DayClock
import com.qurancompanion.app.domain.util.EpochDay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class WirdRepositoryImpl(
    db: QuranDatabase,
    private val settings: SettingsRepository,
    private val progress: ProgressRepository,
) : WirdRepository {
    private val reading = db.readingDao()
    private val review = db.reviewDao()

    override fun observeKhatm(): Flow<KhatmState?> =
        combine(settings.settings, DayClock.ticker()) { s, today -> s to today }
            .flatMapLatest { (s, today) ->
                if (s.khatmStartDay <= 0 || s.khatmDays <= 0) {
                    flowOf(null)
                } else {
                    combine(
                        reading.observeDistinctPagesSince(s.khatmStartDay),
                        reading.observePagesOnDay(today),
                    ) { done, readToday -> KhatmState(s.khatmStartDay, s.khatmDays, done, readToday, today) }
                }
            }

    override fun observeToday(): Flow<WirdToday> {
        val today = DayClock.ticker()
        return combine(
            settings.settings,
            progress.observeSummary(),
            today.flatMapLatest { reading.observePagesOnDay(it) },
            today.flatMapLatest { review.observeReviewedOnDay(it) },
            observeKhatm(),
        ) { s, summary, readPages, reviewed, khatm ->
            // A running khatmah decides the recitation target; otherwise the manual target applies.
            val readTarget = khatm?.requiredToday ?: s.wirdReadPages
            WirdToday(
                readPages = readPages,
                readTarget = readTarget,
                readTargetFromKhatm = khatm != null,
                memorizedAyahs = summary.todayDoneAyahs,
                memorizeTarget = s.dailyTargetAyahs,
                reviewedAyahs = reviewed,
                reviewTarget = s.wirdReviewAyahs,
                manualReadTarget = s.wirdReadPages,
            )
        }
    }

    override suspend fun markRead(page: Int) {
        if (page !in 1..QuranConstants.TOTAL_PAGES) return
        reading.insertAll(listOf(ReadingLogEntity(page, EpochDay.today())))
    }

    override suspend fun logPages(fromPage: Int, toPage: Int) {
        if (fromPage < 1 || toPage < fromPage || toPage > QuranConstants.TOTAL_PAGES) {
            throw IllegalArgumentException("نطاق الصفحات غير صحيح")
        }
        val day = EpochDay.today()
        reading.insertAll((fromPage..toPage).map { ReadingLogEntity(it, day) })
    }

    override suspend fun startKhatm(days: Int) {
        settings.setKhatmPlan(EpochDay.today(), days)
    }

    override suspend fun cancelKhatm() {
        settings.setKhatmPlan(0L, 0)
    }

    override suspend fun setTargets(readPages: Int, memorizeAyahs: Int, reviewAyahs: Int) {
        settings.setWirdTargets(readPages, reviewAyahs)
        settings.setDailyTarget(memorizeAyahs)
    }
}
