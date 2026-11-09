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

    /** Daily recitation (pages) and review (ayahs) targets; 0 means none. */
    suspend fun setWirdTargets(readPages: Int, reviewAyahs: Int)

    /** Automatically move review dates forward when review days are missed. */
    suspend fun setAutoShiftReviews(enabled: Boolean)

    /** The last day the review schedule was rolled forward. */
    suspend fun setReviewRolloverDay(day: Long)

    /** Remembers what the last roll-forward did, for a note on the review screen. */
    suspend fun setRolloverNote(day: Long, overdueMoved: Int, shiftDays: Int)

    /** Starts (startDay > 0) or cancels (0) the recitation khatmah plan. */
    suspend fun setKhatmPlan(startDay: Long, days: Int)

    /** Replaces every setting (used when restoring a backup). */
    suspend fun restore(settings: AppSettings)
}
