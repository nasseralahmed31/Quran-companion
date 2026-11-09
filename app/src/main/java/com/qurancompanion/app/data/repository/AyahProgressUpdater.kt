package com.qurancompanion.app.data.repository

import com.qurancompanion.app.data.local.AyahProgressEntity
import com.qurancompanion.app.data.local.MemorizationDao
import com.qurancompanion.app.data.local.QuranDao

/**
 * Rebuilds per-ayah progress for a range from the records covering it: the newest record
 * (by date, then id) wins; ayahs no longer covered by any record are cleared.
 */
class AyahProgressUpdater(
    private val memorization: MemorizationDao,
    private val quran: QuranDao,
) {
    suspend fun recomputeRange(surahId: Int, from: Int, to: Int) {
        val firstAyahId = quran.firstAyahId(surahId) ?: return
        val entries = memorization.entriesForSurah(surahId)
        val upserts = ArrayList<AyahProgressEntity>()
        val removals = ArrayList<Int>()
        for (n in from..to) {
            val ayahId = firstAyahId + n - 1
            val winner = entries.firstOrNull { n in it.fromAyah..it.toAyah }
            if (winner == null) {
                removals.add(ayahId)
            } else {
                upserts.add(AyahProgressEntity(ayahId, winner.id, winner.state, winner.mastery, winner.memorizedOn, winner.isPrevious))
            }
        }
        if (removals.isNotEmpty()) memorization.deleteProgress(removals)
        if (upserts.isNotEmpty()) memorization.upsertProgress(upserts)
    }
}
