package com.qurancompanion.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.qurancompanion.app.domain.model.AppSettings
import com.qurancompanion.app.domain.model.ColorTheme
import com.qurancompanion.app.domain.model.QuranConstants
import com.qurancompanion.app.domain.model.ReminderSetting
import com.qurancompanion.app.domain.model.ReminderType
import com.qurancompanion.app.domain.model.ThemeMode
import com.qurancompanion.app.domain.repository.SettingsRepository
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class DataStoreSettingsRepository(private val context: Context) : SettingsRepository {
    private val dailyTargetKey = intPreferencesKey("daily_target_ayahs")
    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val colorThemeKey = stringPreferencesKey("color_theme")
    private val lastReadKey = intPreferencesKey("last_read_page")
    private val bookmarksKey = stringSetPreferencesKey("bookmarks")
    private val wirdReadKey = intPreferencesKey("wird_read_pages")
    private val wirdReviewKey = intPreferencesKey("wird_review_ayahs")
    private val khatmStartKey = longPreferencesKey("khatm_start_day")
    private val khatmDaysKey = intPreferencesKey("khatm_days")
    private val autoShiftKey = booleanPreferencesKey("auto_shift_reviews")
    private val rolloverDayKey = longPreferencesKey("review_rollover_day")
    private val rolloverNoteDayKey = longPreferencesKey("rollover_note_day")
    private val rolloverOverdueKey = intPreferencesKey("rollover_overdue")
    private val rolloverShiftKey = intPreferencesKey("rollover_shift")

    private fun enabledKey(type: ReminderType) = booleanPreferencesKey("reminder_${type.name}_enabled")
    private fun hourKey(type: ReminderType) = intPreferencesKey("reminder_${type.name}_hour")
    private fun minuteKey(type: ReminderType) = intPreferencesKey("reminder_${type.name}_minute")

    private val prefs: Flow<Preferences> = context.settingsDataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }

    override val dailyTargetAyahs: Flow<Int> = prefs.map { it[dailyTargetKey] ?: 0 }

    override val settings: Flow<AppSettings> = prefs.map { p ->
        AppSettings(
            themeMode = p[themeModeKey]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
            colorTheme = p[colorThemeKey]?.let { runCatching { ColorTheme.valueOf(it) }.getOrNull() } ?: ColorTheme.EMERALD,
            dailyTargetAyahs = p[dailyTargetKey] ?: 0,
            lastReadPage = (p[lastReadKey] ?: 0).coerceIn(0, QuranConstants.TOTAL_PAGES),
            bookmarks = p[bookmarksKey].orEmpty().mapNotNull { it.toIntOrNull() }
                .filter { it in 1..QuranConstants.TOTAL_PAGES }.toSet(),
            wirdReadPages = p[wirdReadKey] ?: 0,
            wirdReviewAyahs = p[wirdReviewKey] ?: 0,
            khatmStartDay = p[khatmStartKey] ?: 0L,
            khatmDays = p[khatmDaysKey] ?: 0,
            autoShiftReviews = p[autoShiftKey] ?: true,
            reviewRolloverDay = p[rolloverDayKey] ?: 0L,
            rolloverNoteDay = p[rolloverNoteDayKey] ?: 0L,
            rolloverOverdue = p[rolloverOverdueKey] ?: 0,
            rolloverShift = p[rolloverShiftKey] ?: 0,
            reminders = ReminderType.entries.associateWith { type ->
                ReminderSetting(
                    enabled = p[enabledKey(type)] ?: false,
                    hour = p[hourKey(type)] ?: type.defaultHour,
                    minute = p[minuteKey(type)] ?: type.defaultMinute,
                )
            },
        )
    }

    override suspend fun setDailyTarget(ayahs: Int) {
        context.settingsDataStore.edit { it[dailyTargetKey] = ayahs.coerceIn(0, QuranConstants.TOTAL_AYAHS) }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[themeModeKey] = mode.name }
    }

    override suspend fun setColorTheme(theme: ColorTheme) {
        context.settingsDataStore.edit { it[colorThemeKey] = theme.name }
    }

    override suspend fun setLastReadPage(page: Int) {
        context.settingsDataStore.edit { it[lastReadKey] = page.coerceIn(0, QuranConstants.TOTAL_PAGES) }
    }

    override suspend fun toggleBookmark(page: Int) {
        if (page !in 1..QuranConstants.TOTAL_PAGES) return
        context.settingsDataStore.edit { p ->
            val current = p[bookmarksKey].orEmpty()
            val key = page.toString()
            p[bookmarksKey] = if (key in current) current - key else current + key
        }
    }

    override suspend fun setWirdTargets(readPages: Int, reviewAyahs: Int) {
        context.settingsDataStore.edit { p ->
            p[wirdReadKey] = readPages.coerceIn(0, QuranConstants.TOTAL_PAGES)
            p[wirdReviewKey] = reviewAyahs.coerceIn(0, QuranConstants.TOTAL_AYAHS)
        }
    }

    override suspend fun setAutoShiftReviews(enabled: Boolean) {
        context.settingsDataStore.edit { it[autoShiftKey] = enabled }
    }

    override suspend fun setReviewRolloverDay(day: Long) {
        context.settingsDataStore.edit { it[rolloverDayKey] = day }
    }

    override suspend fun setRolloverNote(day: Long, overdueMoved: Int, shiftDays: Int) {
        context.settingsDataStore.edit { p ->
            p[rolloverNoteDayKey] = day
            p[rolloverOverdueKey] = overdueMoved
            p[rolloverShiftKey] = shiftDays
        }
    }

    override suspend fun setKhatmPlan(startDay: Long, days: Int) {
        context.settingsDataStore.edit { p ->
            p[khatmStartKey] = startDay.coerceAtLeast(0L)
            p[khatmDaysKey] = if (startDay > 0) days.coerceIn(1, 1000) else 0
        }
    }

    override suspend fun setReminder(type: ReminderType, setting: ReminderSetting) {
        context.settingsDataStore.edit { p ->
            p[enabledKey(type)] = setting.enabled
            p[hourKey(type)] = setting.hour.coerceIn(0, 23)
            p[minuteKey(type)] = setting.minute.coerceIn(0, 59)
        }
    }

    override suspend fun restore(settings: AppSettings) {
        context.settingsDataStore.edit { p ->
            p[dailyTargetKey] = settings.dailyTargetAyahs.coerceIn(0, QuranConstants.TOTAL_AYAHS)
            p[themeModeKey] = settings.themeMode.name
            p[colorThemeKey] = settings.colorTheme.name
            p[lastReadKey] = settings.lastReadPage.coerceIn(0, QuranConstants.TOTAL_PAGES)
            p[bookmarksKey] = settings.bookmarks.filter { it in 1..QuranConstants.TOTAL_PAGES }.map { it.toString() }.toSet()
            p[wirdReadKey] = settings.wirdReadPages.coerceIn(0, QuranConstants.TOTAL_PAGES)
            p[wirdReviewKey] = settings.wirdReviewAyahs.coerceIn(0, QuranConstants.TOTAL_AYAHS)
            p[khatmStartKey] = settings.khatmStartDay.coerceAtLeast(0L)
            p[khatmDaysKey] = settings.khatmDays.coerceIn(0, 1000)
            ReminderType.entries.forEach { type ->
                val r = settings.reminders[type] ?: return@forEach
                p[enabledKey(type)] = r.enabled
                p[hourKey(type)] = r.hour.coerceIn(0, 23)
                p[minuteKey(type)] = r.minute.coerceIn(0, 59)
            }
        }
    }
}
