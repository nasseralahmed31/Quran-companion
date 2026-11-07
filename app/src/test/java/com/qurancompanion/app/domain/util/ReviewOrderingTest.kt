package com.qurancompanion.app.domain.util

import com.qurancompanion.app.domain.model.MasteryLevel
import com.qurancompanion.app.domain.model.ReviewItem
import com.qurancompanion.app.domain.model.ReviewStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ReviewOrderingTest {
    private fun item(
        id: Long,
        mastery: MasteryLevel,
        status: ReviewStatus,
        overdue: Int = 0,
        next: Long = 100,
    ) = ReviewItem(
        entryId = id, surahId = 1, fromAyah = 1, toAyah = 5, mastery = mastery,
        nextReviewDay = next, intervalDays = 1, repetitions = 0, lapses = 0, lastReviewDay = 0,
        status = status, daysOverdue = overdue,
    )

    private fun order(vararg items: ReviewItem) = items.sortedWith(ReviewOrdering.comparator).map { it.entryId }

    @Test
    fun passagesNeedingReviewComeBeforeConfidentOnesDueTheSameDay() {
        val sorted = order(
            item(1, MasteryLevel.GOOD, ReviewStatus.DUE_TODAY),
            item(2, MasteryLevel.WEAK, ReviewStatus.DUE_TODAY),
            item(3, MasteryLevel.NEEDS_REVIEW, ReviewStatus.DUE_TODAY),
            item(4, MasteryLevel.MASTERED, ReviewStatus.DUE_TODAY),
        )
        assertEquals(listOf(2L, 3L, 1L, 4L), sorted)
    }

    @Test
    fun aDueWeakPassageBeatsALongOverdueConfidentOne() {
        val sorted = order(
            item(1, MasteryLevel.GOOD, ReviewStatus.OVERDUE, overdue = 30),
            item(2, MasteryLevel.NEEDS_REVIEW, ReviewStatus.DUE_TODAY),
        )
        assertEquals(listOf(2L, 1L), sorted)
    }

    @Test
    fun dueItemsComeBeforeUpcomingEvenWhenUpcomingIsWeak() {
        val sorted = order(
            item(1, MasteryLevel.WEAK, ReviewStatus.UPCOMING, next = 102),
            item(2, MasteryLevel.MASTERED, ReviewStatus.DUE_TODAY),
        )
        assertEquals(listOf(2L, 1L), sorted)
    }

    @Test
    fun withinAGroupTheMostOverdueComesFirst() {
        val sorted = order(
            item(1, MasteryLevel.GOOD, ReviewStatus.OVERDUE, overdue = 2),
            item(2, MasteryLevel.GOOD, ReviewStatus.OVERDUE, overdue = 10),
        )
        assertEquals(listOf(2L, 1L), sorted)
    }

    @Test
    fun groupsMatchSectionLabels() {
        assertEquals(ReviewOrdering.GROUP_NEEDS_REVIEW, ReviewOrdering.group(item(1, MasteryLevel.WEAK, ReviewStatus.OVERDUE)))
        assertEquals(ReviewOrdering.GROUP_REGULAR, ReviewOrdering.group(item(1, MasteryLevel.GOOD, ReviewStatus.DUE_TODAY)))
        assertEquals("تحتاج مراجعة أولًا", ReviewOrdering.sectionLabel(ReviewOrdering.GROUP_NEEDS_REVIEW))
    }
}
