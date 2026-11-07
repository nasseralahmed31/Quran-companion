package com.qurancompanion.app.domain.util

import java.util.Calendar
import java.util.TimeZone

object ReminderTime {
    /** Milliseconds from [nowMillis] until the next occurrence of [hour]:[minute] (at most 24h away). */
    fun delayUntil(hour: Int, minute: Int, nowMillis: Long, timeZone: TimeZone = TimeZone.getDefault()): Long {
        val cal = Calendar.getInstance(timeZone)
        cal.timeInMillis = nowMillis
        cal.set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
        cal.set(Calendar.MINUTE, minute.coerceIn(0, 59))
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        if (cal.timeInMillis <= nowMillis) cal.add(Calendar.DAY_OF_MONTH, 1)
        return cal.timeInMillis - nowMillis
    }
}
