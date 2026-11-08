package com.qurancompanion.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.qurancompanion.app.domain.model.ProgressSummary
import com.qurancompanion.app.domain.repository.ProgressRepository
import com.qurancompanion.app.domain.repository.SettingsRepository
import com.qurancompanion.app.ui.common.appContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(repository: ProgressRepository, settings: SettingsRepository) : ViewModel() {
    val summary: StateFlow<ProgressSummary> = repository.observeSummary()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressSummary())

    /** Last mushaf page read (0 = none yet), for the "continue reading" card. */
    val lastReadPage: StateFlow<Int> = settings.settings.map { it.lastReadPage }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = appContainer()
                HomeViewModel(c.progressRepository, c.settingsRepository)
            }
        }
    }
}
