package com.qurancompanion.app.domain.util

import org.junit.Assert.assertEquals
import org.junit.Test

class StreakCalculatorTest {
    @Test
    fun noDaysMeansNoStreak() {
        assertEquals(0, StreakCalculator.current(emptyList(), 100))
    }

    @Test
    fun consecutiveDaysEndingTodayAreCounted() {
        assertEquals(3, StreakCalculator.current(listOf(100L, 99L, 98L, 95L), 100))
    }

    @Test
    fun streakEndingYesterdayIsStillAlive() {
        assertEquals(2, StreakCalculator.current(listOf(99L, 98L), 100))
    }

    @Test
    fun gapOfTwoDaysBreaksTheStreak() {
        assertEquals(0, StreakCalculator.current(listOf(97L, 96L), 100))
    }

    @Test
    fun duplicateDaysAreCountedOnce() {
        assertEquals(2, StreakCalculator.current(listOf(100L, 100L, 99L), 100))
    }
}
