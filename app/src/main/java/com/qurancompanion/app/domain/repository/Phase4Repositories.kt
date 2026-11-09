package com.qurancompanion.app.domain.repository

import com.qurancompanion.app.domain.model.Confusion
import com.qurancompanion.app.domain.model.Goal
import com.qurancompanion.app.domain.model.GoalProgress
import com.qurancompanion.app.domain.model.QuestionResult
import com.qurancompanion.app.domain.model.QuizBuildResult
import com.qurancompanion.app.domain.model.QuizFinish
import com.qurancompanion.app.domain.model.QuizType
import com.qurancompanion.app.domain.model.ReviewItem
import com.qurancompanion.app.domain.model.ReviewOutcome
import com.qurancompanion.app.domain.model.ReviewRating
import com.qurancompanion.app.domain.model.RolloverSummary
import com.qurancompanion.app.domain.model.SimilarGroup
import com.qurancompanion.app.domain.model.SimilarPair
import com.qurancompanion.app.domain.model.StatsOverview
import com.qurancompanion.app.domain.model.SurahMapEntry
import kotlinx.coroutines.flow.Flow

interface ReviewRepository {
    /** All scheduled passages, sorted by priority (overdue first, weakest first). */
    fun observeItems(): Flow<List<ReviewItem>>
    suspend fun getItem(entryId: Long): ReviewItem?

    /**
     * Logs a review, reschedules the passage, updates its mastery and returns what happened.
     * [manualNextDay] overrides the computed next review day.
     */
    suspend fun applyReview(
        entryId: Long,
        rating: ReviewRating,
        errors: Int,
        note: String,
        manualNextDay: Long?,
        source: String = "REVIEW",
    ): ReviewOutcome

    suspend fun reschedule(entryId: Long, nextDay: Long)

    /**
     * Once per day: when review days were missed, re-dates the overdue passages from today (weakest
     * first, spread by the daily review target) and pushes the rest back, so nothing piles up as overdue.
     * Returns a summary when something moved.
     */
    suspend fun rollover(): RolloverSummary?
}

interface GoalRepository {
    fun observeProgress(): Flow<List<GoalProgress>>
    suspend fun create(goal: Goal): Long
    suspend fun delete(id: Long)
}

interface SimilarityRepository {
    /** Builds the similarity index from the Quran text if it does not exist yet. */
    suspend fun ensureIndexed(): Boolean
    suspend fun loadGroups(minScore: Double = 0.6): List<SimilarGroup>
    suspend fun loadPairs(minScore: Double = 0.6): List<SimilarPair>
    fun observeConfusions(): Flow<List<Confusion>>
    fun observeMemorizedAyahIds(): Flow<Set<Int>>
}

interface QuizRepository {
    suspend fun buildQuiz(type: QuizType, fromSurah: Int, toSurah: Int, count: Int): QuizBuildResult
    suspend fun recordConfusion(correctAyahId: Int, chosenAyahId: Int)
    suspend fun finishQuiz(type: QuizType, results: List<QuestionResult>, rating: ReviewRating): QuizFinish
}

interface StatsRepository {
    fun observeOverview(): Flow<StatsOverview>
    fun observeSurahMap(): Flow<List<SurahMapEntry>>
}
