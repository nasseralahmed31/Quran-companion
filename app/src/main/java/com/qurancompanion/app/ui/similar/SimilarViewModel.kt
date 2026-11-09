package com.qurancompanion.app.ui.similar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.qurancompanion.app.data.similarity.ArabicText
import com.qurancompanion.app.data.similarity.WordDiff
import com.qurancompanion.app.domain.model.Ayah
import com.qurancompanion.app.domain.model.AyahMark
import com.qurancompanion.app.domain.model.Confusion
import com.qurancompanion.app.domain.model.SimilarGroup
import com.qurancompanion.app.domain.model.SimilarPair
import com.qurancompanion.app.domain.repository.MemorizationRepository
import com.qurancompanion.app.domain.repository.QuranRepository
import com.qurancompanion.app.domain.repository.SimilarityRepository
import com.qurancompanion.app.ui.common.appContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Two similar ayahs that are both memorized, with exactly the words that tell them apart. */
data class RiskPair(
    val first: Ayah,
    val second: Ayah,
    val score: Double,
    val onlyInFirst: List<String>,
    val onlyInSecond: List<String>,
    val weak: Boolean,
)

enum class SimilarSort(val label: String) {
    RISK("الأخطر أولًا"),
    SIZE("الأكبر"),
    SOURCE("ترتيب المصحف"),
}

data class SimilarUiState(
    val loading: Boolean = true,
    val ready: Boolean = false,
    val groups: List<SimilarGroup> = emptyList(),
    val groupIdByAyah: Map<Int, Int> = emptyMap(),
    val ayahs: Map<Int, Ayah> = emptyMap(),
    val surahNames: Map<Int, String> = emptyMap(),
    val memorized: Set<Int> = emptySet(),
    val marks: Map<Int, AyahMark> = emptyMap(),
    val confusions: List<Confusion> = emptyList(),
    val atRisk: List<RiskPair> = emptyList(),
    val onlyMemorized: Boolean = true,
    val query: String = "",
    val sort: SimilarSort = SimilarSort.RISK,
)

class SimilarViewModel(
    private val similarity: SimilarityRepository,
    private val quran: QuranRepository,
    private val memorization: MemorizationRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SimilarUiState())
    val state: StateFlow<SimilarUiState> = _state.asStateFlow()

    private var pairs: List<SimilarPair> = emptyList()
    private var searchKey: Map<Int, String> = emptyMap()
    private var riskJob: Job? = null

    init {
        viewModelScope.launch {
            similarity.observeMemorizedAyahIds().collect { ids ->
                _state.update { it.copy(memorized = ids) }
                recomputeRisk()
            }
        }
        viewModelScope.launch {
            similarity.observeConfusions().collect { list -> _state.update { it.copy(confusions = list) } }
        }
        viewModelScope.launch {
            memorization.observeAllMarks().collect { marks ->
                _state.update { it.copy(marks = marks) }
                recomputeRisk()
            }
        }
        viewModelScope.launch {
            if (!similarity.ensureIndexed()) {
                _state.update { it.copy(loading = false, ready = false) }
                return@launch
            }
            val groups = similarity.loadGroups()
            pairs = similarity.loadPairs()
            val ayahs = quran.allAyahs().associateBy { it.id }
            val names = quran.observeSurahs().first().associate { it.id to it.nameAr }
            val groupIds = HashMap<Int, Int>()
            groups.forEach { g -> g.ayahIds.forEach { groupIds[it] = g.id } }
            // Normalized surah names + ayah words per group, so searching ignores diacritics.
            searchKey = withContext(Dispatchers.Default) {
                groups.associate { g ->
                    g.id to g.ayahIds.joinToString(" ") { id ->
                        ayahs[id]?.let { a ->
                            ArabicText.tokenize(names[a.surahId].orEmpty()).normalized.joinToString(" ") + " " +
                                ArabicText.tokenize(a.text).normalized.joinToString(" ")
                        }.orEmpty()
                    }
                }
            }
            _state.update {
                it.copy(loading = false, ready = true, groups = groups, groupIdByAyah = groupIds, ayahs = ayahs, surahNames = names)
            }
            recomputeRisk()
        }
    }

    fun toggleOnlyMemorized() = _state.update { it.copy(onlyMemorized = !it.onlyMemorized) }
    fun setQuery(q: String) = _state.update { it.copy(query = q.take(60)) }
    fun setSort(sort: SimilarSort) = _state.update { it.copy(sort = sort) }

    /** Groups after the "mine only" filter, the search and the chosen order. */
    fun visibleGroups(s: SimilarUiState): List<SimilarGroup> {
        val q = ArabicText.normalizeWord(s.query.trim())
        var list = s.groups
        if (s.onlyMemorized) list = list.filter { g -> g.ayahIds.any { it in s.memorized } }
        if (q.isNotEmpty()) list = list.filter { g -> searchKey[g.id]?.contains(q) == true }
        return when (s.sort) {
            SimilarSort.RISK -> list.sortedWith(
                compareByDescending<SimilarGroup> { g -> g.ayahIds.any { s.marks[it] == AyahMark.NEEDS_REVIEW } }
                    .thenByDescending { g -> g.ayahIds.count { it in s.memorized } }
                    .thenBy { it.id },
            )
            SimilarSort.SIZE -> list.sortedWith(compareByDescending<SimilarGroup> { it.ayahIds.size }.thenBy { it.id })
            SimilarSort.SOURCE -> list.sortedBy { it.id }
        }
    }

    /** Pairs where the user has memorized both ayahs: the places a slip is most likely. */
    private fun recomputeRisk() {
        riskJob?.cancel()
        riskJob = viewModelScope.launch(Dispatchers.Default) {
            val s = _state.value
            if (!s.ready || pairs.isEmpty() || s.ayahs.isEmpty()) return@launch
            val risky = pairs.asSequence()
                .filter { it.a in s.memorized && it.b in s.memorized }
                .mapNotNull { p ->
                    val a = s.ayahs[p.a] ?: return@mapNotNull null
                    val b = s.ayahs[p.b] ?: return@mapNotNull null
                    val diff = WordDiff.pairDifferences(a.text, b.text)
                    // Identical wording leaves nothing to tell apart by words.
                    if (diff.onlyInFirst.isEmpty() && diff.onlyInSecond.isEmpty()) return@mapNotNull null
                    val weak = s.marks[p.a] == AyahMark.NEEDS_REVIEW || s.marks[p.b] == AyahMark.NEEDS_REVIEW
                    RiskPair(a, b, p.score, diff.onlyInFirst, diff.onlyInSecond, weak)
                }
                .sortedWith(compareByDescending<RiskPair> { it.weak }.thenByDescending { it.score })
                .take(30)
                .toList()
            _state.update { it.copy(atRisk = risky) }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = appContainer()
                SimilarViewModel(c.similarityRepository, c.quranRepository, c.memorizationRepository)
            }
        }
    }
}
