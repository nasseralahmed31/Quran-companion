package com.qurancompanion.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// 'MEMORIZED' must match MemorizationState.MEMORIZED.name.
private const val COMPLETED_SURAHS_SQL =
    "SELECT COUNT(*) FROM surahs s WHERE s.ayahCount = (SELECT COUNT(*) FROM ayah_progress p " +
        "INNER JOIN ayahs a ON a.id = p.ayahId WHERE a.surahId = s.id AND p.state = 'MEMORIZED')"

private const val MEMORIZED_JUZ_SQL =
    "SELECT COUNT(*) FROM (SELECT a.juz AS g FROM ayahs a LEFT JOIN ayah_progress p " +
        "ON p.ayahId = a.id AND p.state = 'MEMORIZED' GROUP BY a.juz HAVING COUNT(*) = COUNT(p.ayahId)) t"

private const val MEMORIZED_PAGES_SQL =
    "SELECT COUNT(*) FROM (SELECT a.page AS g FROM ayahs a LEFT JOIN ayah_progress p " +
        "ON p.ayahId = a.id AND p.state = 'MEMORIZED' GROUP BY a.page HAVING COUNT(*) = COUNT(p.ayahId)) t"

@Dao
interface MemorizationDao {
    // ----- records -----
    @Insert
    suspend fun insertEntry(entry: MemorizationEntryEntity): Long

    @Update
    suspend fun updateEntry(entry: MemorizationEntryEntity)

    @Query("DELETE FROM memorization_entries WHERE id = :id")
    suspend fun deleteEntry(id: Long)

    @Query("SELECT * FROM memorization_entries WHERE id = :id")
    suspend fun getEntry(id: Long): MemorizationEntryEntity?

    @Query("SELECT * FROM memorization_entries ORDER BY memorizedOn DESC, id DESC")
    fun observeEntries(): Flow<List<MemorizationEntryEntity>>

    @Query("SELECT * FROM memorization_entries WHERE surahId = :surahId ORDER BY memorizedOn DESC, id DESC")
    suspend fun entriesForSurah(surahId: Int): List<MemorizationEntryEntity>

    @Query("SELECT DISTINCT memorizedOn FROM memorization_entries ORDER BY memorizedOn DESC")
    fun observeEntryDays(): Flow<List<Long>>

    @Query("SELECT DISTINCT memorizedOn FROM memorization_entries ORDER BY memorizedOn DESC")
    suspend fun entryDays(): List<Long>

    // ----- per-ayah progress -----
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgress(items: List<AyahProgressEntity>)

    @Query("DELETE FROM ayah_progress WHERE ayahId IN (:ids)")
    suspend fun deleteProgress(ids: List<Int>)

    @Query(
        "SELECT COUNT(*) FROM ayah_progress p INNER JOIN ayahs a ON a.id = p.ayahId " +
            "WHERE a.surahId = :surahId AND a.ayahNumber BETWEEN :from AND :to",
    )
    suspend fun recordedInRange(surahId: Int, from: Int, to: Int): Int

    @Query("SELECT p.* FROM ayah_progress p INNER JOIN ayahs a ON a.id = p.ayahId WHERE a.page = :page")
    fun observePageProgress(page: Int): Flow<List<AyahProgressEntity>>

    // ----- aggregates (state = MEMORIZED only) -----
    @Query("SELECT COUNT(*) FROM ayah_progress WHERE state = 'MEMORIZED'")
    fun observeMemorizedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM ayah_progress WHERE state = 'MEMORIZED'")
    suspend fun memorizedCount(): Int

    @Query(COMPLETED_SURAHS_SQL)
    fun observeCompletedSurahsCount(): Flow<Int>

    @Query(COMPLETED_SURAHS_SQL)
    suspend fun completedSurahsCount(): Int

    @Query(MEMORIZED_JUZ_SQL)
    fun observeMemorizedJuzCount(): Flow<Int>

    @Query(MEMORIZED_JUZ_SQL)
    suspend fun memorizedJuzCount(): Int

    @Query(MEMORIZED_PAGES_SQL)
    fun observeMemorizedPagesCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM ayah_progress WHERE memorizedOn = :day AND state = 'MEMORIZED'")
    fun observeMemorizedOnDay(day: Long): Flow<Int>

    // ----- activity & achievements -----
    @Insert
    suspend fun insertActivity(item: ActivityEntity)

    @Query("SELECT * FROM activity_log ORDER BY timestamp DESC, id DESC LIMIT 500")
    fun observeActivity(): Flow<List<ActivityEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAchievement(item: AchievementEntity): Long

    @Query("SELECT * FROM achievements")
    fun observeAchievements(): Flow<List<AchievementEntity>>

    @Query("SELECT achievementKey FROM achievements")
    suspend fun unlockedKeys(): List<String>
}
