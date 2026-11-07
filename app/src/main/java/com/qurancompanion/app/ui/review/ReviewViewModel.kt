package com.qurancompanion.app.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.qurancompanion.app.domain.model.ReviewItem
import com.qurancompanion.app.domain.repository.QuranRepository
import com.qurancompanion.app.domain.repository.ReviewRepository
import com.qurancompanion.app.domain.util.EpochDay
import com.qurancompanion.app.ui.common.appContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReviewViewModel(
    private val review: ReviewRepository,
    quran: QuranRepository,
) : ViewModel() {

    val items: StateFlow<List<ReviewItem>> = review.observeItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val surahNames: StateFlow<Map<Int, String>> = quran.observeSurahs()
        .map { list -> list.associate { it.id to it.nameAr } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** Manually moves the next review to [daysFromToday] days from now. */
    fun reschedule(entryId: Long, daysFromToday: Int) {
        viewModelScope.launch { review.reschedule(entryId, EpochDay.today() + daysFromToday) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = appContainer()
                ReviewViewModel(c.reviewRepository, c.quranRepository)
            }
        }
    }
}
