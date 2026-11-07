package com.qurancompanion.app.notifications

import com.qurancompanion.app.domain.util.ReminderTime
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderTimeTest {
    private val utc = TimeZone.getTimeZone("UTC")
    private val hour = 3_600_000L
    // Day 20000 at 10:00 UTC.
    private val now = 20_000L * 24 * hour + 10 * hour

    @Test
    fun laterTodayIsWithinTheDay() {
        assertEquals(1 * hour, ReminderTime.delayUntil(11, 0, now, utc))
    }

    @Test
    fun anEarlierTimeMeansTomorrow() {
        assertEquals(23 * hour, ReminderTime.delayUntil(9, 0, now, utc))
    }

    @Test
    fun theExactCurrentTimeMeansTomorrow() {
        assertEquals(24 * hour, ReminderTime.delayUntil(10, 0, now, utc))
    }

    @Test
    fun theDelayIsNeverMoreThanADayOrNonPositive() {
        for (h in 0..23) {
            val d = ReminderTime.delayUntil(h, 30, now, utc)
            assertTrue(d > 0 && d <= 24 * hour)
        }
    }
}
