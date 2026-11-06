package com.qurancompanion.app.ui.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.qurancompanion.app.domain.model.ActivityItem
import com.qurancompanion.app.domain.repository.MemorizationRepository
import com.qurancompanion.app.domain.repository.QuranRepository
import com.qurancompanion.app.ui.common.appContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ActivityUiState(
    val items: List<ActivityItem> = emptyList(),
    val surahNames: Map<Int, String> = emptyMap(),
)

class ActivityLogViewModel(
    memorization: MemorizationRepository,
    quran: QuranRepository,
) : ViewModel() {

    val state: StateFlow<ActivityUiState> = combine(
        memorization.observeActivity(),
        quran.observeSurahs(),
    ) { items, surahs -> ActivityUiState(items, surahs.associate { it.id to it.nameAr }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ActivityUiState())

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = appContainer()
                ActivityLogViewModel(c.memorizationRepository, c.quranRepository)
            }
        }
    }
}
