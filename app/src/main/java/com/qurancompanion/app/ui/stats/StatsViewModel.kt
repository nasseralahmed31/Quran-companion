package com.qurancompanion.app.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.qurancompanion.app.domain.model.StatsOverview
import com.qurancompanion.app.domain.repository.StatsRepository
import com.qurancompanion.app.ui.common.appContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class StatsViewModel(stats: StatsRepository) : ViewModel() {
    val overview: StateFlow<StatsOverview?> = stats.observeOverview()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    companion object {
        val Factory = viewModelFactory {
            initializer { StatsViewModel(appContainer().statsRepository) }
        }
    }
}
