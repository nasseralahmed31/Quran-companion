package com.qurancompanion.app.data.repository

import androidx.room.withTransaction
import com.qurancompanion.app.data.local.QuranDatabase
import com.qurancompanion.app.data.local.ReviewItemRow
import com.qurancompanion.app.data.local.ReviewScheduleEntity
import com.qurancompanion.app.data.local.ReviewSessionEntity
import com.qurancompanion.app.domain.model.MasteryLevel
import com.qurancompanion.app.domain.model.MemorizationState
import com.qurancompanion.app.domain.model.ReviewItem
import com.qurancompanion.app.domain.model.ReviewOutcome
import com.qurancompanion.app.domain.model.ReviewRating
import com.qurancompanion.app.domain.model.RolloverSummary
import com.qurancompanion.app.domain.repository.ReviewRepository
import com.qurancompanion.app.domain.repository.SettingsRepository
import com.qurancompanion.app.domain.util.DayClock
import com.qurancompanion.app.domain.util.EpochDay
import com.qurancompanion.app.domain.util.ReviewOrdering
import com.qurancompanion.app.domain.util.ReviewRollover
import com.qurancompanion.app.domain.util.ReviewSample
import com.qurancompanion.app.domain.util.RolloverItem
import com.qurancompanion.app.domain.util.ReviewScheduler
import com.qurancompanion.app.domain.util.ScheduleState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

class ReviewRepositoryImpl(
    private val db: QuranDatabase,
    private val updater: AyahProgressUpdater,
    private val achievements: AchievementEvaluator,
    private val settings: SettingsRepository,
) : ReviewRepository {
    private val reviewDao = db.reviewDao()
    private val memoDao = db.memorizationDao()

    override fun observeItems(): Flow<List<ReviewItem>> =
        combine(reviewDao.observeItems(), DayClock.ticker()) { rows, today ->
            rows.map { it.toItem(today) }.sortedWith(ReviewOrdering.comparator)
        }

    override suspend fun getItem(entryId: Long): ReviewItem? =
        reviewDao.getItemRow(entryId)?.toItem(EpochDay.today())

    override suspend fun applyReview(
        entryId: Long,
        rating: ReviewRating,
        errors: Int,
        note: String,
        manualNextDay: Long?,
        source: String,
    ): ReviewOutcome = db.withTransaction {
        val today = EpochDay.today()
        val now = System.currentTimeMillis()
        val entry = memoDao.getEntry(entryId) ?: throw IllegalStateException("سجل الحفظ غير موجود")
        val schedule = reviewDao.getSchedule(entryId)
        val current = schedule?.let { ScheduleState(it.intervalDays, it.ease, it.repetitions, it.lapses) }
            ?: ReviewScheduler.initial()

        val effective = ReviewScheduler.effectiveRating(rating, errors.coerceAtLeast(0))
        val next = ReviewScheduler.next(current, effective)
        val previousMastery = runCatching { MasteryLevel.valueOf(entry.mastery) }.getOrDefault(MasteryLevel.GOOD)
        val newMastery = ReviewScheduler.masteryAfter(effective, next.repetitions)
        // This review plus the last few adapt the interval: repeated mistakes shorten it, and weak
        // passages are capped so they come back often.
        val history = listOf(ReviewSample(effective, errors.coerceAtLeast(0))) +
            reviewDao.recentSessions(entryId, 4).map {
                ReviewSample(runCatching { ReviewRating.valueOf(it.rating) }.getOrDefault(ReviewRating.GOOD), it.errorCount)
            }
        val factor = ReviewScheduler.historyFactor(history)
        val adapted = ReviewScheduler.adaptInterval(next.intervalDays, factor, newMastery)
        val nextDay = manualNextDay?.coerceAtLeast(today) ?: (today + adapted)
        val interval = if (manualNextDay != null) (nextDay - today).toInt().coerceAtLeast(1) else adapted
        val adaptationNote =
            if (manualNextDay != null) null else ReviewScheduler.adaptationNote(factor, next.intervalDays, adapted, newMastery)

        reviewDao.upsertSchedule(
            ReviewScheduleEntity(entryId, nextDay, interval, next.ease, next.repetitions, next.lapses, today),
        )
        if (entry.state == MemorizationState.MEMORIZED.name) {
            memoDao.updateEntry(entry.copy(mastery = newMastery.name, updatedAt = now))
            updater.recomputeRange(entry.surahId, entry.fromAyah, entry.toAyah)
        }
        reviewDao.insertSession(
            ReviewSessionEntity(
                entryId = entryId,
                surahId = entry.surahId,
                fromAyah = entry.fromAyah,
                toAyah = entry.toAyah,
                reviewedOn = today,
                timestamp = now,
                rating = effective.name,
                errorCount = errors.coerceAtLeast(0),
                note = note.trim(),
                nextReviewDay = nextDay,
                source = source,
            ),
        )
        ReviewOutcome(
            nextReviewDay = nextDay,
            intervalDays = interval,
            newMastery = newMastery,
            suggestRememorize = ReviewScheduler.suggestRememorize(effective, next, errors, previousMastery),
            newAchievements = achievements.evaluate(now),
            adaptationNote = adaptationNote,
        )
    }

    override suspend fun rollover(): RolloverSummary? {
        val s = settings.settings.first()
        val today = EpochDay.today()
        if (s.reviewRolloverDay >= today) return null
        if (!s.autoShiftReviews) {
            // Keep the marker current so turning the feature on later does not shift everything at once.
            settings.setReviewRolloverDay(today)
            return null
        }
        val items = reviewDao.itemRows().map { row ->
            RolloverItem(
                entryId = row.entryId,
                nextReviewDay = row.nextReviewDay,
                ayahCount = row.toAyah - row.fromAyah + 1,
                mastery = runCatching { MasteryLevel.valueOf(row.mastery) }.getOrDefault(MasteryLevel.GOOD),
            )
        }
        val result = ReviewRollover.compute(items, today, s.reviewRolloverDay, reviewDao.reviewDays().toSet(), s.wirdReviewAyahs)
        if (result.changes.isNotEmpty()) {
            db.withTransaction { result.changes.forEach { reviewDao.setNextDay(it.entryId, it.newNextDay) } }
        }
        settings.setReviewRolloverDay(today)
        if (result.overdueMoved <= 0) return null
        settings.setRolloverNote(today, result.overdueMoved, result.shiftDays)
        return RolloverSummary(result.overdueMoved, result.shiftDays)
    }

    override suspend fun reschedule(entryId: Long, nextDay: Long) {
        val schedule = reviewDao.getSchedule(entryId) ?: return
        val today = EpochDay.today()
        val day = nextDay.coerceAtLeast(today)
        reviewDao.upsertSchedule(
            schedule.copy(nextReviewDay = day, intervalDays = (day - today).toInt().coerceAtLeast(1)),
        )
    }
}

private fun ReviewItemRow.toItem(today: Long) = ReviewItem(
    entryId = entryId,
    surahId = surahId,
    fromAyah = fromAyah,
    toAyah = toAyah,
    mastery = runCatching { MasteryLevel.valueOf(mastery) }.getOrDefault(MasteryLevel.GOOD),
    nextReviewDay = nextReviewDay,
    intervalDays = intervalDays,
    repetitions = repetitions,
    lapses = lapses,
    lastReviewDay = lastReviewDay,
    status = ReviewScheduler.statusOf(nextReviewDay, lastReviewDay, today),
    daysOverdue = if (nextReviewDay < today) (today - nextReviewDay).toInt() else 0,
)
