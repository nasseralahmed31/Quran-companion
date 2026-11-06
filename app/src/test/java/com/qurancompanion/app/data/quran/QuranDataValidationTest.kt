package com.qurancompanion.app.data.quran

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class QuranDataValidationTest {
    private val dir = File("src/main/assets/quran")

    @Test
    fun bundledQuranDataMatchesStandardCounts() {
        val textFile = File(dir, "quran-uthmani.txt")
        val metaFile = File(dir, "quran-data.xml")
        assumeTrue("Quran data files are not bundled yet", textFile.exists() && metaFile.exists())

        val text = textFile.inputStream().use { QuranSourceParser.parseText(it) }
        val meta = metaFile.inputStream().use { QuranSourceParser.parseMetadata(it) }
        QuranSourceParser.validate(text, meta)

        assertEquals(6236, text.size)
        assertEquals(114, meta.suras.size)
        assertEquals(30, meta.juz.size)
        assertEquals(604, meta.pages.size)
    }

    @Test
    fun validatorRejectsIncompleteData() {
        val text = listOf(RawAyah(1, 1, "x"))
        val meta = QuranMetadata(emptyList(), emptyList(), emptyList())
        val failed = try {
            QuranSourceParser.validate(text, meta)
            false
        } catch (e: QuranDataException) {
            true
        }
        assertTrue(failed)
    }
}
