package com.qurancompanion.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.qurancompanion.app.domain.model.QuranConstants
import com.qurancompanion.app.domain.repository.SettingsRepository
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class DataStoreSettingsRepository(private val context: Context) : SettingsRepository {
    private val dailyTargetKey = intPreferencesKey("daily_target_ayahs")

    override val dailyTargetAyahs: Flow<Int> = context.settingsDataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it[dailyTargetKey] ?: 0 }

    override suspend fun setDailyTarget(ayahs: Int) {
        context.settingsDataStore.edit { it[dailyTargetKey] = ayahs.coerceIn(0, QuranConstants.TOTAL_AYAHS) }
    }
}
