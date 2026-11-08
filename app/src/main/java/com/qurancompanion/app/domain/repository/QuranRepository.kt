package com.qurancompanion.app.domain.repository

import com.qurancompanion.app.domain.model.Ayah
import com.qurancompanion.app.domain.model.AyahMarker
import com.qurancompanion.app.domain.model.Juz
import com.qurancompanion.app.domain.model.QuranLoadResult
import com.qurancompanion.app.domain.model.Surah
import kotlinx.coroutines.flow.Flow

interface QuranRepository {
    /** Imports the bundled, validated Quran data on first run. Safe to call repeatedly. */
    suspend fun ensureLoaded(): QuranLoadResult

    fun observeSurahs(): Flow<List<Surah>>
    fun observeJuz(): Flow<List<Juz>>
    fun observePage(page: Int): Flow<List<Ayah>>

    suspend fun ayahId(surahId: Int, ayahNumber: Int): Int?
    suspend fun pageOfAyah(surahId: Int, ayahNumber: Int): Int?
    suspend fun getAyah(id: Int): Ayah?
    suspend fun ayahsOfRange(surahId: Int, from: Int, to: Int): List<Ayah>
    suspend fun allAyahs(): List<Ayah>

    /** Ayah id -> extra marks (hizb quarter starts, sajdas). Empty if the metadata has none. */
    suspend fun markers(): Map<Int, Set<AyahMarker>>

    /** First and last global ayah id covered by pages [fromPage]..[toPage], or null if invalid. */
    suspend fun ayahIdRangeOfPages(fromPage: Int, toPage: Int): Pair<Int, Int>?
}
