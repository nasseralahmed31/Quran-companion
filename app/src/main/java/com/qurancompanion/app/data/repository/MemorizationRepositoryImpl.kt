package com.qurancompanion.app.data.repository

import androidx.room.withTransaction
import com.qurancompanion.app.data.local.ActivityEntity
import com.qurancompanion.app.data.local.AyahProgressEntity
import com.qurancompanion.app.data.local.MemorizationEntryEntity
import com.qurancompanion.app.data.local.QuranDatabase
import com.qurancompanion.app.data.local.ReviewScheduleEntity
import com.qurancompanion.app.domain.model.ActivityItem
import com.qurancompanion.app.domain.model.ActivityType
import com.qurancompanion.app.domain.model.AyahMark
import com.qurancompanion.app.domain.model.MasteryLevel
import com.qurancompanion.app.domain.model.MemorizationEntry
import com.qurancompanion.app.domain.model.MemorizationState
import com.qurancompanion.app.domain.model.SaveResult
import com.qurancompanion.app.domain.repository.MemorizationRepository
import com.qurancompanion.app.domain.util.EpochDay
import com.qurancompanion.app.domain.util.ReviewScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MemorizationRepositoryImpl(
    private val db: QuranDatabase,
    private val updater: AyahProgressUpdater,
    private val achievements: AchievementEvaluator,
) : MemorizationRepository {
    private val dao = db.memorizationDao()
    private val quranDao = db.quranDao()
    private val reviewDao = db.reviewDao()

    override fun observeEntries(): Flow<List<MemorizationEntry>> =
        dao.observeEntries().map { list -> list.map { it.toDomain() } }

    override suspend fun getEntry(id: Long): MemorizationEntry? = dao.getEntry(id)?.toDomain()

    override suspend fun countRecordedInRange(surahId: Int, from: Int, to: Int): Int =
        dao.recordedInRange(surahId, from, to)

    override suspend fun save(entry: MemorizationEntry): SaveResult = db.withTransaction {
        val now = System.currentTimeMillis()
        saveOne(entry, now)
        SaveResult(achievements.evaluate(now))
    }

    override suspend fun saveRange(
        startAyahId: Int,
        endAyahId: Int,
        memorizedOn: Long,
        state: MemorizationState,
        mastery: MasteryLevel,
        note: String,
    ): SaveResult = db.withTransaction {
        val first = quranDao.getAyah(startAyahId)
        val last = quranDao.getAyah(endAyahId)
        if (first == null || last == null || endAyahId < startAyahId) {
            throw IllegalStateException("نطاق الآيات غير صحيح")
        }
        val now = System.currentTimeMillis()
        for (surahId in first.surahId..last.surahId) {
            val from = if (surahId == first.surahId) first.ayahNumber else 1
            val to = if (surahId == last.surahId) {
                last.ayahNumber
            } else {
                quranDao.surahAyahCount(surahId) ?: throw IllegalStateException("سورة غير موجودة")
            }
            saveOne(MemorizationEntry(0L, surahId, from, to, memorizedOn, state, mastery, note), now)
        }
        SaveResult(achievements.evaluate(now))
    }

    override suspend fun delete(id: Long) {
        db.withTransaction {
            val old = dao.getEntry(id) ?: return@withTransaction
            dao.deleteEntry(id)
            reviewDao.deleteSchedule(id)
            updater.recomputeRange(old.surahId, old.fromAyah, old.toAyah)
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

    private suspend fun saveOne(entry: MemorizationEntry, now: Long) {
        val type: ActivityType
        val entryId: Long
        if (entry.id == 0L) {
            entryId = dao.insertEntry(entry.toEntity(createdAt = now, updatedAt = now))
            updater.recomputeRange(entry.surahId, entry.fromAyah, entry.toAyah)
            type = ActivityType.ADDED
        } else {
            val old = dao.getEntry(entry.id) ?: throw IllegalStateException("سجل الحفظ غير موجود")
            dao.updateEntry(entry.toEntity(createdAt = old.createdAt, updatedAt = now))
            updater.recomputeRange(old.surahId, old.fromAyah, old.toAyah)
            updater.recomputeRange(entry.surahId, entry.fromAyah, entry.toAyah)
            entryId = entry.id
            type = ActivityType.EDITED
        }
        syncSchedule(entryId, entry)
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
    }

    /**
     * Memorized records are reviewed; records still being learned are not scheduled.
     * A passage flagged as weak / needing review is pulled forward so it never waits behind
     * passages that are in good shape.
     */
    private suspend fun syncSchedule(entryId: Long, entry: MemorizationEntry) {
        if (entry.state != MemorizationState.MEMORIZED) {
            reviewDao.deleteSchedule(entryId)
            return
        }
        val today = EpochDay.today()
        val existing = reviewDao.getSchedule(entryId)
        if (existing == null) {
            val initial = ReviewScheduler.initial()
            reviewDao.upsertSchedule(
                ReviewScheduleEntity(
                    entryId = entryId,
                    nextReviewDay = ReviewScheduler.firstDueDay(entry.memorizedOn, entry.displayMastery, today),
                    intervalDays = initial.intervalDays,
                    ease = initial.ease,
                    repetitions = initial.repetitions,
                    lapses = initial.lapses,
                    lastReviewDay = 0L,
                ),
            )
            return
        }
        val pullTo = when (entry.displayMastery) {
            MasteryLevel.WEAK -> today
            MasteryLevel.NEEDS_REVIEW -> today + 1
            else -> null
        }
        if (pullTo != null && existing.nextReviewDay > pullTo) {
            reviewDao.upsertSchedule(
                existing.copy(nextReviewDay = pullTo, intervalDays = (pullTo - today).toInt().coerceAtLeast(1)),
            )
        }
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
