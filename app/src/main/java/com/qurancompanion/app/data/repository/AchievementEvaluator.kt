package com.qurancompanion.app.data.repository

import com.qurancompanion.app.data.local.AchievementEntity
import com.qurancompanion.app.data.local.MemorizationDao
import com.qurancompanion.app.data.local.ReviewDao
import com.qurancompanion.app.domain.model.Achievement
import com.qurancompanion.app.domain.model.AchievementMetric
import com.qurancompanion.app.domain.util.EpochDay
import com.qurancompanion.app.domain.util.StreakCalculator

/** Unlocks achievements whose target has been reached; unlocks are permanent. */
class AchievementEvaluator(
    private val memorization: MemorizationDao,
    private val review: ReviewDao,
) {
    suspend fun evaluate(now: Long): List<Achievement> {
        val activeDays = (memorization.entryDays() + review.reviewDays()).distinct()
        val values = mapOf(
            AchievementMetric.MEMORIZED_AYAHS to memorization.memorizedCount(),
            AchievementMetric.COMPLETED_SURAHS to memorization.completedSurahsCount(),
            AchievementMetric.MEMORIZED_JUZ to memorization.memorizedJuzCount(),
            AchievementMetric.STREAK_DAYS to StreakCalculator.current(activeDays, EpochDay.today()),
            AchievementMetric.REVIEWED_AYAHS to review.reviewedAyahs(),
        )
        val unlocked = memorization.unlockedKeys().toSet()
        val fresh = Achievement.entries.filter { it.key !in unlocked && (values[it.metric] ?: 0) >= it.target }
        fresh.forEach { memorization.insertAchievement(AchievementEntity(it.key, now)) }
        return fresh
    }
}
