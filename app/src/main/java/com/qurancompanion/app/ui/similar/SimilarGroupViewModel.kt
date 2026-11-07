package com.qurancompanion.app.ui.similar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.qurancompanion.app.data.similarity.WordDiff
import com.qurancompanion.app.domain.model.Ayah
import com.qurancompanion.app.domain.repository.QuranRepository
import com.qurancompanion.app.domain.repository.SimilarityRepository
import com.qurancompanion.app.ui.common.appContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GroupMember(val ayah: Ayah, val words: List<WordDiff.Word>)

data class SimilarGroupState(
    val loading: Boolean = true,
    val missing: Boolean = false,
    val members: List<GroupMember> = emptyList(),
    val surahNames: Map<Int, String> = emptyMap(),
    val memorized: Set<Int> = emptySet(),
)

class SimilarGroupViewModel(
    private val groupId: Int,
    private val similarity: SimilarityRepository,
    private val quran: QuranRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(SimilarGroupState())
    val state: StateFlow<SimilarGroupState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            similarity.observeMemorizedAyahIds().collect { ids -> _state.update { it.copy(memorized = ids) } }
        }
        viewModelScope.launch {
            val group = similarity.loadGroups().firstOrNull { it.id == groupId }
            if (group == null) {
                _state.update { it.copy(loading = false, missing = true) }
                return@launch
            }
            val ayahs = group.ayahIds.mapNotNull { quran.getAyah(it) }
            val diffs = WordDiff.highlight(ayahs.map { it.text })
            val names = quran.observeSurahs().first().associate { it.id to it.nameAr }
            _state.update {
                it.copy(
                    loading = false,
                    members = ayahs.mapIndexed { i, a -> GroupMember(a, diffs[i]) },
                    surahNames = names,
                )
            }
        }
    }

    companion object {
        fun factory(groupId: Int) = viewModelFactory {
            initializer {
                val c = appContainer()
                SimilarGroupViewModel(groupId, c.similarityRepository, c.quranRepository)
            }
        }
    }
}
