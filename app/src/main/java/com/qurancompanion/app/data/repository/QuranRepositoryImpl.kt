package com.qurancompanion.app.data.repository

import com.qurancompanion.app.data.local.AyahEntity
import com.qurancompanion.app.data.local.JuzEntity
import com.qurancompanion.app.data.local.QuranDao
import com.qurancompanion.app.data.local.SurahEntity
import com.qurancompanion.app.data.quran.QuranImporter
import com.qurancompanion.app.domain.model.Ayah
import com.qurancompanion.app.domain.model.Juz
import com.qurancompanion.app.domain.model.QuranLoadResult
import com.qurancompanion.app.domain.model.Surah
import com.qurancompanion.app.domain.repository.QuranRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class QuranRepositoryImpl(
    private val dao: QuranDao,
    private val importer: QuranImporter,
) : QuranRepository {

    override suspend fun ensureLoaded(): QuranLoadResult = importer.ensureImported()

    override fun observeSurahs(): Flow<List<Surah>> =
        dao.observeSurahs().map { list -> list.map { it.toDomain() } }

    override fun observeJuz(): Flow<List<Juz>> =
        dao.observeJuz().map { list -> list.map { it.toDomain() } }

    override fun observePage(page: Int): Flow<List<Ayah>> =
        dao.observePage(page).map { list -> list.map { it.toDomain() } }

    override suspend fun ayahId(surahId: Int, ayahNumber: Int): Int? = dao.ayahId(surahId, ayahNumber)

    override suspend fun pageOfAyah(surahId: Int, ayahNumber: Int): Int? = dao.pageOfAyah(surahId, ayahNumber)

    override suspend fun getAyah(id: Int): Ayah? = dao.getAyah(id)?.toDomain()

    override suspend fun ayahsOfRange(surahId: Int, from: Int, to: Int): List<Ayah> =
        dao.ayahsInRange(surahId, from, to).map { it.toDomain() }

    override suspend fun allAyahs(): List<Ayah> = dao.allAyahs().map { it.toDomain() }

    override suspend fun ayahIdRangeOfPages(fromPage: Int, toPage: Int): Pair<Int, Int>? {
        if (fromPage < 1 || toPage < fromPage) return null
        val first = dao.minAyahIdOnPage(fromPage) ?: return null
        val last = dao.maxAyahIdOnPage(toPage) ?: return null
        return first to last
    }
}

private fun SurahEntity.toDomain() =
    Surah(id, nameAr, nameTranslit, nameEn, ayahCount, revelationType, startPage, firstAyahId)

private fun AyahEntity.toDomain() = Ayah(id, surahId, ayahNumber, text, juz, page)

private fun JuzEntity.toDomain() = Juz(id, startSurah, startAyah, startPage, firstAyahId)
