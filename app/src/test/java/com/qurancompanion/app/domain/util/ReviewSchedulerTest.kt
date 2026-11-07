package com.qurancompanion.app.domain.util

import com.qurancompanion.app.domain.model.MasteryLevel
import com.qurancompanion.app.domain.model.ReviewRating
import com.qurancompanion.app.domain.model.ReviewStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewSchedulerTest {
    @Test
    fun excellentReviewsGrowTheIntervalGradually() {
        var s = ReviewScheduler.initial()
        s = ReviewScheduler.next(s, ReviewRating.EXCELLENT)
        assertEquals(3, s.intervalDays)
        s = ReviewScheduler.next(s, ReviewRating.EXCELLENT)
        assertEquals(7, s.intervalDays)
        val third = ReviewScheduler.next(s, ReviewRating.EXCELLENT)
        assertTrue(third.intervalDays > 7)
    }

    @Test
    fun weakReviewComesBackTomorrowAndResetsProgress() {
        val grown = ReviewScheduler.next(ReviewScheduler.next(ReviewScheduler.initial(), ReviewRating.EXCELLENT), ReviewRating.EXCELLENT)
        val weak = ReviewScheduler.next(grown, ReviewRating.WEAK)
        assertEquals(1, weak.intervalDays)
        assertEquals(0, weak.repetitions)
        assertEquals(1, weak.lapses)
        assertTrue(weak.ease < grown.ease)
    }

    @Test
    fun averageReviewShortensTheInterval() {
        val s = ReviewScheduler.initial().copy(intervalDays = 10)
        assertTrue(ReviewScheduler.next(s, ReviewRating.AVERAGE).intervalDays < 10)
    }

    @Test
    fun manyErrorsOverrideAGoodRating() {
        assertEquals(ReviewRating.WEAK, ReviewScheduler.effectiveRating(ReviewRating.EXCELLENT, 5))
        assertEquals(ReviewRating.AVERAGE, ReviewScheduler.effectiveRating(ReviewRating.EXCELLENT, 3))
        assertEquals(ReviewRating.GOOD, ReviewScheduler.effectiveRating(ReviewRating.GOOD, 1))
    }

    @Test
    fun easeStaysWithinBounds() {
        var s = ReviewScheduler.initial()
        repeat(30) { s = ReviewScheduler.next(s, ReviewRating.WEAK) }
        assertEquals(ReviewScheduler.MIN_EASE, s.ease, 1e-9)
        repeat(60) { s = ReviewScheduler.next(s, ReviewRating.EXCELLENT) }
        assertTrue(s.ease <= ReviewScheduler.MAX_EASE)
        assertTrue(s.intervalDays <= ReviewScheduler.MAX_INTERVAL)
    }

    @Test
    fun masteryFollowsPerformance() {
        assertEquals(MasteryLevel.WEAK, ReviewScheduler.masteryAfter(ReviewRating.WEAK, 0))
        assertEquals(MasteryLevel.NEEDS_REVIEW, ReviewScheduler.masteryAfter(ReviewRating.AVERAGE, 2))
        assertEquals(MasteryLevel.GOOD, ReviewScheduler.masteryAfter(ReviewRating.EXCELLENT, 1))
        assertEquals(MasteryLevel.MASTERED, ReviewScheduler.masteryAfter(ReviewRating.EXCELLENT, 3))
    }

    @Test
    fun rememorizeIsSuggestedOnlyForRepeatedFailure() {
        val once = ReviewScheduler.next(ReviewScheduler.initial(), ReviewRating.WEAK)
        assertFalse(ReviewScheduler.suggestRememorize(ReviewRating.WEAK, once, 1, MasteryLevel.GOOD))
        val twice = ReviewScheduler.next(once, ReviewRating.WEAK)
        assertTrue(ReviewScheduler.suggestRememorize(ReviewRating.WEAK, twice, 1, MasteryLevel.GOOD))
        assertTrue(ReviewScheduler.suggestRememorize(ReviewRating.WEAK, once, 6, MasteryLevel.GOOD))
    }

    @Test
    fun statusClassification() {
        assertEquals(ReviewStatus.OVERDUE, ReviewScheduler.statusOf(9, 0, 10))
        assertEquals(ReviewStatus.DUE_TODAY, ReviewScheduler.statusOf(10, 0, 10))
        assertEquals(ReviewStatus.DONE, ReviewScheduler.statusOf(13, 10, 10))
        assertEquals(ReviewStatus.UPCOMING, ReviewScheduler.statusOf(13, 0, 10))
        assertEquals(ReviewStatus.LATER, ReviewScheduler.statusOf(40, 0, 10))
    }

    @Test
    fun firstReviewIsNeverInThePast() {
        assertEquals(11L, ReviewScheduler.firstDueDay(10, MasteryLevel.GOOD, 5))
        assertEquals(50L, ReviewScheduler.firstDueDay(10, MasteryLevel.GOOD, 50))
        assertEquals(13L, ReviewScheduler.firstDueDay(10, MasteryLevel.MASTERED, 5))
    }
}
