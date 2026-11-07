package com.qurancompanion.app.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.qurancompanion.app.domain.model.SurahMapEntry
import com.qurancompanion.app.domain.repository.StatsRepository
import com.qurancompanion.app.ui.common.appContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class MapViewModel(stats: StatsRepository) : ViewModel() {
    val entries: StateFlow<List<SurahMapEntry>> = stats.observeSurahMap()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    companion object {
        val Factory = viewModelFactory {
            initializer { MapViewModel(appContainer().statsRepository) }
        }
    }
}
