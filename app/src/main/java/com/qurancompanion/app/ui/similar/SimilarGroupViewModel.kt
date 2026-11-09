package com.qurancompanion.app.ui.similar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.qurancompanion.app.data.similarity.ArabicText
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

/** [nextWord] is the word where this ayah leaves the opening that all the ayahs share. */
data class GroupMember(val ayah: Ayah, val words: List<WordDiff.Word>, val nextWord: String)

data class SimilarGroupState(
    val loading: Boolean = true,
    val missing: Boolean = false,
    val members: List<GroupMember> = emptyList(),
    val surahNames: Map<Int, String> = emptyMap(),
    val memorized: Set<Int> = emptySet(),
    val commonStart: String = "",
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
            val tokens = ayahs.map { ArabicText.tokenize(it.text) }
            // How many opening words do all the ayahs share, and where does each one go its own way?
            var prefix = tokens.firstOrNull()?.normalized?.size ?: 0
            for (t in tokens.drop(1)) {
                prefix = minOf(prefix, ArabicText.commonPrefixLength(tokens.first().normalized, t.normalized))
            }
            val commonStart = tokens.firstOrNull()?.original?.take(prefix)?.joinToString(" ").orEmpty()
            val names = quran.observeSurahs().first().associate { it.id to it.nameAr }
            _state.update {
                it.copy(
                    loading = false,
                    members = ayahs.mapIndexed { i, a -> GroupMember(a, diffs[i], tokens[i].original.getOrNull(prefix).orEmpty()) },
                    surahNames = names,
                    commonStart = commonStart,
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
