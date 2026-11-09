package com.qurancompanion.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

// ----- row types for joined / aggregated queries -----
data class ReviewItemRow(
    val entryId: Long,
    val nextReviewDay: Long,
    val intervalDays: Int,
    val ease: Double,
    val repetitions: Int,
    val lapses: Int,
    val lastReviewDay: Long,
    val surahId: Int,
    val fromAyah: Int,
    val toAyah: Int,
    val mastery: String,
)

data class DayCountRow(val day: Long, val total: Int)
data class MasteryCountRow(val level: String, val total: Int)
data class SurahProgressRow(
    val surahId: Int,
    val memorized: Int,
    val needsReview: Int,
    val weak: Int,
    val mastered: Int,
)

private const val REVIEW_ITEM_SELECT =
    "SELECT r.entryId AS entryId, r.nextReviewDay AS nextReviewDay, r.intervalDays AS intervalDays, " +
        "r.ease AS ease, r.repetitions AS repetitions, r.lapses AS lapses, r.lastReviewDay AS lastReviewDay, " +
        "e.surahId AS surahId, e.fromAyah AS fromAyah, e.toAyah AS toAyah, e.mastery AS mastery " +
        "FROM review_schedules r INNER JOIN memorization_entries e ON e.id = r.entryId"

@Dao
interface ReviewDao {
    @Query(REVIEW_ITEM_SELECT)
    fun observeItems(): Flow<List<ReviewItemRow>>

    @Query("$REVIEW_ITEM_SELECT WHERE r.entryId = :entryId")
    suspend fun getItemRow(entryId: Long): ReviewItemRow?

    @Query(REVIEW_ITEM_SELECT)
    suspend fun itemRows(): List<ReviewItemRow>

    @Query("UPDATE review_schedules SET nextReviewDay = :day WHERE entryId = :entryId")
    suspend fun setNextDay(entryId: Long, day: Long)

    @Query("SELECT * FROM review_schedules WHERE entryId = :entryId")
    suspend fun getSchedule(entryId: Long): ReviewScheduleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSchedule(item: ReviewScheduleEntity)

    @Query("DELETE FROM review_schedules WHERE entryId = :entryId")
    suspend fun deleteSchedule(entryId: Long)

    @Insert
    suspend fun insertSession(item: ReviewSessionEntity)

    @Query(
        "SELECT COALESCE(SUM(e.toAyah - e.fromAyah + 1), 0) FROM review_schedules r " +
            "INNER JOIN memorization_entries e ON e.id = r.entryId WHERE r.nextReviewDay <= :day",
    )
    fun observeDueAyahs(day: Long): Flow<Int>

    @Query("SELECT COALESCE(SUM(toAyah - fromAyah + 1), 0) FROM review_sessions")
    fun observeReviewedAyahs(): Flow<Int>

    @Query("SELECT COALESCE(SUM(toAyah - fromAyah + 1), 0) FROM review_sessions")
    suspend fun reviewedAyahs(): Int

    @Query("SELECT COALESCE(SUM(toAyah - fromAyah + 1), 0) FROM review_sessions WHERE reviewedOn = :day")
    fun observeReviewedOnDay(day: Long): Flow<Int>

    @Query("SELECT * FROM review_sessions WHERE entryId = :entryId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun recentSessions(entryId: Long, limit: Int): List<ReviewSessionEntity>

    @Query("SELECT DISTINCT reviewedOn FROM review_sessions")
    fun observeReviewDays(): Flow<List<Long>>

    @Query("SELECT DISTINCT reviewedOn FROM review_sessions")
    suspend fun reviewDays(): List<Long>
}

@Dao
interface PlanningDao {
    @Insert
    suspend fun insertGoal(goal: GoalEntity): Long

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteGoal(id: Long)

    @Query("SELECT * FROM goals ORDER BY createdAt DESC, id DESC")
    fun observeGoals(): Flow<List<GoalEntity>>

    @Query("SELECT COUNT(*) FROM ayah_progress WHERE state = 'MEMORIZED' AND ayahId BETWEEN :startId AND :endId")
    fun observeMemorizedInRange(startId: Int, endId: Int): Flow<Int>

    /** Pages memorized on [day], counting each ayah as its share of its page. */
    @Query(
        "SELECT COALESCE(SUM(1.0 / pc.cnt), 0.0) FROM ayah_progress p " +
            "INNER JOIN ayahs a ON a.id = p.ayahId " +
            "INNER JOIN (SELECT page AS pg, COUNT(*) AS cnt FROM ayahs GROUP BY page) pc ON pc.pg = a.page " +
            "WHERE p.memorizedOn = :day AND p.state = 'MEMORIZED' AND p.isPrevious = 0",
    )
    fun observePagesOnDay(day: Long): Flow<Double>
}

@Dao
interface InsightsDao {
    @Query("SELECT COUNT(*) FROM similar_pairs")
    suspend fun pairCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPairs(items: List<SimilarPairEntity>)

    @Query("DELETE FROM similar_pairs")
    suspend fun deletePairs()

    @Query("SELECT * FROM similar_pairs")
    suspend fun allPairs(): List<SimilarPairEntity>

    @Query("SELECT * FROM confusions WHERE ayahA = :a AND ayahB = :b")
    suspend fun getConfusion(a: Int, b: Int): ConfusionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConfusion(item: ConfusionEntity)

    @Query("SELECT * FROM confusions ORDER BY times DESC, lastAt DESC LIMIT 50")
    fun observeConfusions(): Flow<List<ConfusionEntity>>

    @Insert
    suspend fun insertQuiz(item: QuizResultEntity)

    @Query(
        "SELECT a.* FROM ayahs a INNER JOIN ayah_progress p ON p.ayahId = a.id " +
            "WHERE p.state = 'MEMORIZED' AND a.surahId BETWEEN :fromSurah AND :toSurah ORDER BY a.id",
    )
    suspend fun memorizedAyahs(fromSurah: Int, toSurah: Int): List<AyahEntity>

    @Query("SELECT entryId FROM ayah_progress WHERE ayahId = :ayahId")
    suspend fun entryIdOfAyah(ayahId: Int): Long?

    @Query("SELECT ayahId FROM ayah_progress WHERE state = 'MEMORIZED'")
    fun observeMemorizedAyahIds(): Flow<List<Int>>
}

@Dao
interface StatsDao {
    @Query("SELECT memorizedOn AS day, COUNT(*) AS total FROM ayah_progress WHERE state = 'MEMORIZED' AND isPrevious = 0 GROUP BY memorizedOn ORDER BY memorizedOn")
    fun observeMemorizedPerDay(): Flow<List<DayCountRow>>

    @Query("SELECT reviewedOn AS day, SUM(toAyah - fromAyah + 1) AS total FROM review_sessions GROUP BY reviewedOn ORDER BY reviewedOn")
    fun observeReviewedPerDay(): Flow<List<DayCountRow>>

    @Query("SELECT mastery AS level, COUNT(*) AS total FROM ayah_progress GROUP BY mastery")
    fun observeMasteryCounts(): Flow<List<MasteryCountRow>>

    @Query("SELECT COUNT(*) FROM memorization_entries WHERE isPrevious = 0")
    fun observeEntryCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM ayah_progress WHERE state = 'MEMORIZED' AND isPrevious = 1")
    fun observePreviousCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM review_sessions")
    fun observeReviewSessionCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM quiz_results")
    fun observeQuizCount(): Flow<Int>

    @Query(
        "SELECT a.surahId AS surahId, COUNT(*) AS memorized, " +
            "SUM(CASE WHEN p.mastery = 'NEEDS_REVIEW' THEN 1 ELSE 0 END) AS needsReview, " +
            "SUM(CASE WHEN p.mastery = 'WEAK' THEN 1 ELSE 0 END) AS weak, " +
            "SUM(CASE WHEN p.mastery = 'MASTERED' THEN 1 ELSE 0 END) AS mastered " +
            "FROM ayah_progress p INNER JOIN ayahs a ON a.id = p.ayahId " +
            "WHERE p.state = 'MEMORIZED' GROUP BY a.surahId",
    )
    fun observeSurahProgress(): Flow<List<SurahProgressRow>>
}

@Dao
interface ReadingDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<ReadingLogEntity>)

    @Query("SELECT COUNT(*) FROM reading_log WHERE day = :day")
    fun observePagesOnDay(day: Long): Flow<Int>

    @Query("SELECT COUNT(DISTINCT page) FROM reading_log WHERE day >= :sinceDay")
    fun observeDistinctPagesSince(sinceDay: Long): Flow<Int>
}
