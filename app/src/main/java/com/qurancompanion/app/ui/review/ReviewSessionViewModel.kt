package com.qurancompanion.app.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.qurancompanion.app.domain.model.Ayah
import com.qurancompanion.app.domain.model.ReviewItem
import com.qurancompanion.app.domain.model.ReviewOutcome
import com.qurancompanion.app.domain.model.ReviewRating
import com.qurancompanion.app.domain.repository.QuranRepository
import com.qurancompanion.app.domain.repository.ReviewRepository
import com.qurancompanion.app.domain.util.EpochDay
import com.qurancompanion.app.ui.common.appContainer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReviewSessionState(
    val loading: Boolean = true,
    val missing: Boolean = false,
    val item: ReviewItem? = null,
    val surahName: String = "",
    val ayahs: List<Ayah> = emptyList(),
    val revealed: Boolean = false,
    val rating: ReviewRating? = null,
    val errors: Int = 0,
    val note: String = "",
    val manualDays: String = "",
    val saving: Boolean = false,
    val error: String? = null,
    val outcome: ReviewOutcome? = null,
)

class ReviewSessionViewModel(
    private val entryId: Long,
    private val review: ReviewRepository,
    private val quran: QuranRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ReviewSessionState())
    val state: StateFlow<ReviewSessionState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val item = review.getItem(entryId)
            if (item == null) {
                _state.update { it.copy(loading = false, missing = true) }
                return@launch
            }
            val ayahs = quran.ayahsOfRange(item.surahId, item.fromAyah, item.toAyah)
            val name = quran.observeSurahs().first().firstOrNull { it.id == item.surahId }?.nameAr.orEmpty()
            _state.update { it.copy(loading = false, item = item, surahName = name, ayahs = ayahs) }
        }
    }

    fun reveal() = _state.update { it.copy(revealed = !it.revealed) }
    fun setRating(r: ReviewRating) = _state.update { it.copy(rating = r, error = null) }
    fun changeErrors(delta: Int) = _state.update { it.copy(errors = (it.errors + delta).coerceIn(0, 99)) }
    fun setNote(n: String) = _state.update { it.copy(note = n.take(500)) }
    fun setManualDays(d: String) = _state.update { it.copy(manualDays = d) }

    fun save() {
        val s = _state.value
        val rating = s.rating
        if (rating == null) {
            _state.update { it.copy(error = "اختر تقييم أدائك أولًا") }
            return
        }
        val manualDay = s.manualDays.toIntOrNull()?.let { EpochDay.today() + it }
        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }
            try {
                val outcome = review.applyReview(entryId, rating, s.errors, s.note, manualDay)
                _state.update { it.copy(saving = false, outcome = outcome) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(saving = false, error = "تعذّر حفظ المراجعة: ${e.message}") }
            }
        }
    }

    companion object {
        fun factory(entryId: Long) = viewModelFactory {
            initializer {
                val c = appContainer()
                ReviewSessionViewModel(entryId, c.reviewRepository, c.quranRepository)
            }
        }
    }
}
