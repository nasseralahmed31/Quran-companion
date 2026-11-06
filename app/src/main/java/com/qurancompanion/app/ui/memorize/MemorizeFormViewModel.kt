package com.qurancompanion.app.ui.memorize

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.qurancompanion.app.domain.model.Achievement
import com.qurancompanion.app.domain.model.MasteryLevel
import com.qurancompanion.app.domain.model.MemorizationEntry
import com.qurancompanion.app.domain.model.MemorizationState
import com.qurancompanion.app.domain.model.Surah
import com.qurancompanion.app.domain.repository.MemorizationRepository
import com.qurancompanion.app.domain.repository.QuranRepository
import com.qurancompanion.app.domain.util.EpochDay
import com.qurancompanion.app.ui.common.appContainer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MemorizeFormState(
    val loaded: Boolean = false,
    val notFound: Boolean = false,
    val isEdit: Boolean = false,
    val surahs: List<Surah> = emptyList(),
    val surahId: Int = 0,
    val from: String = "",
    val to: String = "",
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
    private val quran: QuranRepository,
    private val memorization: MemorizationRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(MemorizeFormState(epochDay = EpochDay.today()))
    val state: StateFlow<MemorizeFormState> = _state.asStateFlow()
    private var overlapJob: Job? = null

    init {
        viewModelScope.launch {
            quran.observeSurahs().collect { list -> _state.update { it.copy(surahs = list) } }
        }
        viewModelScope.launch {
            if (entryId == 0L) {
                _state.update { it.copy(loaded = true) }
                return@launch
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
    }

    fun onSurah(id: Int) {
        _state.update { s ->
            val surah = s.surahs.firstOrNull { it.id == id }
            // Picking a surah with empty range pre-fills the whole surah (easy to narrow down).
            val fill = surah != null && s.from.isBlank() && s.to.isBlank()
            s.copy(
                surahId = id,
                from = if (fill) "1" else s.from,
                to = if (fill) surah!!.ayahCount.toString() else s.to,
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

    fun shiftDay(delta: Int) {
        _state.update { it.copy(epochDay = (it.epochDay + delta).coerceAtMost(EpochDay.today())) }
    }

    fun onState(value: MemorizationState) = _state.update { it.copy(state = value, error = null) }
    fun onMastery(value: MasteryLevel) = _state.update { it.copy(mastery = value) }
    fun onNote(value: String) = _state.update { it.copy(note = value.take(500)) }

    private fun refreshOverlap() {
        overlapJob?.cancel()
        overlapJob = viewModelScope.launch {
            val s = _state.value
            val surah = s.selectedSurah
            val from = s.from.toIntOrNull()
            val to = s.to.toIntOrNull()
            if (s.isEdit || surah == null || from == null || to == null || from < 1 || to < from || to > surah.ayahCount) {
                _state.update { it.copy(overlapCount = 0) }
                return@launch
            }
            val count = memorization.countRecordedInRange(surah.id, from, to)
            _state.update { it.copy(overlapCount = count) }
        }
    }

    fun save() {
        val s = _state.value
        val surah = s.selectedSurah
        val from = s.from.toIntOrNull()
        val to = s.to.toIntOrNull()
        fun fail(message: String) = _state.update { it.copy(error = message) }

        if (surah == null) return fail("اختر السورة")
        if (from == null || to == null) return fail("أدخل رقم الآية الأولى والأخيرة")
        if (from < 1 || to < from) return fail("ترتيب الآيات غير صحيح")
        if (to > surah.ayahCount) return fail("سورة ${surah.nameAr} فيها ${surah.ayahCount} آية فقط")

        val mastery = when {
            s.state == MemorizationState.IN_PROGRESS -> MasteryLevel.IN_PROGRESS
            s.mastery in MasteryLevel.selectableWhenMemorized -> s.mastery
            else -> MasteryLevel.GOOD
        }
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
        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }
            try {
                val result = memorization.save(entry)
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
        fun factory(entryId: Long) = viewModelFactory {
            initializer {
                val c = appContainer()
                MemorizeFormViewModel(entryId, c.quranRepository, c.memorizationRepository)
            }
        }
    }
}
