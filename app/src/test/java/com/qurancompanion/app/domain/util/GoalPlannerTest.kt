package com.qurancompanion.app.domain.util

import com.qurancompanion.app.domain.model.GoalStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class GoalPlannerTest {
    @Test
    fun scheduleSplitsEvenlyAndCoversEverything() {
        val plan = GoalPlanner.schedule(1, 10, 3)
        assertEquals(listOf(4, 3, 3), plan.map { it.ayahCount })
        assertEquals(1, plan.first().fromAyahId)
        assertEquals(10, plan.last().toAyahId)
        plan.zipWithNext().forEach { (a, b) -> assertEquals(a.toAyahId + 1, b.fromAyahId) }
    }

    @Test
    fun moreDaysThanAyahsGivesOneAyahPerDay() {
        val plan = GoalPlanner.schedule(5, 7, 30)
        assertEquals(3, plan.size)
        assertEquals(listOf(1, 1, 1), plan.map { it.ayahCount })
    }

    @Test
    fun requiredPerDayRoundsUp() {
        assertEquals(4, GoalPlanner.requiredPerDay(10, 3))
        assertEquals(0, GoalPlanner.requiredPerDay(0, 3))
        assertEquals(10, GoalPlanner.requiredPerDay(10, 0))
    }

    @Test
    fun daysLeftIncludesToday() {
        assertEquals(1, GoalPlanner.daysLeft(100, 100))
        assertEquals(0, GoalPlanner.daysLeft(99, 100))
    }

    @Test
    fun statusComparesWithTheExpectedPace() {
        // 100 ayahs over 10 days; on day 5 the expected pace is 50.
        assertEquals(GoalStatus.AHEAD, GoalPlanner.status(60, 100, 1, 10, 5))
        assertEquals(GoalStatus.ON_TRACK, GoalPlanner.status(48, 100, 1, 10, 5))
        assertEquals(GoalStatus.BEHIND, GoalPlanner.status(20, 100, 1, 10, 5))
        assertEquals(GoalStatus.COMPLETED, GoalPlanner.status(100, 100, 1, 10, 5))
        assertEquals(GoalStatus.OVERDUE, GoalPlanner.status(20, 100, 1, 10, 11))
    }
}
