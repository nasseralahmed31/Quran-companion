package com.qurancompanion.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface QuranDao {
    @Query("SELECT * FROM surahs ORDER BY id")
    fun observeSurahs(): Flow<List<SurahEntity>>

    @Query("SELECT * FROM juz ORDER BY id")
    fun observeJuz(): Flow<List<JuzEntity>>

    @Query("SELECT * FROM ayahs WHERE page = :page ORDER BY id")
    fun observePage(page: Int): Flow<List<AyahEntity>>

    @Query("SELECT id FROM ayahs WHERE surahId = :surahId AND ayahNumber = :ayahNumber LIMIT 1")
    suspend fun ayahId(surahId: Int, ayahNumber: Int): Int?

    @Query("SELECT page FROM ayahs WHERE surahId = :surahId AND ayahNumber = :ayahNumber LIMIT 1")
    suspend fun pageOfAyah(surahId: Int, ayahNumber: Int): Int?

    @Query("SELECT COUNT(*) FROM surahs")
    suspend fun surahCount(): Int

    @Query("SELECT COUNT(*) FROM ayahs")
    suspend fun ayahCount(): Int

    @Query("SELECT COUNT(*) FROM juz")
    suspend fun juzCount(): Int

    @Insert
    suspend fun insertSurahs(items: List<SurahEntity>)

    @Insert
    suspend fun insertAyahs(items: List<AyahEntity>)

    @Insert
    suspend fun insertJuz(items: List<JuzEntity>)

    @Query("DELETE FROM surahs")
    suspend fun deleteSurahs()

    @Query("DELETE FROM ayahs")
    suspend fun deleteAyahs()

    @Query("DELETE FROM juz")
    suspend fun deleteJuz()

    @Query("SELECT firstAyahId FROM surahs WHERE id = :surahId")
    suspend fun firstAyahId(surahId: Int): Int?
}
