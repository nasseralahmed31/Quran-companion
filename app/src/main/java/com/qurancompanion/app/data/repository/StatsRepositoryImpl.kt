package com.qurancompanion.app.data.repository

import com.qurancompanion.app.data.local.DayCountRow
import com.qurancompanion.app.data.local.MasteryCountRow
import com.qurancompanion.app.data.local.QuranDatabase
import com.qurancompanion.app.data.local.SurahProgressRow
import com.qurancompanion.app.domain.model.DayCount
import com.qurancompanion.app.domain.model.MapStatus
import com.qurancompanion.app.domain.model.MasteryLevel
import com.qurancompanion.app.domain.model.ProgressSummary
import com.qurancompanion.app.domain.model.QuranConstants
import com.qurancompanion.app.domain.model.StatsOverview
import com.qurancompanion.app.domain.model.SurahMapEntry
import com.qurancompanion.app.domain.repository.ProgressRepository
import com.qurancompanion.app.domain.repository.StatsRepository
import com.qurancompanion.app.domain.util.DayClock
import com.qurancompanion.app.domain.util.KhatmForecast
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class StatsRepositoryImpl(
    db: QuranDatabase,
    private val progress: ProgressRepository,
) : StatsRepository {
    private val stats = db.statsDao()
    private val memorization = db.memorizationDao()
    private val review = db.reviewDao()
    private val quran = db.quranDao()

    private data class Head(val summary: ProgressSummary, val records: Int, val sessions: Int, val quizzes: Int)
    private data class Series(
        val memorized: List<DayCountRow>,
        val reviewed: List<DayCountRow>,
        val mastery: List<MasteryCountRow>,
        val activeDays: Set<Long>,
    )

    override fun observeOverview(): Flow<StatsOverview> {
        val head = combine(
            progress.observeSummary(),
            stats.observeEntryCount(),
            stats.observeReviewSessionCount(),
            stats.observeQuizCount(),
        ) { summary, records, sessions, quizzes -> Head(summary, records, sessions, quizzes) }
        val series = combine(
            stats.observeMemorizedPerDay(),
            stats.observeReviewedPerDay(),
            stats.observeMasteryCounts(),
            memorization.observeEntryDays(),
            review.observeReviewDays(),
        ) { memorized, reviewed, mastery, entryDays, reviewDays ->
            Series(memorized, reviewed, mastery, (entryDays + reviewDays).toSet())
        }
        return combine(head, series, DayClock.ticker()) { h, s, today -> build(h, s, today) }
    }

    override fun observeSurahMap(): Flow<List<SurahMapEntry>> =
        combine(quran.observeSurahs(), stats.observeSurahProgress()) { surahs, rows ->
            val byId = rows.associateBy { it.surahId }
            surahs.map { s ->
                val row = byId[s.id]
                val status = when {
                    row == null || row.memorized == 0 -> MapStatus.NOT_MEMORIZED
                    row.weak > 0 -> MapStatus.WEAK
                    row.needsReview > 0 -> MapStatus.NEEDS_REVIEW
                    else -> MapStatus.MASTERED
                }
                SurahMapEntry(s.id, s.nameAr, s.ayahCount, row?.memorized ?: 0, status, s.startPage)
            }
        }

    private fun build(h: Head, s: Series, today: Long): StatsOverview {
        val memorized = s.memorized.map { DayCount(it.day, it.total) }
        val reviewed = s.reviewed.map { DayCount(it.day, it.total) }

        val last30 = memorized.filter { it.day > today - 30 }.sumOf { it.total }
        val weekly = (0 until 7).count { (today - it) in s.activeDays } * 100 / 7
        val monthly = (0 until 30).count { (today - it) in s.activeDays } * 100 / 30

        val windowStart = today - 89
        val byDay = memorized.associate { it.day to it.total }
        var running = memorized.filter { it.day < windowStart }.sumOf { it.total }
        val cumulative = (windowStart..today).map { day ->
            running += byDay[day] ?: 0
            DayCount(day, running)
        }

        val counts = HashMap<MasteryLevel, Int>()
        for (row in s.mastery) {
            val level = runCatching { MasteryLevel.valueOf(row.level) }.getOrNull() ?: continue
            counts[level] = (counts[level] ?: 0) + row.total
        }
        counts[MasteryLevel.NEW] = (QuranConstants.TOTAL_AYAHS - counts.values.sum()).coerceAtLeast(0)

        val firstDay = memorized.minOfOrNull { it.day }
        val pace = KhatmForecast.pacePerDay(last30, firstDay, today)
        val remaining = (QuranConstants.TOTAL_AYAHS - h.summary.memorizedAyahs).coerceAtLeast(0)
        val daysToFinish = KhatmForecast.daysToFinish(remaining, pace)

        return StatsOverview(
            summary = h.summary,
            memorizationRecords = h.records,
            reviewSessions = h.sessions,
            quizSessions = h.quizzes,
            avgDailyAyahs = last30 / 30.0,
            weeklyConsistencyPercent = weekly,
            monthlyConsistencyPercent = monthly,
            memorizedPerDay = memorized,
            reviewedPerDay = reviewed,
            cumulativeMemorized = cumulative,
            masteryAyahs = counts,
            activeDays = s.activeDays,
            today = today,
            paceAyahsPerDay = pace,
            projectedKhatmDay = daysToFinish?.let { today + it },
        )
    }
}
