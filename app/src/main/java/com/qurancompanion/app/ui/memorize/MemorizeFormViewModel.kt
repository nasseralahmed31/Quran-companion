package com.qurancompanion.app.ui.memorize

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.qurancompanion.app.domain.model.Achievement
import com.qurancompanion.app.domain.model.MasteryLevel
import com.qurancompanion.app.domain.model.MemorizationEntry
import com.qurancompanion.app.domain.model.MemorizationState
import com.qurancompanion.app.domain.model.QuranConstants
import com.qurancompanion.app.domain.model.SaveResult
import com.qurancompanion.app.domain.model.Surah
import com.qurancompanion.app.domain.repository.MemorizationRepository
import com.qurancompanion.app.domain.repository.QuranRepository
import com.qurancompanion.app.domain.util.EpochDay
import com.qurancompanion.app.ui.common.appContainer
import com.qurancompanion.app.ui.common.toArabicIndic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** SURAH: surah + ayah numbers. PAGES: "I memorized pages 1 to 17". RANGE: a day's portion from a goal plan. */
enum class FormMode { SURAH, PAGES, RANGE }

data class MemorizeFormState(
    val loaded: Boolean = false,
    val notFound: Boolean = false,
    val isEdit: Boolean = false,
    val mode: FormMode = FormMode.SURAH,
    val surahs: List<Surah> = emptyList(),
    val surahId: Int = 0,
    val from: String = "",
    val to: String = "",
    val pageFrom: String = "",
    val pageTo: String = "",
    val rangeStartId: Int = 0,
    val rangeEndId: Int = 0,
    val rangeSummary: String? = null,
    val epochDay: Long = 0,
    val state: MemorizationState = MemorizationState.MEMORIZED,
    val mastery: MasteryLevel = MasteryLevel.GOOD,
    val note: String = "",
    val overlapCount: Int = 0,
    val error: String? = null,
    val saving: Boolean = false,
    val finished: Boolean = false,
    val newAchievements: List<Achievement> = emptyList(),
) {
    val selectedSurah: Surah? get() = surahs.firstOrNull { it.id == surahId }
}

class MemorizeFormViewModel(
    private val entryId: Long,
    private val presetStart: Int,
    private val presetEnd: Int,
    private val quran: QuranRepository,
    private val memorization: MemorizationRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(MemorizeFormState(epochDay = EpochDay.today()))
    val state: StateFlow<MemorizeFormState> = _state.asStateFlow()
    private var overlapJob: Job? = null
    private var summaryJob: Job? = null

    init {
        viewModelScope.launch {
            quran.observeSurahs().collect { list ->
                _state.update { it.copy(surahs = list) }
                refreshSummary()
                refreshOverlap()
            }
        }
        viewModelScope.launch { loadInitial() }
    }

    private suspend fun loadInitial() {
        if (entryId == 0L) {
            if (presetStart > 0 && presetEnd >= presetStart) {
                val first = quran.getAyah(presetStart)
                val last = quran.getAyah(presetEnd)
                if (first != null && last != null) {
                    if (first.surahId == last.surahId) {
                        _state.update {
                            it.copy(
                                loaded = true, surahId = first.surahId,
                                from = first.ayahNumber.toString(), to = last.ayahNumber.toString(),
                            )
                        }
                    } else {
                        _state.update {
                            it.copy(loaded = true, mode = FormMode.RANGE, rangeStartId = presetStart, rangeEndId = presetEnd)
                        }
                    }
                    refreshSummary()
                    refreshOverlap()
                    return
                }
            }
            _state.update { it.copy(loaded = true) }
            return
        }
        val entry = memorization.getEntry(entryId)
        if (entry == null) {
            _state.update { it.copy(loaded = true, notFound = true) }
        } else {
            _state.update {
                it.copy(
                    loaded = true,
                    isEdit = true,
                    surahId = entry.surahId,
                    from = entry.fromAyah.toString(),
                    to = entry.toAyah.toString(),
                    epochDay = entry.memorizedOn,
                    state = entry.state,
                    mastery = if (entry.state == MemorizationState.MEMORIZED) entry.mastery else MasteryLevel.GOOD,
                    note = entry.note,
                )
            }
        }
    }

    fun setMode(mode: FormMode) {
        _state.update { it.copy(mode = mode, error = null) }
        refreshSummary()
    }

    fun onSurah(id: Int) {
        _state.update { s ->
            val surah = s.surahs.firstOrNull { it.id == id }
            // Picking a surah with an empty range pre-fills the whole surah (easy to narrow down).
            val fill = surah != null && s.from.isBlank() && s.to.isBlank()
            s.copy(
                surahId = id,
                from = if (fill) "1" else s.from,
                to = if (fill && surah != null) surah.ayahCount.toString() else s.to,
                error = null,
            )
        }
        refreshOverlap()
    }

    fun onFrom(value: String) {
        _state.update { it.copy(from = value, error = null) }
        refreshOverlap()
    }

    fun onTo(value: String) {
        _state.update { it.copy(to = value, error = null) }
        refreshOverlap()
    }

    fun onPageFrom(value: String) {
        _state.update { it.copy(pageFrom = value, error = null) }
        refreshSummary()
    }

    fun onPageTo(value: String) {
        _state.update { it.copy(pageTo = value, error = null) }
        refreshSummary()
    }

    fun shiftDay(delta: Int) {
        _state.update { it.copy(epochDay = (it.epochDay + delta).coerceAtMost(EpochDay.today())) }
    }

    fun onState(value: MemorizationState) = _state.update { it.copy(state = value, error = null) }
    fun onMastery(value: MasteryLevel) = _state.update { it.copy(mastery = value) }
    fun onNote(value: String) = _state.update { it.copy(note = value.take(500)) }

    private fun refreshSummary() {
        summaryJob?.cancel()
        summaryJob = viewModelScope.launch {
            val s = _state.value
            when (s.mode) {
                FormMode.PAGES -> {
                    val from = s.pageFrom.toIntOrNull()
                    val to = s.pageTo.toIntOrNull()
                    val valid = from != null && to != null && from in 1..QuranConstants.TOTAL_PAGES && to in from..QuranConstants.TOTAL_PAGES
                    val ids = if (valid && from != null && to != null) quran.ayahIdRangeOfPages(from, to) else null
                    if (ids == null) {
                        _state.update { it.copy(rangeStartId = 0, rangeEndId = 0, rangeSummary = null) }
                    } else {
                        _state.update {
                            it.copy(
                                rangeStartId = ids.first,
                                rangeEndId = ids.second,
                                rangeSummary = describe(ids.first, ids.second, s.surahs),
                            )
                        }
                    }
                }
                FormMode.RANGE -> _state.update { it.copy(rangeSummary = describe(it.rangeStartId, it.rangeEndId, it.surahs)) }
                FormMode.SURAH -> Unit
            }
        }
    }

    private suspend fun describe(startId: Int, endId: Int, surahs: List<Surah>): String? {
        val a = quran.getAyah(startId) ?: return null
        val b = quran.getAyah(endId) ?: return null
        fun name(id: Int) = surahs.firstOrNull { it.id == id }?.nameAr.orEmpty()
        return "من سورة ${name(a.surahId)} آية ${a.ayahNumber.toArabicIndic()} إلى سورة ${name(b.surahId)} " +
            "آية ${b.ayahNumber.toArabicIndic()} • ${(endId - startId + 1).toArabicIndic()} آية"
    }

    private fun refreshOverlap() {
        overlapJob?.cancel()
        overlapJob = viewModelScope.launch {
            val s = _state.value
            val surah = s.selectedSurah
            val from = s.from.toIntOrNull()
            val to = s.to.toIntOrNull()
            if (s.mode != FormMode.SURAH || s.isEdit || surah == null || from == null || to == null ||
                from < 1 || to < from || to > surah.ayahCount
            ) {
                _state.update { it.copy(overlapCount = 0) }
                return@launch
            }
            val count = memorization.countRecordedInRange(surah.id, from, to)
            _state.update { it.copy(overlapCount = count) }
        }
    }

    fun save() {
        val s = _state.value
        fun fail(message: String) = _state.update { it.copy(error = message) }

        val mastery = when {
            s.state == MemorizationState.IN_PROGRESS -> MasteryLevel.IN_PROGRESS
            s.mastery in MasteryLevel.selectableWhenMemorized -> s.mastery
            else -> MasteryLevel.GOOD
        }

        if (s.mode == FormMode.SURAH) {
            val surah = s.selectedSurah
            val from = s.from.toIntOrNull()
            val to = s.to.toIntOrNull()
            if (surah == null) return fail("اختر السورة")
            if (from == null || to == null) return fail("أدخل رقم الآية الأولى والأخيرة")
            if (from < 1 || to < from) return fail("ترتيب الآيات غير صحيح")
            if (to > surah.ayahCount) return fail("سورة ${surah.nameAr} فيها ${surah.ayahCount} آية فقط")
            val entry = MemorizationEntry(
                id = if (s.isEdit) entryId else 0L,
                surahId = surah.id,
                fromAyah = from,
                toAyah = to,
                memorizedOn = s.epochDay,
                state = s.state,
                mastery = mastery,
                note = s.note,
            )
            persist { memorization.save(entry) }
        } else {
            if (s.rangeStartId <= 0 || s.rangeEndId < s.rangeStartId) {
                return fail("أدخل رقمي الصفحتين بين ١ و ٦٠٤ (من ≤ إلى)")
            }
            persist {
                memorization.saveRange(s.rangeStartId, s.rangeEndId, s.epochDay, s.state, mastery, s.note)
            }
        }
    }

    private fun persist(block: suspend () -> SaveResult) {
        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }
            try {
                val result = block()
                _state.update { it.copy(saving = false, finished = true, newAchievements = result.newAchievements) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(saving = false, error = "تعذّر الحفظ: ${e.message}") }
            }
        }
    }

    fun delete() {
        if (!_state.value.isEdit) return
        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }
            try {
                memorization.delete(entryId)
                _state.update { it.copy(saving = false, finished = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(saving = false, error = "تعذّر الحذف: ${e.message}") }
            }
        }
    }

    companion object {
        fun factory(entryId: Long, presetStart: Int, presetEnd: Int) = viewModelFactory {
            initializer {
                val c = appContainer()
                MemorizeFormViewModel(entryId, presetStart, presetEnd, c.quranRepository, c.memorizationRepository)
            }
        }
    }
}
