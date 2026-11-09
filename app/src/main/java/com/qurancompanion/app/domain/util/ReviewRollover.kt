package com.qurancompanion.app.domain.util

import com.qurancompanion.app.domain.model.MasteryLevel

data class RolloverItem(
    val entryId: Long,
    val nextReviewDay: Long,
    val ayahCount: Int,
    val mastery: MasteryLevel,
)

data class RolloverChange(val entryId: Long, val newNextDay: Long)

data class RolloverResult(val changes: List<RolloverChange>, val overdueMoved: Int, val shiftDays: Int) {
    companion object {
        val NONE = RolloverResult(emptyList(), 0, 0)
    }
}

/**
 * When review days were missed, the schedule moves forward instead of piling up as "overdue":
 * overdue passages are re-dated from today (weakest first, spread by a daily cap), and the
 * passages that were not yet due are pushed back by the number of days with no review, so the
 * spacing between reviews is preserved.
 */
object ReviewRollover {
    fun compute(
        items: List<RolloverItem>,
        today: Long,
        lastProcessedDay: Long,
        reviewedDays: Set<Long>,
        dailyCapAyahs: Int,
    ): RolloverResult {
        if (lastProcessedDay <= 0 || today <= lastProcessedDay) return RolloverResult.NONE
        val overdue = items.filter { it.nextReviewDay < today }
        if (overdue.isEmpty()) return RolloverResult.NONE

        val skippedDays = (lastProcessedDay until today).count { it !in reviewedDays }
        val changes = ArrayList<RolloverChange>()

        // Weakest first, then the one that has waited longest.
        val ordered = overdue.sortedWith(
            compareBy<RolloverItem> { ReviewOrdering.masteryRank(it.mastery) }
                .thenBy { it.nextReviewDay }
                .thenBy { it.entryId },
        )
        var day = today
        var load = 0
        for (item in ordered) {
            if (dailyCapAyahs > 0 && load > 0 && load + item.ayahCount > dailyCapAyahs) {
                day++
                load = 0
            }
            changes.add(RolloverChange(item.entryId, day))
            load += item.ayahCount
        }

        if (skippedDays > 0) {
            items.filter { it.nextReviewDay >= today }
                .forEach { changes.add(RolloverChange(it.entryId, it.nextReviewDay + skippedDays)) }
        }
        return RolloverResult(changes, overdue.size, skippedDays)
    }
}
