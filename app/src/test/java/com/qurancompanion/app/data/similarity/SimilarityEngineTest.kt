package com.qurancompanion.app.data.similarity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Uses invented word sequences, not Quran text. */
class SimilarityEngineTest {
    @Test
    fun normalizationIgnoresDiacriticsAndAlefVariants() {
        assertEquals(ArabicText.normalizeWord("أَبْجَد"), ArabicText.normalizeWord("ابجد"))
        assertEquals("ابجد", ArabicText.normalizeWord("إبجد"))
    }

    @Test
    fun marksOnlyTokensAreGluedToThePreviousWord() {
        val tokens = ArabicText.tokenize("كلمة \u06D6 اخرى")
        assertEquals(2, tokens.original.size)
        assertEquals(2, tokens.normalized.size)
    }

    @Test
    fun similarTextsArePairedAndDifferentOnesAreNot() {
        val ayahs = listOf(
            AyahText(1, "الف باء جيم دال هاء واو زاي"),
            AyahText(2, "الف باء جيم دال هاء واو حاء"),
            AyahText(3, "ميم نون سين عين فاء صاد قاف"),
        )
        val pairs = SimilarityEngine().compute(ayahs)
        assertEquals(1, pairs.size)
        assertEquals(1, pairs[0].a)
        assertEquals(2, pairs[0].b)
        assertTrue(pairs[0].score >= 0.45)
    }

    @Test
    fun veryShortTextsAreIgnored() {
        val pairs = SimilarityEngine().compute(listOf(AyahText(1, "الف باء جيم"), AyahText(2, "الف باء جيم")))
        assertTrue(pairs.isEmpty())
    }

    @Test
    fun diffMarksOnlyTheWordsThatAreNotShared() {
        val diff = WordDiff.highlight(listOf("الف باء جيم", "الف باء دال"))
        assertFalse(diff[0][0].differs)
        assertFalse(diff[0][1].differs)
        assertTrue(diff[0][2].differs)
        assertTrue(diff[1][2].differs)
    }

    @Test
    fun commonPrefixLengthCountsLeadingEqualWords() {
        assertEquals(2, ArabicText.commonPrefixLength(listOf("a", "b", "c"), listOf("a", "b", "d")))
        assertEquals(0, ArabicText.commonPrefixLength(listOf("a"), listOf("b")))
    }
}
