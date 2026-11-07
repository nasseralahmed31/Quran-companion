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
import com.qurancompanion.app.domain.repository.ReviewRepository
import com.qurancompanion.app.domain.util.DayClock
import com.qurancompanion.app.domain.util.EpochDay
import com.qurancompanion.app.domain.util.ReviewOrdering
import com.qurancompanion.app.domain.util.ReviewScheduler
import com.qurancompanion.app.domain.util.ScheduleState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ReviewRepositoryImpl(
    private val db: QuranDatabase,
    private val updater: AyahProgressUpdater,
    private val achievements: AchievementEvaluator,
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
        val nextDay = manualNextDay?.coerceAtLeast(today) ?: (today + next.intervalDays)
        val interval = if (manualNextDay != null) (nextDay - today).toInt().coerceAtLeast(1) else next.intervalDays
        val previousMastery = runCatching { MasteryLevel.valueOf(entry.mastery) }.getOrDefault(MasteryLevel.GOOD)
        val newMastery = ReviewScheduler.masteryAfter(effective, next.repetitions)

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
        )
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
