package com.qurancompanion.app.data.quran

import android.content.Context
import androidx.room.withTransaction
import com.qurancompanion.app.data.local.AyahEntity
import com.qurancompanion.app.data.local.JuzEntity
import com.qurancompanion.app.data.local.QuranDatabase
import com.qurancompanion.app.data.local.SurahEntity
import com.qurancompanion.app.domain.model.QuranConstants
import com.qurancompanion.app.domain.model.QuranLoadResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Imports the bundled Tanzil files into Room, only after they pass validation. */
class QuranImporter(
    private val context: Context,
    private val db: QuranDatabase,
) {
    private val mutex = Mutex()

    suspend fun ensureImported(): QuranLoadResult = mutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                importInternal()
            } catch (e: CancellationException) {
                throw e
            } catch (e: QuranDataException) {
                QuranLoadResult.Invalid(e.message.orEmpty())
            } catch (e: Exception) {
                QuranLoadResult.Failed(e.message ?: e.javaClass.simpleName)
            }
        }
    }

    private suspend fun importInternal(): QuranLoadResult {
        val dao = db.quranDao()
        if (dao.surahCount() == QuranConstants.TOTAL_SURAHS &&
            dao.ayahCount() == QuranConstants.TOTAL_AYAHS &&
            dao.juzCount() == QuranConstants.TOTAL_JUZ
        ) {
            return QuranLoadResult.Ready
        }
        val present = context.assets.list(ASSET_DIR)?.toSet().orEmpty()
        if (TEXT_FILE !in present || META_FILE !in present) return QuranLoadResult.MissingFiles

        val text = context.assets.open("$ASSET_DIR/$TEXT_FILE").use { QuranSourceParser.parseText(it) }
        val meta = context.assets.open("$ASSET_DIR/$META_FILE").use { QuranSourceParser.parseMetadata(it) }
        QuranSourceParser.validate(text, meta)

        val (surahs, juz, ayahs) = build(text, meta)
        db.withTransaction {
            dao.deleteAyahs()
            dao.deleteSurahs()
            dao.deleteJuz()
            dao.insertSurahs(surahs)
            dao.insertJuz(juz)
            ayahs.chunked(500).forEach { dao.insertAyahs(it) }
        }
        return QuranLoadResult.Ready
    }

    /** Hizb-quarter and sajda positions from the bundled metadata; empty if unavailable. */
    fun readMarkers(): MarkerPositions = try {
        val present = context.assets.list(ASSET_DIR)?.toSet().orEmpty()
        if (META_FILE !in present) {
            MarkerPositions(emptyList(), emptyList())
        } else {
            context.assets.open("$ASSET_DIR/$META_FILE").use { QuranSourceParser.parseMarkers(it) }
        }
    } catch (e: Exception) {
        MarkerPositions(emptyList(), emptyList())
    }

    private fun build(
        text: List<RawAyah>,
        meta: QuranMetadata,
    ): Triple<List<SurahEntity>, List<JuzEntity>, List<AyahEntity>> {
        val sorted = text.sortedBy { it.key }
        var j = 0
        var p = 0
        val ayahs = sorted.mapIndexed { i, a ->
            while (j + 1 < meta.juz.size && meta.juz[j + 1].key <= a.key) j++
            while (p + 1 < meta.pages.size && meta.pages[p + 1].key <= a.key) p++
            AyahEntity(
                id = i + 1,
                surahId = a.sura,
                ayahNumber = a.aya,
                text = a.text,
                juz = meta.juz[j].index,
                page = meta.pages[p].index,
            )
        }
        val byKey = ayahs.associateBy { it.surahId * 1000 + it.ayahNumber }
        val surahs = meta.suras.map { s ->
            val first = byKey.getValue(s.index * 1000 + 1)
            SurahEntity(s.index, s.name, s.translit, s.english, s.ayas, s.type, first.page, first.id)
        }
        val juz = meta.juz.map { pos ->
            val first = byKey.getValue(pos.key)
            JuzEntity(pos.index, pos.sura, pos.aya, first.page, first.id)
        }
        return Triple(surahs, juz, ayahs)
    }

    companion object {
        const val ASSET_DIR = "quran"
        const val TEXT_FILE = "quran-uthmani.txt"
        const val META_FILE = "quran-data.xml"
    }
}
