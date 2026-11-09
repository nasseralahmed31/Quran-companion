package com.qurancompanion.app.ui.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.qurancompanion.app.domain.model.QuestionResult
import com.qurancompanion.app.domain.model.QuizBuildResult
import com.qurancompanion.app.domain.model.QuizFinish
import com.qurancompanion.app.domain.model.QuizQuestion
import com.qurancompanion.app.domain.model.QuizType
import com.qurancompanion.app.domain.model.QuranConstants
import com.qurancompanion.app.domain.model.ReviewRating
import com.qurancompanion.app.domain.repository.QuizRepository
import com.qurancompanion.app.domain.repository.QuranRepository
import com.qurancompanion.app.ui.common.appContainer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class QuizPhase { SETUP, LOADING, ACTIVE, RESULT }

data class QuizUiState(
    val phase: QuizPhase = QuizPhase.SETUP,
    val type: QuizType = QuizType.RECITE,
    val fromSurah: String = "1",
    val toSurah: String = "114",
    val count: Int = 10,
    val error: String? = null,
    val questions: List<QuizQuestion> = emptyList(),
    val index: Int = 0,
    val revealed: Boolean = false,
    val chosen: Int = -1,
    val results: List<QuestionResult> = emptyList(),
    val rating: ReviewRating = ReviewRating.GOOD,
    val finishing: Boolean = false,
    val finish: QuizFinish? = null,
) {
    val current: QuizQuestion? get() = questions.getOrNull(index)
    val correctCount: Int get() = results.count { it.correct }
}

class QuizViewModel(
    initialType: QuizType?,
    private val quiz: QuizRepository,
    quran: QuranRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(QuizUiState(type = initialType ?: QuizType.RECITE))
    val state: StateFlow<QuizUiState> = _state.asStateFlow()

    val surahNames: StateFlow<Map<Int, String>> = quran.observeSurahs()
        .map { list -> list.associate { it.id to it.nameAr } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun setType(t: QuizType) = _state.update { it.copy(type = t, error = null) }
    fun setFrom(v: String) = _state.update { it.copy(fromSurah = v, error = null) }
    fun setTo(v: String) = _state.update { it.copy(toSurah = v, error = null) }
    fun setCount(c: Int) = _state.update { it.copy(count = c) }
    fun setRating(r: ReviewRating) = _state.update { it.copy(rating = r) }

    fun start() {
        val s = _state.value
        val comprehensive = s.type == QuizType.COMPREHENSIVE
        val from = if (comprehensive) 1 else s.fromSurah.toIntOrNull()
        val to = if (comprehensive) QuranConstants.TOTAL_SURAHS else s.toSurah.toIntOrNull()
        val max = QuranConstants.TOTAL_SURAHS
        if (from == null || to == null || from !in 1..max || to !in 1..max || from > to) {
            _state.update { it.copy(error = "حدّد نطاق سور صحيحًا بين ١ و ١١٤ (من ≤ إلى)") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(phase = QuizPhase.LOADING, error = null) }
            when (val built = quiz.buildQuiz(s.type, from, to, s.count)) {
                is QuizBuildResult.Success -> _state.update {
                    it.copy(
                        phase = QuizPhase.ACTIVE, questions = built.questions, index = 0,
                        revealed = false, chosen = -1, results = emptyList(), finish = null,
                    )
                }
                is QuizBuildResult.Failure -> _state.update { it.copy(phase = QuizPhase.SETUP, error = built.message) }
            }
        }
    }

    fun reveal() = _state.update { it.copy(revealed = true) }

    /** Self-graded questions: the user says whether they got it right. */
    fun grade(correct: Boolean) {
        val q = _state.value.current ?: return
        record(QuestionResult(q.ayahId, correct))
        advance()
    }

    /** Multiple-choice questions: auto-graded; wrong picks between similar ayahs are remembered. */
    fun choose(index: Int) {
        val s = _state.value
        val q = s.current ?: return
        if (s.chosen != -1) return
        val correct = index == q.correctIndex
        _state.update { it.copy(chosen = index) }
        record(QuestionResult(q.ayahId, correct))
        if (!correct && q.type == QuizType.SIMILAR) {
            val wrongAyah = q.optionAyahIds.getOrNull(index)
            if (wrongAyah != null) viewModelScope.launch { quiz.recordConfusion(q.ayahId, wrongAyah) }
        }
    }

    fun next() = advance()

    private fun record(r: QuestionResult) = _state.update { it.copy(results = it.results + r) }

    private fun advance() {
        _state.update { s ->
            val nextIndex = s.index + 1
            if (nextIndex >= s.questions.size) {
                val pct = if (s.results.isEmpty()) 0 else s.correctCount * 100 / s.results.size
                val suggested = when {
                    pct >= 90 -> ReviewRating.EXCELLENT
                    pct >= 70 -> ReviewRating.GOOD
                    pct >= 50 -> ReviewRating.AVERAGE
                    else -> ReviewRating.WEAK
                }
                s.copy(phase = QuizPhase.RESULT, rating = suggested, revealed = false, chosen = -1)
            } else {
                s.copy(index = nextIndex, revealed = false, chosen = -1)
            }
        }
    }

    /** Applies the (editable) rating to the review schedule and mastery of the passages quizzed. */
    fun apply() {
        val s = _state.value
        if (s.finishing || s.finish != null) return
        viewModelScope.launch {
            _state.update { it.copy(finishing = true, error = null) }
            try {
                val result = quiz.finishQuiz(s.type, s.results, s.rating)
                _state.update { it.copy(finishing = false, finish = result) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(finishing = false, error = "تعذّر تطبيق النتيجة: ${e.message}") }
            }
        }
    }

    fun restart() = _state.update { QuizUiState(type = it.type, fromSurah = it.fromSurah, toSurah = it.toSurah, count = it.count) }

    companion object {
        fun factory(initialType: QuizType?) = viewModelFactory {
            initializer {
                val c = appContainer()
                QuizViewModel(initialType, c.quizRepository, c.quranRepository)
            }
        }
    }
}
