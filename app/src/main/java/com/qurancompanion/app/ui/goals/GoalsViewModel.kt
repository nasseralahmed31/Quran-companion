package com.qurancompanion.app.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.qurancompanion.app.domain.model.Ayah
import com.qurancompanion.app.domain.model.Goal
import com.qurancompanion.app.domain.model.GoalKind
import com.qurancompanion.app.domain.model.GoalProgress
import com.qurancompanion.app.domain.repository.GoalRepository
import com.qurancompanion.app.domain.repository.QuranRepository
import com.qurancompanion.app.domain.util.GoalPlanner
import com.qurancompanion.app.ui.common.appContainer
import com.qurancompanion.app.ui.common.toArabicIndic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlanRow(
    val dayIndex: Int,
    val day: Long,
    val fromId: Int,
    val toId: Int,
    val fromLabel: String,
    val toLabel: String,
    val count: Int,
)

class GoalsViewModel(
    private val goals: GoalRepository,
    private val quran: QuranRepository,
) : ViewModel() {

    val progress: StateFlow<List<GoalProgress>> = goals.observeProgress()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _plans = MutableStateFlow<Map<Long, List<PlanRow>>>(emptyMap())
    val plans: StateFlow<Map<Long, List<PlanRow>>> = _plans.asStateFlow()

    fun delete(id: Long) {
        viewModelScope.launch {
            goals.delete(id)
            _plans.update { it - id }
        }
    }

    /** Builds the day-by-day distribution of a range goal (once). */
    fun loadPlan(goal: Goal) {
        if (goal.kind != GoalKind.RANGE || _plans.value.containsKey(goal.id)) return
        viewModelScope.launch {
            val names = quran.observeSurahs().first().associate { it.id to it.nameAr }
            fun label(a: Ayah?) = if (a == null) "" else "${names[a.surahId].orEmpty()} ${a.ayahNumber.toArabicIndic()}"
            val days = (goal.endDay - goal.startDay + 1).toInt()
            val rows = GoalPlanner.schedule(goal.startAyahId, goal.endAyahId, days).map { p ->
                PlanRow(
                    dayIndex = p.dayIndex,
                    day = goal.startDay + p.dayIndex - 1,
                    fromId = p.fromAyahId,
                    toId = p.toAyahId,
                    fromLabel = label(quran.getAyah(p.fromAyahId)),
                    toLabel = label(quran.getAyah(p.toAyahId)),
                    count = p.ayahCount,
                )
            }
            _plans.update { it + (goal.id to rows) }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = appContainer()
                GoalsViewModel(c.goalRepository, c.quranRepository)
            }
        }
    }
}
