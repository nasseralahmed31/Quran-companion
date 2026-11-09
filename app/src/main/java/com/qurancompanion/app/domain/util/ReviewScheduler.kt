package com.qurancompanion.app.domain.util

import com.qurancompanion.app.domain.model.MasteryLevel
import com.qurancompanion.app.domain.model.ReviewRating
import com.qurancompanion.app.domain.model.ReviewStatus
import kotlin.math.roundToInt

data class ScheduleState(
    val intervalDays: Int,
    val ease: Double,
    val repetitions: Int,
    val lapses: Int,
)

/** One past review of a passage: how it went and how many mistakes were made. */
data class ReviewSample(val rating: ReviewRating, val errors: Int)

/** Spaced-repetition scheduling (SM-2 inspired). Pure functions, covered by unit tests. */
object ReviewScheduler {
    const val MIN_EASE = 1.3
    const val MAX_EASE = 2.8
    const val MAX_INTERVAL = 365

    fun initial() = ScheduleState(intervalDays = 1, ease = 2.3, repetitions = 0, lapses = 0)

    /**
     * First review date. Passages that are weak / need review are due as soon as possible (the day
     * after memorizing, or today for older records); passages in good shape are spaced further
     * out, so a backlog never puts confident passages on the same day as the ones that need work.
     */
    fun firstDueDay(memorizedOn: Long, mastery: MasteryLevel, today: Long): Long {
        val offset: Int
        val floor: Int
        when (mastery) {
            MasteryLevel.WEAK, MasteryLevel.NEEDS_REVIEW -> { offset = 1; floor = 0 }
            MasteryLevel.MASTERED -> { offset = 4; floor = 2 }
            else -> { offset = 2; floor = 1 }
        }
        return maxOf(memorizedOn + offset, today + floor)
    }

    /** Many mistakes override a good self-rating. */
    fun effectiveRating(rating: ReviewRating, errors: Int): ReviewRating = when {
        errors >= 5 -> ReviewRating.WEAK
        errors >= 3 && rating.ordinal < ReviewRating.AVERAGE.ordinal -> ReviewRating.AVERAGE
        else -> rating
    }

    fun next(s: ScheduleState, rating: ReviewRating): ScheduleState {
        val updated = when (rating) {
            ReviewRating.EXCELLENT -> {
                val reps = s.repetitions + 1
                val interval = when (reps) {
                    1 -> 3
                    2 -> 7
                    else -> maxOf(s.intervalDays + 1, (s.intervalDays * s.ease).roundToInt())
                }
                s.copy(intervalDays = interval, ease = minOf(MAX_EASE, s.ease + 0.1), repetitions = reps)
            }
            ReviewRating.GOOD -> {
                val reps = s.repetitions + 1
                val interval = when (reps) {
                    1 -> 2
                    2 -> 5
                    else -> maxOf(s.intervalDays + 1, (s.intervalDays * s.ease * 0.85).roundToInt())
                }
                s.copy(intervalDays = interval, repetitions = reps)
            }
            ReviewRating.AVERAGE -> s.copy(
                intervalDays = maxOf(1, (s.intervalDays * 0.6).roundToInt()),
                ease = maxOf(MIN_EASE, s.ease - 0.1),
            )
            ReviewRating.WEAK -> s.copy(
                intervalDays = 1,
                ease = maxOf(MIN_EASE, s.ease - 0.2),
                repetitions = 0,
                lapses = s.lapses + 1,
            )
        }
        return updated.copy(intervalDays = updated.intervalDays.coerceIn(1, MAX_INTERVAL))
    }

    fun masteryAfter(rating: ReviewRating, repetitions: Int): MasteryLevel = when (rating) {
        ReviewRating.EXCELLENT -> if (repetitions >= 3) MasteryLevel.MASTERED else MasteryLevel.GOOD
        ReviewRating.GOOD -> MasteryLevel.GOOD
        ReviewRating.AVERAGE -> MasteryLevel.NEEDS_REVIEW
        ReviewRating.WEAK -> MasteryLevel.WEAK
    }

    /** Recommend re-memorizing when a passage keeps failing. */
    fun suggestRememorize(rating: ReviewRating, state: ScheduleState, errors: Int, previous: MasteryLevel): Boolean =
        rating == ReviewRating.WEAK && (state.lapses >= 2 || errors >= 5 || previous == MasteryLevel.WEAK)

    /**
     * Interval multiplier from the recent history (newest first, this review included):
     * repeated weak results or many mistakes shorten the next interval; a clean streak lengthens it.
     */
    fun historyFactor(recent: List<ReviewSample>): Double {
        if (recent.isEmpty()) return 1.0
        val window = recent.take(5)
        val avgErrors = window.sumOf { it.errors } / window.size.toDouble()
        val weak = window.count { it.rating == ReviewRating.WEAK }
        return when {
            weak >= 2 || avgErrors >= 3.0 -> 0.6
            avgErrors >= 1.5 || (weak == 1 && window.size >= 2) -> 0.8
            window.size >= 3 && avgErrors == 0.0 &&
                window.all { it.rating == ReviewRating.EXCELLENT || it.rating == ReviewRating.GOOD } -> 1.15
            else -> 1.0
        }
    }

    /** Weak passages are capped to short intervals so they come back often. */
    fun maxIntervalFor(mastery: MasteryLevel): Int = when (mastery) {
        MasteryLevel.WEAK -> 2
        MasteryLevel.NEEDS_REVIEW -> 6
        else -> MAX_INTERVAL
    }

    fun adaptInterval(baseInterval: Int, factor: Double, mastery: MasteryLevel): Int =
        (baseInterval * factor).roundToInt().coerceAtLeast(1).coerceAtMost(maxIntervalFor(mastery))

    /** A short explanation for the user when the interval was adapted, or null when it was not. */
    fun adaptationNote(factor: Double, baseInterval: Int, adapted: Int, mastery: MasteryLevel): String? = when {
        adapted < (baseInterval * factor).roundToInt().coerceAtLeast(1) ->
            "ثُبِّت الفاصل قصيرًا لأن المقطع ${mastery.label}، فيعود إليك أكثر."
        factor < 1.0 -> "قُصِّر الفاصل لكثرة أخطائك في المراجعات الأخيرة."
        factor > 1.0 -> "طُوِّل الفاصل لأنك راجعته بلا أخطاء في آخر مرات."
        else -> null
    }

    fun statusOf(next: Long, last: Long, today: Long): ReviewStatus = when {
        next < today -> ReviewStatus.OVERDUE
        next == today -> ReviewStatus.DUE_TODAY
        last == today -> ReviewStatus.DONE
        next - today <= 7 -> ReviewStatus.UPCOMING
        else -> ReviewStatus.LATER
    }
}
