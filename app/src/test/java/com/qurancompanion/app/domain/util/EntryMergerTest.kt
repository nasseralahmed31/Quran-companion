package com.qurancompanion.app.domain.util

import com.qurancompanion.app.domain.model.MasteryLevel
import com.qurancompanion.app.domain.model.MemorizationEntry
import com.qurancompanion.app.domain.model.MemorizationState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EntryMergerTest {
    private fun entry(
        from: Int, to: Int,
        state: MemorizationState = MemorizationState.MEMORIZED,
        mastery: MasteryLevel = MasteryLevel.GOOD,
        previous: Boolean = false,
        note: String = "",
        date: Long = 100,
    ) = MemorizationEntry(0, 2, from, to, date, state, mastery, note, previous)

    private fun other(
        id: Long, from: Int, to: Int,
        state: MemorizationState = MemorizationState.MEMORIZED,
        mastery: MasteryLevel = MasteryLevel.GOOD,
        previous: Boolean = false,
        note: String = "",
        date: Long = 90,
    ) = EntryMerger.Other(id, from, to, state, mastery, previous, note, date)

    @Test
    fun continuingRightAfterAnExistingRecordGivesOneRecord() {
        val result = EntryMerger.merge(entry(101, 105), listOf(other(1, 1, 100)))
        assertEquals(1, result.entry.fromAyah)
        assertEquals(105, result.entry.toAyah)
        assertEquals(listOf(1L), result.mergedIds)
    }

    @Test
    fun enteringTheSameSurahAgainWithALargerRangeReplacesTheOldOne() {
        val result = EntryMerger.merge(entry(1, 105, mastery = MasteryLevel.MASTERED), listOf(other(1, 1, 100, mastery = MasteryLevel.WEAK)))
        assertEquals(1, result.entry.fromAyah)
        assertEquals(105, result.entry.toAyah)
        // The old record was fully covered, so its weakness does not hold the new one back.
        assertEquals(MasteryLevel.MASTERED, result.entry.mastery)
        assertTrue(result.partialIds.isEmpty())
    }

    @Test
    fun aPartlyCoveredWeakerNeighbourKeepsTheMergedRecordCautious() {
        val result = EntryMerger.merge(entry(90, 110), listOf(other(1, 1, 100, mastery = MasteryLevel.NEEDS_REVIEW)))
        assertEquals(MasteryLevel.NEEDS_REVIEW, result.entry.mastery)
        assertEquals(listOf(1L), result.partialIds)
    }

    @Test
    fun separateRunsAndDifferentKindsAreNotMerged() {
        assertTrue(EntryMerger.merge(entry(150, 160), listOf(other(1, 1, 100))).mergedIds.isEmpty())
        assertTrue(EntryMerger.merge(entry(101, 105), listOf(other(1, 1, 100, state = MemorizationState.IN_PROGRESS))).mergedIds.isEmpty())
        assertTrue(EntryMerger.merge(entry(101, 105), listOf(other(1, 1, 100, previous = true))).mergedIds.isEmpty())
    }

    @Test
    fun notesAreCombinedWithoutDuplicatesAndTheNewestDateWins() {
        val result = EntryMerger.merge(
            entry(101, 105, note = "ب", date = 100),
            listOf(other(1, 1, 100, note = "أ", date = 90), other(2, 106, 110, note = "ب", date = 95)),
        )
        assertEquals("ب؛ أ", result.entry.note)
        assertEquals(100L, result.entry.memorizedOn)
        assertEquals(1, result.entry.fromAyah)
        assertEquals(110, result.entry.toAyah)
    }

    @Test
    fun theRecordBeingEditedIsNotMergedWithItself() {
        val edited = entry(1, 105).copy(id = 7)
        val result = EntryMerger.merge(edited, listOf(other(7, 1, 100)))
        assertTrue(result.mergedIds.isEmpty())
        assertEquals(105, result.entry.toAyah)
    }
}
