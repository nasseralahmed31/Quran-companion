package com.qurancompanion.app.domain.util

import kotlin.math.ceil

object KhatmForecast {
    /**
     * Memorization pace (ayahs per day) over the recent window. A newer user is measured only over
     * the days since the first memorization, so the average is not dragged down by days before it.
     */
    fun pacePerDay(ayahsInWindow: Int, firstDay: Long?, today: Long, windowDays: Int = 30): Double {
        if (firstDay == null || ayahsInWindow <= 0) return 0.0
        val span = minOf(windowDays.toLong(), today - firstDay + 1).coerceAtLeast(1)
        return ayahsInWindow.toDouble() / span
    }

    /** Whole days needed at [pacePerDay]; null when there is no pace to project from. */
    fun daysToFinish(remainingAyahs: Int, pacePerDay: Double): Int? = when {
        remainingAyahs <= 0 -> 0
        pacePerDay <= 0.0 -> null
        else -> ceil(remainingAyahs / pacePerDay).toInt()
    }
}
