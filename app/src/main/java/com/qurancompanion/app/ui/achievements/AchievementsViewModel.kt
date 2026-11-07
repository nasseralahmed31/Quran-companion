package com.qurancompanion.app.ui.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.qurancompanion.app.domain.model.Achievement
import com.qurancompanion.app.domain.model.AchievementMetric
import com.qurancompanion.app.domain.model.AchievementProgress
import com.qurancompanion.app.domain.model.ProgressSummary
import com.qurancompanion.app.domain.repository.MemorizationRepository
import com.qurancompanion.app.domain.repository.ProgressRepository
import com.qurancompanion.app.ui.common.appContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class AchievementsViewModel(
    progress: ProgressRepository,
    memorization: MemorizationRepository,
) : ViewModel() {

    val achievements: StateFlow<List<AchievementProgress>> = combine(
        progress.observeSummary(),
        memorization.observeUnlocked(),
    ) { summary, unlocked ->
        Achievement.entries.map { a ->
            AchievementProgress(a, current = metricValue(a.metric, summary), unlockedAt = unlocked[a.key])
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun metricValue(metric: AchievementMetric, s: ProgressSummary): Int = when (metric) {
        AchievementMetric.MEMORIZED_AYAHS -> s.memorizedAyahs
        AchievementMetric.COMPLETED_SURAHS -> s.completedSurahs
        AchievementMetric.MEMORIZED_JUZ -> s.memorizedJuz
        AchievementMetric.STREAK_DAYS -> s.streakDays
        AchievementMetric.REVIEWED_AYAHS -> s.reviewedAyahs
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = appContainer()
                AchievementsViewModel(c.progressRepository, c.memorizationRepository)
            }
        }
    }
}
