package com.qurancompanion.app.domain.repository

import com.qurancompanion.app.domain.model.AppSettings
import com.qurancompanion.app.domain.model.ColorTheme
import com.qurancompanion.app.domain.model.ReminderSetting
import com.qurancompanion.app.domain.model.ReminderType
import com.qurancompanion.app.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    /** Daily memorization target in ayahs; 0 means no target set. */
    val dailyTargetAyahs: Flow<Int>
    suspend fun setDailyTarget(ayahs: Int)

    val settings: Flow<AppSettings>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setColorTheme(theme: ColorTheme)
    suspend fun setReminder(type: ReminderType, setting: ReminderSetting)

    /** Last mushaf page the user was reading (0 = none yet). */
    suspend fun setLastReadPage(page: Int)

    /** Adds the page to the bookmarks, or removes it if it is already there. */
    suspend fun toggleBookmark(page: Int)

    /** Replaces every setting (used when restoring a backup). */
    suspend fun restore(settings: AppSettings)
}
