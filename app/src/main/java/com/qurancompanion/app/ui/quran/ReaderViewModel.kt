package com.qurancompanion.app.ui.quran

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.qurancompanion.app.domain.model.Ayah
import com.qurancompanion.app.domain.model.AyahMark
import com.qurancompanion.app.domain.repository.MemorizationRepository
import com.qurancompanion.app.domain.repository.QuranRepository
import com.qurancompanion.app.ui.common.appContainer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class ReaderViewModel(
    private val quran: QuranRepository,
    private val memorization: MemorizationRepository,
) : ViewModel() {
    val surahNames: StateFlow<Map<Int, String>> = quran.observeSurahs()
        .map { list -> list.associate { it.id to it.nameAr } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun observePage(page: Int): Flow<List<Ayah>> = quran.observePage(page)

    fun observeMarks(page: Int): Flow<Map<Int, AyahMark>> = memorization.observePageMarks(page)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = appContainer()
                ReaderViewModel(c.quranRepository, c.memorizationRepository)
            }
        }
    }
}
