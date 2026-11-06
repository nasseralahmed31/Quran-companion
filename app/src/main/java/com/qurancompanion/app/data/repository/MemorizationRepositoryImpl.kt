package com.qurancompanion.app.data.repository

import androidx.room.withTransaction
import com.qurancompanion.app.data.local.AchievementEntity
import com.qurancompanion.app.data.local.ActivityEntity
import com.qurancompanion.app.data.local.AyahProgressEntity
import com.qurancompanion.app.data.local.MemorizationEntryEntity
import com.qurancompanion.app.data.local.QuranDatabase
import com.qurancompanion.app.domain.model.Achievement
import com.qurancompanion.app.domain.model.AchievementMetric
import com.qurancompanion.app.domain.model.ActivityItem
import com.qurancompanion.app.domain.model.ActivityType
import com.qurancompanion.app.domain.model.AyahMark
import com.qurancompanion.app.domain.model.MasteryLevel
import com.qurancompanion.app.domain.model.MemorizationEntry
import com.qurancompanion.app.domain.model.MemorizationState
import com.qurancompanion.app.domain.model.SaveResult
import com.qurancompanion.app.domain.repository.MemorizationRepository
import com.qurancompanion.app.domain.util.EpochDay
import com.qurancompanion.app.domain.util.StreakCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MemorizationRepositoryImpl(private val db: QuranDatabase) : MemorizationRepository {
    private val dao = db.memorizationDao()
    private val quranDao = db.quranDao()

    override fun observeEntries(): Flow<List<MemorizationEntry>> =
        dao.observeEntries().map { list -> list.map { it.toDomain() } }

    override suspend fun getEntry(id: Long): MemorizationEntry? = dao.getEntry(id)?.toDomain()

    override suspend fun countRecordedInRange(surahId: Int, from: Int, to: Int): Int =
        dao.recordedInRange(surahId, from, to)

    override suspend fun save(entry: MemorizationEntry): SaveResult = db.withTransaction {
        val now = System.currentTimeMillis()
        val type: ActivityType
        if (entry.id == 0L) {
            dao.insertEntry(entry.toEntity(createdAt = now, updatedAt = now))
            recomputeRange(entry.surahId, entry.fromAyah, entry.toAyah)
            type = ActivityType.ADDED
        } else {
            val old = dao.getEntry(entry.id) ?: throw IllegalStateException("سجل الحفظ غير موجود")
            dao.updateEntry(entry.toEntity(createdAt = old.createdAt, updatedAt = now))
            recomputeRange(old.surahId, old.fromAyah, old.toAyah)
            recomputeRange(entry.surahId, entry.fromAyah, entry.toAyah)
            type = ActivityType.EDITED
        }
        dao.insertActivity(
            ActivityEntity(
                type = type.name,
                day = EpochDay.today(),
                timestamp = now,
                surahId = entry.surahId,
                fromAyah = entry.fromAyah,
                toAyah = entry.toAyah,
                detail = entry.displayMastery.label,
            ),
        )
        SaveResult(evaluateAchievements(now))
    }

    override suspend fun delete(id: Long) {
        db.withTransaction {
            val old = dao.getEntry(id) ?: return@withTransaction
            dao.deleteEntry(id)
            recomputeRange(old.surahId, old.fromAyah, old.toAyah)
            dao.insertActivity(
                ActivityEntity(
                    type = ActivityType.DELETED.name,
                    day = EpochDay.today(),
                    timestamp = System.currentTimeMillis(),
                    surahId = old.surahId,
                    fromAyah = old.fromAyah,
                    toAyah = old.toAyah,
                    detail = "",
                ),
            )
        }
    }

    override fun observeActivity(): Flow<List<ActivityItem>> =
        dao.observeActivity().map { list ->
            list.map {
                ActivityItem(
                    id = it.id,
                    type = runCatching { ActivityType.valueOf(it.type) }.getOrDefault(ActivityType.EDITED),
                    day = it.day,
                    timestamp = it.timestamp,
                    surahId = it.surahId,
                    fromAyah = it.fromAyah,
                    toAyah = it.toAyah,
                    detail = it.detail,
                )
            }
        }

    override fun observeUnlocked(): Flow<Map<String, Long>> =
        dao.observeAchievements().map { list -> list.associate { it.achievementKey to it.unlockedAt } }

    override fun observePageMarks(page: Int): Flow<Map<Int, AyahMark>> =
        dao.observePageProgress(page).map { list -> list.associate { it.ayahId to markOf(it) } }

    /**
     * Rebuilds per-ayah progress for a range from the records covering it: the newest record
     * (by date, then id) wins; ayahs no longer covered by any record are cleared.
     */
    private suspend fun recomputeRange(surahId: Int, from: Int, to: Int) {
        val firstAyahId = quranDao.firstAyahId(surahId) ?: return
        val entries = dao.entriesForSurah(surahId)
        val upserts = ArrayList<AyahProgressEntity>()
        val removals = ArrayList<Int>()
        for (n in from..to) {
            val ayahId = firstAyahId + n - 1
            val winner = entries.firstOrNull { n in it.fromAyah..it.toAyah }
            if (winner == null) {
                removals.add(ayahId)
            } else {
                upserts.add(AyahProgressEntity(ayahId, winner.id, winner.state, winner.mastery, winner.memorizedOn))
            }
        }
        if (removals.isNotEmpty()) dao.deleteProgress(removals)
        if (upserts.isNotEmpty()) dao.upsertProgress(upserts)
    }

    private suspend fun evaluateAchievements(now: Long): List<Achievement> {
        val values = mapOf(
            AchievementMetric.MEMORIZED_AYAHS to dao.memorizedCount(),
            AchievementMetric.COMPLETED_SURAHS to dao.completedSurahsCount(),
            AchievementMetric.MEMORIZED_JUZ to dao.memorizedJuzCount(),
            AchievementMetric.STREAK_DAYS to StreakCalculator.current(dao.entryDays(), EpochDay.today()),
            AchievementMetric.REVIEWED_AYAHS to 0, // review sessions arrive in phase 4
        )
        val unlocked = dao.unlockedKeys().toSet()
        val fresh = Achievement.entries.filter { it.key !in unlocked && (values[it.metric] ?: 0) >= it.target }
        fresh.forEach { dao.insertAchievement(AchievementEntity(it.key, now)) }
        return fresh
    }
}

private fun markOf(p: AyahProgressEntity): AyahMark = when {
    p.state == MemorizationState.IN_PROGRESS.name -> AyahMark.IN_PROGRESS
    p.mastery == MasteryLevel.NEEDS_REVIEW.name || p.mastery == MasteryLevel.WEAK.name -> AyahMark.NEEDS_REVIEW
    else -> AyahMark.MEMORIZED
}

private fun MemorizationEntryEntity.toDomain(): MemorizationEntry {
    val parsedState = runCatching { MemorizationState.valueOf(state) }.getOrDefault(MemorizationState.IN_PROGRESS)
    val parsedMastery = runCatching { MasteryLevel.valueOf(mastery) }.getOrDefault(MasteryLevel.IN_PROGRESS)
    return MemorizationEntry(id, surahId, fromAyah, toAyah, memorizedOn, parsedState, parsedMastery, note)
}

private fun MemorizationEntry.toEntity(createdAt: Long, updatedAt: Long) = MemorizationEntryEntity(
    id = id,
    surahId = surahId,
    fromAyah = fromAyah,
    toAyah = toAyah,
    memorizedOn = memorizedOn,
    state = state.name,
    mastery = displayMastery.name,
    note = note.trim(),
    createdAt = createdAt,
    updatedAt = updatedAt,
)
