package com.qurancompanion.app.domain.util

import com.qurancompanion.app.domain.model.DayPlan
import com.qurancompanion.app.domain.model.GoalStatus

object GoalPlanner {
    /** Days left including today. */
    fun daysLeft(endDay: Long, today: Long): Int = (endDay - today + 1).toInt().coerceAtLeast(0)

    fun requiredPerDay(remainingAyahs: Int, daysLeft: Int): Int = when {
        remainingAyahs <= 0 -> 0
        daysLeft <= 0 -> remainingAyahs
        else -> (remainingAyahs + daysLeft - 1) / daysLeft
    }

    /** Splits [startAyahId]..[endAyahId] into [days] consecutive parts as evenly as possible. */
    fun schedule(startAyahId: Int, endAyahId: Int, days: Int): List<DayPlan> {
        val total = endAyahId - startAyahId + 1
        if (total <= 0 || days <= 0) return emptyList()
        val parts = minOf(days, total)
        val base = total / parts
        val remainder = total % parts
        val plans = ArrayList<DayPlan>(parts)
        var cursor = startAyahId
        for (i in 0 until parts) {
            val size = base + if (i < remainder) 1 else 0
            plans.add(DayPlan(i + 1, cursor, cursor + size - 1))
            cursor += size
        }
        return plans
    }

    fun status(done: Int, total: Int, startDay: Long, endDay: Long, today: Long): GoalStatus {
        if (total > 0 && done >= total) return GoalStatus.COMPLETED
        if (today > endDay) return GoalStatus.OVERDUE
        val totalDays = (endDay - startDay + 1).coerceAtLeast(1)
        val elapsed = (today - startDay + 1).coerceIn(0, totalDays)
        val expected = total * elapsed.toDouble() / totalDays
        return when {
            done >= expected * 1.05 -> GoalStatus.AHEAD
            done >= expected * 0.9 -> GoalStatus.ON_TRACK
            else -> GoalStatus.BEHIND
        }
    }
}
