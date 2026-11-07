package com.qurancompanion.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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
            ReminderType.entries.forEach { type ->
                val r = settings.reminders[type] ?: return@forEach
                p[enabledKey(type)] = r.enabled
                p[hourKey(type)] = r.hour.coerceIn(0, 23)
                p[minuteKey(type)] = r.minute.coerceIn(0, 59)
            }
        }
    }
}
