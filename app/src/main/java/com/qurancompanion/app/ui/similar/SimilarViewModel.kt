package com.qurancompanion.app.ui.similar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.qurancompanion.app.domain.model.Ayah
import com.qurancompanion.app.domain.model.Confusion
import com.qurancompanion.app.domain.model.SimilarGroup
import com.qurancompanion.app.domain.repository.QuranRepository
import com.qurancompanion.app.domain.repository.SimilarityRepository
import com.qurancompanion.app.ui.common.appContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SimilarUiState(
    val loading: Boolean = true,
    val ready: Boolean = false,
    val groups: List<SimilarGroup> = emptyList(),
    val groupIdByAyah: Map<Int, Int> = emptyMap(),
    val ayahs: Map<Int, Ayah> = emptyMap(),
    val surahNames: Map<Int, String> = emptyMap(),
    val memorized: Set<Int> = emptySet(),
    val confusions: List<Confusion> = emptyList(),
    val onlyMemorized: Boolean = true,
)

class SimilarViewModel(
    private val similarity: SimilarityRepository,
    private val quran: QuranRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SimilarUiState())
    val state: StateFlow<SimilarUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            similarity.observeMemorizedAyahIds().collect { ids -> _state.update { it.copy(memorized = ids) } }
        }
        viewModelScope.launch {
            similarity.observeConfusions().collect { list -> _state.update { it.copy(confusions = list) } }
        }
        viewModelScope.launch {
            if (!similarity.ensureIndexed()) {
                _state.update { it.copy(loading = false, ready = false) }
                return@launch
            }
            val groups = similarity.loadGroups()
            val ayahs = quran.allAyahs().associateBy { it.id }
            val names = quran.observeSurahs().first().associate { it.id to it.nameAr }
            val groupIds = HashMap<Int, Int>()
            groups.forEach { g -> g.ayahIds.forEach { groupIds[it] = g.id } }
            _state.update {
                it.copy(loading = false, ready = true, groups = groups, groupIdByAyah = groupIds, ayahs = ayahs, surahNames = names)
            }
        }
    }

    fun toggleOnlyMemorized() = _state.update { it.copy(onlyMemorized = !it.onlyMemorized) }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = appContainer()
                SimilarViewModel(c.similarityRepository, c.quranRepository)
            }
        }
    }
}
