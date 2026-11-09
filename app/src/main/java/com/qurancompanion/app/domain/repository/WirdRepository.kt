package com.qurancompanion.app.domain.repository

import com.qurancompanion.app.domain.model.KhatmState
import com.qurancompanion.app.domain.model.WirdToday
import kotlinx.coroutines.flow.Flow

interface WirdRepository {
    /** Today's recitation, memorization and review against the user's daily targets. */
    fun observeToday(): Flow<WirdToday>

    /** The running recitation khatmah, or null when there is none. */
    fun observeKhatm(): Flow<KhatmState?>

    /** Counts a mushaf page as read today. */
    suspend fun markRead(page: Int)

    /** Counts pages [fromPage]..[toPage] as read today (e.g. recited outside the app). */
    suspend fun logPages(fromPage: Int, toPage: Int)

    suspend fun startKhatm(days: Int)
    suspend fun cancelKhatm()

    /** Sets all three daily targets; 0 means no target for that part. */
    suspend fun setTargets(readPages: Int, memorizeAyahs: Int, reviewAyahs: Int)
}
