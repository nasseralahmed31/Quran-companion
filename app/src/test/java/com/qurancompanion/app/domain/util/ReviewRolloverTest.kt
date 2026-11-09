package com.qurancompanion.app.domain.util

import com.qurancompanion.app.domain.model.MasteryLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewRolloverTest {
    private fun item(id: Long, next: Long, ayahs: Int = 10, mastery: MasteryLevel = MasteryLevel.GOOD) =
        RolloverItem(id, next, ayahs, mastery)

    private fun RolloverResult.dayOf(id: Long) = changes.first { it.entryId == id }.newNextDay

    @Test
    fun nothingHappensWithoutOverdueItemsOrOnTheFirstRun() {
        assertTrue(ReviewRollover.compute(listOf(item(1, 105)), today = 100, lastProcessedDay = 99, reviewedDays = emptySet(), dailyCapAyahs = 0).changes.isEmpty())
        assertTrue(ReviewRollover.compute(listOf(item(1, 90)), today = 100, lastProcessedDay = 0, reviewedDays = emptySet(), dailyCapAyahs = 0).changes.isEmpty())
        assertTrue(ReviewRollover.compute(listOf(item(1, 90)), today = 100, lastProcessedDay = 100, reviewedDays = emptySet(), dailyCapAyahs = 0).changes.isEmpty())
    }

    @Test
    fun overdueItemsMoveToTodayAndUpcomingOnesShiftByTheMissedDays() {
        val result = ReviewRollover.compute(
            items = listOf(item(1, 98), item(2, 103)),
            today = 100, lastProcessedDay = 97, reviewedDays = emptySet(), dailyCapAyahs = 0,
        )
        assertEquals(100L, result.dayOf(1))
        assertEquals(106L, result.dayOf(2)) // 3 days (97, 98, 99) had no review
        assertEquals(1, result.overdueMoved)
        assertEquals(3, result.shiftDays)
    }

    @Test
    fun reviewedDaysDoNotShiftTheUpcomingItems() {
        val result = ReviewRollover.compute(
            items = listOf(item(1, 98), item(2, 103)),
            today = 100, lastProcessedDay = 98, reviewedDays = setOf(98L, 99L), dailyCapAyahs = 0,
        )
        assertEquals(100L, result.dayOf(1))
        assertTrue(result.changes.none { it.entryId == 2L })
        assertEquals(0, result.shiftDays)
    }

    @Test
    fun theDailyCapSpreadsTheBacklogWeakestFirst() {
        val result = ReviewRollover.compute(
            items = listOf(
                item(1, 95, ayahs = 10, mastery = MasteryLevel.GOOD),
                item(2, 99, ayahs = 10, mastery = MasteryLevel.WEAK),
                item(3, 96, ayahs = 10, mastery = MasteryLevel.NEEDS_REVIEW),
            ),
            today = 100, lastProcessedDay = 99, reviewedDays = emptySet(), dailyCapAyahs = 20,
        )
        assertEquals(100L, result.dayOf(2)) // weak first
        assertEquals(100L, result.dayOf(3))
        assertEquals(101L, result.dayOf(1)) // spilled to tomorrow
    }

    @Test
    fun anItemBiggerThanTheCapStillGetsADay() {
        val result = ReviewRollover.compute(
            items = listOf(item(1, 90, ayahs = 50), item(2, 91, ayahs = 5)),
            today = 100, lastProcessedDay = 99, reviewedDays = emptySet(), dailyCapAyahs = 20,
        )
        assertEquals(100L, result.dayOf(1))
        assertEquals(101L, result.dayOf(2))
    }
}
