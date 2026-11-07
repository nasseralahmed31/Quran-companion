package com.qurancompanion.app.data.repository

import com.qurancompanion.app.data.local.GoalEntity
import com.qurancompanion.app.data.local.MemorizationDao
import com.qurancompanion.app.data.local.PlanningDao
import com.qurancompanion.app.domain.model.Goal
import com.qurancompanion.app.domain.model.GoalKind
import com.qurancompanion.app.domain.model.GoalProgress
import com.qurancompanion.app.domain.model.GoalUnit
import com.qurancompanion.app.domain.repository.GoalRepository
import com.qurancompanion.app.domain.util.DayClock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class GoalRepositoryImpl(
    private val planning: PlanningDao,
    private val memorization: MemorizationDao,
) : GoalRepository {

    override fun observeProgress(): Flow<List<GoalProgress>> =
        combine(planning.observeGoals(), DayClock.ticker()) { goals, day -> goals to day }
            .flatMapLatest { (goals, day) ->
                if (goals.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    combine(goals.map { progressFlow(it.toDomain(), day) }) { it.toList() }
                }
            }

    override suspend fun create(goal: Goal): Long = planning.insertGoal(goal.toEntity())

    override suspend fun delete(id: Long) = planning.deleteGoal(id)

    private fun progressFlow(goal: Goal, day: Long): Flow<GoalProgress> = when {
        goal.kind == GoalKind.RANGE ->
            planning.observeMemorizedInRange(goal.startAyahId, goal.endAyahId).map {
                GoalProgress(goal, it, goal.endAyahId - goal.startAyahId + 1, 0.0, day)
            }
        goal.unit == GoalUnit.AYAHS ->
            memorization.observeMemorizedOnDay(day).map { GoalProgress(goal, it, 0, it.toDouble(), day) }
        else ->
            planning.observePagesOnDay(day).map { GoalProgress(goal, 0, 0, it, day) }
    }
}

private fun GoalEntity.toDomain() = Goal(
    id = id,
    name = name,
    kind = runCatching { GoalKind.valueOf(kind) }.getOrDefault(GoalKind.RANGE),
    startAyahId = startAyahId,
    endAyahId = endAyahId,
    startDay = startDay,
    endDay = endDay,
    dailyAmount = dailyAmount,
    unit = runCatching { GoalUnit.valueOf(unit) }.getOrDefault(GoalUnit.AYAHS),
)

private fun Goal.toEntity() = GoalEntity(
    id = id,
    name = name.trim(),
    kind = kind.name,
    startAyahId = startAyahId,
    endAyahId = endAyahId,
    startDay = startDay,
    endDay = endDay,
    dailyAmount = dailyAmount,
    unit = unit.name,
    createdAt = System.currentTimeMillis(),
)
