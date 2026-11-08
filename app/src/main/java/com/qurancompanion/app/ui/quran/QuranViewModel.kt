package com.qurancompanion.app.ui.quran

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.qurancompanion.app.domain.model.Juz
import com.qurancompanion.app.domain.model.QuranLoadResult
import com.qurancompanion.app.domain.model.Surah
import com.qurancompanion.app.domain.repository.QuranRepository
import com.qurancompanion.app.domain.repository.SettingsRepository
import com.qurancompanion.app.ui.common.appContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface QuranUiState {
    data object Loading : QuranUiState
    data class Problem(val result: QuranLoadResult) : QuranUiState
    data class Ready(val surahs: List<Surah>, val juz: List<Juz>) : QuranUiState
}

class QuranViewModel(
    private val repository: QuranRepository,
    settings: SettingsRepository,
) : ViewModel() {
    val lastReadPage: StateFlow<Int> = settings.settings.map { it.lastReadPage }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val _state = MutableStateFlow<QuranUiState>(QuranUiState.Loading)
    val state: StateFlow<QuranUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val result = repository.ensureLoaded()
            if (result == QuranLoadResult.Ready) {
                combine(repository.observeSurahs(), repository.observeJuz()) { surahs, juz ->
                    QuranUiState.Ready(surahs, juz)
                }.collect { _state.value = it }
            } else {
                _state.value = QuranUiState.Problem(result)
            }
        }
    }

    /** Returns (page, ayahId) for a surah:ayah position, or null if it does not exist. */
    suspend fun resolveAyah(surah: Int, ayah: Int): Pair<Int, Int>? {
        val id = repository.ayahId(surah, ayah) ?: return null
        val page = repository.pageOfAyah(surah, ayah) ?: return null
        return page to id
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = appContainer()
                QuranViewModel(c.quranRepository, c.settingsRepository)
            }
        }
    }
}
