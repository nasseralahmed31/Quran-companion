package com.qurancompanion.app.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.qurancompanion.app.domain.model.Goal
import com.qurancompanion.app.domain.model.GoalKind
import com.qurancompanion.app.domain.model.GoalUnit
import com.qurancompanion.app.domain.model.Juz
import com.qurancompanion.app.domain.model.QuranConstants
import com.qurancompanion.app.domain.model.Surah
import com.qurancompanion.app.domain.repository.GoalRepository
import com.qurancompanion.app.domain.repository.QuranRepository
import com.qurancompanion.app.domain.util.EpochDay
import com.qurancompanion.app.domain.util.GoalPlanner
import com.qurancompanion.app.ui.common.appContainer
import com.qurancompanion.app.ui.common.oneDecimal
import com.qurancompanion.app.ui.common.toArabicIndic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class GoalPreset(val label: String) {
    SURAH("سورة"),
    JUZ("جزء"),
    PAGES("صفحات"),
    KHATM("ختم القرآن"),
    DAILY("حفظ يومي"),
}

data class GoalFormState(
    val surahs: List<Surah> = emptyList(),
    val juz: List<Juz> = emptyList(),
    val preset: GoalPreset = GoalPreset.SURAH,
    val name: String = "",
    val surahId: Int = 0,
    val juzNumber: String = "",
    val pageFrom: String = "",
    val pageTo: String = "",
    val days: String = "30",
    val dailyAmount: String = "",
    val unit: GoalUnit = GoalUnit.AYAHS,
    val preview: String? = null,
    val error: String? = null,
    val saving: Boolean = false,
    val saved: Boolean = false,
) {
    val selectedSurah: Surah? get() = surahs.firstOrNull { it.id == surahId }
}

private data class ResolvedRange(val startId: Int, val endId: Int, val defaultName: String)

class GoalFormViewModel(
    private val goals: GoalRepository,
    private val quran: QuranRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(GoalFormState())
    val state: StateFlow<GoalFormState> = _state.asStateFlow()
    private var previewJob: Job? = null

    init {
        viewModelScope.launch {
            combine(quran.observeSurahs(), quran.observeJuz()) { s, j -> s to j }.collect { (s, j) ->
                _state.update { it.copy(surahs = s, juz = j) }
                refreshPreview()
            }
        }
    }

    fun setPreset(p: GoalPreset) {
        _state.update { it.copy(preset = p, error = null, days = if (p == GoalPreset.KHATM) "365" else if (it.preset == GoalPreset.KHATM) "30" else it.days) }
        refreshPreview()
    }

    fun setName(v: String) = _state.update { it.copy(name = v.take(60)) }
    fun setSurah(id: Int) { _state.update { it.copy(surahId = id, error = null) }; refreshPreview() }
    fun setJuz(v: String) { _state.update { it.copy(juzNumber = v, error = null) }; refreshPreview() }
    fun setPageFrom(v: String) { _state.update { it.copy(pageFrom = v, error = null) }; refreshPreview() }
    fun setPageTo(v: String) { _state.update { it.copy(pageTo = v, error = null) }; refreshPreview() }
    fun setDays(v: String) { _state.update { it.copy(days = v, error = null) }; refreshPreview() }
    fun setDailyAmount(v: String) = _state.update { it.copy(dailyAmount = v, error = null) }
    fun setUnit(u: GoalUnit) = _state.update { it.copy(unit = u) }

    private suspend fun resolve(s: GoalFormState): ResolvedRange? = when (s.preset) {
        GoalPreset.SURAH -> s.selectedSurah?.let {
            ResolvedRange(it.firstAyahId, it.firstAyahId + it.ayahCount - 1, "حفظ سورة ${it.nameAr}")
        }
        GoalPreset.JUZ -> {
            val n = s.juzNumber.toIntOrNull()
            val juz = s.juz.firstOrNull { it.id == n }
            if (n == null || juz == null) {
                null
            } else {
                val end = s.juz.firstOrNull { it.id == n + 1 }?.firstAyahId?.minus(1) ?: QuranConstants.TOTAL_AYAHS
                ResolvedRange(juz.firstAyahId, end, "حفظ الجزء ${n.toArabicIndic()}")
            }
        }
        GoalPreset.PAGES -> {
            val from = s.pageFrom.toIntOrNull()
            val to = s.pageTo.toIntOrNull()
            val ids = if (from != null && to != null && from in 1..QuranConstants.TOTAL_PAGES && to in from..QuranConstants.TOTAL_PAGES) {
                quran.ayahIdRangeOfPages(from, to)
            } else {
                null
            }
            if (ids == null || from == null || to == null) {
                null
            } else {
                ResolvedRange(ids.first, ids.second, "حفظ الصفحات ${from.toArabicIndic()}–${to.toArabicIndic()}")
            }
        }
        GoalPreset.KHATM -> ResolvedRange(1, QuranConstants.TOTAL_AYAHS, "ختم حفظ القرآن")
        GoalPreset.DAILY -> null
    }

    private fun refreshPreview() {
        previewJob?.cancel()
        previewJob = viewModelScope.launch {
            val s = _state.value
            val days = s.days.toIntOrNull()
            val range = if (s.preset == GoalPreset.DAILY) null else resolve(s)
            if (range == null || days == null || days <= 0) {
                _state.update { it.copy(preview = null) }
                return@launch
            }
            val total = range.endId - range.startId + 1
            val perDay = GoalPlanner.requiredPerDay(total, days)
            val firstPage = quran.getAyah(range.startId)?.page ?: 0
            val lastPage = quran.getAyah(range.endId)?.page ?: 0
            val pages = (lastPage - firstPage + 1).coerceAtLeast(1)
            val pagesPerDay = pages / days.toDouble()
            _state.update {
                it.copy(
                    preview = "${total.toArabicIndic()} آية في ${pages.toArabicIndic()} صفحة • المقدار اليومي ≈ " +
                        "${perDay.toArabicIndic()} آية (≈ ${pagesPerDay.oneDecimal()} صفحة)",
                )
            }
        }
    }

    fun save() {
        val s = _state.value
        fun fail(message: String) = _state.update { it.copy(error = message) }
        val today = EpochDay.today()

        viewModelScope.launch {
            val goal: Goal
            if (s.preset == GoalPreset.DAILY) {
                val amount = s.dailyAmount.toIntOrNull()
                val max = if (s.unit == GoalUnit.PAGES) QuranConstants.TOTAL_PAGES else QuranConstants.TOTAL_AYAHS
                if (amount == null || amount !in 1..max) return@launch fail("أدخل مقدارًا يوميًا بين ١ و ${max.toArabicIndic()}")
                goal = Goal(
                    name = s.name.ifBlank { "حفظ ${amount.toArabicIndic()} ${s.unit.label} يوميًا" },
                    kind = GoalKind.DAILY,
                    startAyahId = 0, endAyahId = 0,
                    startDay = today, endDay = today,
                    dailyAmount = amount, unit = s.unit,
                )
            } else {
                val range = resolve(s) ?: return@launch fail("أكمل بيانات الهدف (السورة أو الجزء أو الصفحات)")
                val days = s.days.toIntOrNull()
                if (days == null || days !in 1..1000) return@launch fail("المدة بين ١ و ١٠٠٠ يوم")
                goal = Goal(
                    name = s.name.ifBlank { "${range.defaultName} خلال ${days.toArabicIndic()} يومًا" },
                    kind = GoalKind.RANGE,
                    startAyahId = range.startId, endAyahId = range.endId,
                    startDay = today, endDay = today + days - 1,
                    dailyAmount = 0, unit = GoalUnit.AYAHS,
                )
            }
            _state.update { it.copy(saving = true, error = null) }
            try {
                goals.create(goal)
                _state.update { it.copy(saving = false, saved = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(saving = false, error = "تعذّر إنشاء الهدف: ${e.message}") }
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = appContainer()
                GoalFormViewModel(c.goalRepository, c.quranRepository)
            }
        }
    }
}
