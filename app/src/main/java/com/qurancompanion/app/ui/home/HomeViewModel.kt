package com.qurancompanion.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.qurancompanion.app.QuranCompanionApp
import com.qurancompanion.app.domain.model.ProgressSummary
import com.qurancompanion.app.domain.repository.ProgressRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(repository: ProgressRepository) : ViewModel() {
    val summary: StateFlow<ProgressSummary> = repository.observeSummary()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressSummary())

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as QuranCompanionApp
                HomeViewModel(app.container.progressRepository)
            }
        }
    }
}
