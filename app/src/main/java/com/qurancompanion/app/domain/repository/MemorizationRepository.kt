package com.qurancompanion.app.domain.repository

import com.qurancompanion.app.domain.model.ActivityItem
import com.qurancompanion.app.domain.model.AyahMark
import com.qurancompanion.app.domain.model.MasteryLevel
import com.qurancompanion.app.domain.model.MemorizationEntry
import com.qurancompanion.app.domain.model.MemorizationState
import com.qurancompanion.app.domain.model.SaveResult
import kotlinx.coroutines.flow.Flow

interface MemorizationRepository {
    fun observeEntries(): Flow<List<MemorizationEntry>>
    suspend fun getEntry(id: Long): MemorizationEntry?

    /** How many ayahs in the range already have a memorization record (to avoid duplicate records). */
    suspend fun countRecordedInRange(surahId: Int, from: Int, to: Int): Int

    /** Inserts (id == 0) or updates a record, then refreshes per-ayah progress, review schedule and achievements. */
    suspend fun save(entry: MemorizationEntry): SaveResult

    /**
     * Records a range given by global ayah ids (e.g. "pages 1 to 17"); a range that crosses
     * surahs becomes one record per surah, all in one transaction.
     */
    suspend fun saveRange(
        startAyahId: Int,
        endAyahId: Int,
        memorizedOn: Long,
        state: MemorizationState,
        mastery: MasteryLevel,
        note: String,
    ): SaveResult

    suspend fun delete(id: Long)

    fun observeActivity(): Flow<List<ActivityItem>>

    /** Achievement key -> unlock time (millis). */
    fun observeUnlocked(): Flow<Map<String, Long>>

    /** Ayah id -> highlight kind, for the ayahs of one mushaf page. */
    fun observePageMarks(page: Int): Flow<Map<Int, AyahMark>>
}
