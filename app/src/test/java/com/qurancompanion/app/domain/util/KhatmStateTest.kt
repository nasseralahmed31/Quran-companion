package com.qurancompanion.app.domain.util

import com.qurancompanion.app.domain.model.GoalStatus
import com.qurancompanion.app.domain.model.KhatmState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KhatmStateTest {
    @Test
    fun theFirstDayPlanStartsAtPageOneAndTheDailyShareRoundsUp() {
        val state = KhatmState(startDay = 100, days = 30, pagesRead = 0, readToday = 0, today = 100)
        val plan = state.todayPlan
        assertEquals(1, plan?.fromAyahId)
        assertEquals(21, plan?.toAyahId)
        assertEquals(21, state.requiredToday)
        assertEquals(30, state.daysLeft)
    }

    @Test
    fun theRequiredShareStaysConstantWhileReadingToday() {
        val before = KhatmState(100, 30, pagesRead = 0, readToday = 0, today = 100)
        val after = KhatmState(100, 30, pagesRead = 21, readToday = 21, today = 100)
        assertEquals(before.requiredToday, after.requiredToday)
    }

    @Test
    fun noPlanAfterThePeriodEnds() {
        val state = KhatmState(startDay = 100, days = 30, pagesRead = 300, readToday = 0, today = 131)
        assertNull(state.todayPlan)
        assertEquals(GoalStatus.OVERDUE, state.status)
    }
}
