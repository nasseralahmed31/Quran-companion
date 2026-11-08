package com.qurancompanion.app.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KhatmForecastTest {
    @Test
    fun paceCountsOnlyTheDaysSinceTheFirstMemorization() {
        assertEquals(6.0, KhatmForecast.pacePerDay(60, firstDay = 91, today = 100), 1e-9)
    }

    @Test
    fun paceUsesTheWholeWindowOnceOlderThanIt() {
        assertEquals(2.0, KhatmForecast.pacePerDay(60, firstDay = 1, today = 100), 1e-9)
    }

    @Test
    fun noRecentMemorizationMeansNoPace() {
        assertEquals(0.0, KhatmForecast.pacePerDay(0, null, 100), 1e-9)
        assertEquals(0.0, KhatmForecast.pacePerDay(0, 50, 100), 1e-9)
    }

    @Test
    fun daysToFinishRoundsUp() {
        assertEquals(4, KhatmForecast.daysToFinish(10, 3.0))
        assertNull(KhatmForecast.daysToFinish(10, 0.0))
        assertEquals(0, KhatmForecast.daysToFinish(0, 0.0))
    }
}
