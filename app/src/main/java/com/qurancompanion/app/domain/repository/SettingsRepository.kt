package com.qurancompanion.app.domain.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    /** Daily memorization target in ayahs; 0 means no target set. */
    val dailyTargetAyahs: Flow<Int>
    suspend fun setDailyTarget(ayahs: Int)
}
