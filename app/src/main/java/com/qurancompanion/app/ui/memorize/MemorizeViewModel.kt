package com.qurancompanion.app.ui.memorize

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.qurancompanion.app.domain.model.MemorizationEntry
import com.qurancompanion.app.domain.repository.MemorizationRepository
import com.qurancompanion.app.domain.repository.ProgressRepository
import com.qurancompanion.app.domain.repository.QuranRepository
import com.qurancompanion.app.domain.repository.SettingsRepository
import com.qurancompanion.app.ui.common.appContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MemorizeUiState(
    val loading: Boolean = true,
    val surahNames: Map<Int, String> = emptyMap(),
    val entries: List<MemorizationEntry> = emptyList(),
    val dailyTarget: Int = 0,
    val todayDone: Int = 0,
)

class MemorizeViewModel(
    private val quran: QuranRepository,
    private val memorization: MemorizationRepository,
    private val settings: SettingsRepository,
    progress: ProgressRepository,
) : ViewModel() {

    val state: StateFlow<MemorizeUiState> = combine(
        quran.observeSurahs(),
        memorization.observeEntries(),
        progress.observeSummary(),
    ) { surahs, entries, summary ->
        MemorizeUiState(
            loading = false,
            surahNames = surahs.associate { it.id to it.nameAr },
            entries = entries,
            dailyTarget = summary.todayTargetAyahs,
            todayDone = summary.todayDoneAyahs,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MemorizeUiState())

    /** For users who entered old memorization with recent dates: it stops counting as new progress. */
    fun markAllPrevious() {
        viewModelScope.launch {
            memorization.markAllAsPrevious()
            memorization.consolidate()
        }
    }

    fun setDailyTarget(ayahs: Int) {
        viewModelScope.launch { settings.setDailyTarget(ayahs) }
    }

    /** Returns (page, ayahId) for a surah:ayah position, or null if it does not exist. */
    suspend fun resolveAyah(surah: Int, ayah: Int): Pair<Int, Int>? {
        val id = quran.ayahId(surah, ayah) ?: return null
        val page = quran.pageOfAyah(surah, ayah) ?: return null
        return page to id
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = appContainer()
                MemorizeViewModel(c.quranRepository, c.memorizationRepository, c.settingsRepository, c.progressRepository)
            }
        }
    }
}
