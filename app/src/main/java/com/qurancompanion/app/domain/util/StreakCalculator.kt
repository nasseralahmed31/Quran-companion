package com.qurancompanion.app.domain.util

object StreakCalculator {
    /**
     * Length of the run of consecutive days ending today. A streak that ended yesterday is still
     * alive (the user has until the end of today to continue it).
     */
    fun current(days: List<Long>, today: Long): Int {
        if (days.isEmpty()) return 0
        val set = days.toHashSet()
        var cursor = when {
            today in set -> today
            (today - 1) in set -> today - 1
            else -> return 0
        }
        var count = 0
        while (cursor in set) {
            count++
            cursor--
        }
        return count
    }
}
