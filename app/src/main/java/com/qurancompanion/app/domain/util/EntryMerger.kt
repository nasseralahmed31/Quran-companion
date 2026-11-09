package com.qurancompanion.app.domain.util

import com.qurancompanion.app.domain.model.MasteryLevel
import com.qurancompanion.app.domain.model.MemorizationEntry
import com.qurancompanion.app.domain.model.MemorizationState

/**
 * Keeps one record per run of ayahs: a new or edited record is merged with the records of the same
 * surah that overlap or touch it (same state, same old/new kind), e.g. ayahs 1-100 then 101-105
 * (or 1-105) become the single record 1-105.
 */
object EntryMerger {
    data class Other(
        val id: Long,
        val fromAyah: Int,
        val toAyah: Int,
        val state: MemorizationState,
        val mastery: MasteryLevel,
        val isPrevious: Boolean,
        val note: String,
        val memorizedOn: Long,
    )

    /**
     * [entry] is the merged record; [mergedIds] are the existing records it absorbed (to delete);
     * [partialIds] are the absorbed ones that were only partly covered by the new range, whose review
     * schedules still matter.
     */
    data class Result(val entry: MemorizationEntry, val mergedIds: List<Long>, val partialIds: List<Long>)

    fun merge(entry: MemorizationEntry, others: List<Other>): Result {
        val touching = others.filter {
            it.id != entry.id &&
                it.state == entry.state &&
                it.isPrevious == entry.isPrevious &&
                it.toAyah + 1 >= entry.fromAyah &&
                it.fromAyah - 1 <= entry.toAyah
        }
        if (touching.isEmpty()) return Result(entry, emptyList(), emptyList())

        // A record fully inside the new range is simply replaced by it.
        val covered = touching.filter { it.fromAyah >= entry.fromAyah && it.toAyah <= entry.toAyah }
        val partial = touching.filter { it !in covered }

        val from = minOf(entry.fromAyah, touching.minOf { it.fromAyah })
        val to = maxOf(entry.toAyah, touching.maxOf { it.toAyah })
        // The merged passage is only as strong as its weakest part that was not replaced.
        val mastery = if (entry.state == MemorizationState.IN_PROGRESS) {
            MasteryLevel.IN_PROGRESS
        } else {
            (listOf(entry.mastery) + partial.map { it.mastery }).minByOrNull { ReviewOrdering.masteryRank(it) } ?: entry.mastery
        }
        val note = (listOf(entry.note) + touching.map { it.note })
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .joinToString("؛ ")
            .take(500)
        val date = maxOf(entry.memorizedOn, touching.maxOf { it.memorizedOn })
        return Result(
            entry = entry.copy(fromAyah = from, toAyah = to, mastery = mastery, note = note, memorizedOn = date),
            mergedIds = touching.map { it.id },
            partialIds = partial.map { it.id },
        )
    }
}
