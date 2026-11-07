package com.qurancompanion.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface BackupDao {
    @Query("SELECT * FROM memorization_entries ORDER BY id")
    suspend fun entries(): List<MemorizationEntryEntity>

    @Query("SELECT * FROM review_schedules")
    suspend fun schedules(): List<ReviewScheduleEntity>

    @Query("SELECT * FROM review_sessions ORDER BY id")
    suspend fun sessions(): List<ReviewSessionEntity>

    @Query("SELECT * FROM goals ORDER BY id")
    suspend fun goals(): List<GoalEntity>

    @Query("SELECT * FROM confusions")
    suspend fun confusions(): List<ConfusionEntity>

    @Query("SELECT * FROM quiz_results ORDER BY id")
    suspend fun quizResults(): List<QuizResultEntity>

    @Query("SELECT * FROM achievements")
    suspend fun achievements(): List<AchievementEntity>

    @Query("SELECT * FROM activity_log ORDER BY id")
    suspend fun activity(): List<ActivityEntity>

    @Query("DELETE FROM ayah_progress")
    suspend fun deleteProgress()

    @Query("DELETE FROM memorization_entries")
    suspend fun deleteEntries()

    @Query("DELETE FROM review_schedules")
    suspend fun deleteSchedules()

    @Query("DELETE FROM review_sessions")
    suspend fun deleteSessions()

    @Query("DELETE FROM goals")
    suspend fun deleteGoals()

    @Query("DELETE FROM confusions")
    suspend fun deleteConfusions()

    @Query("DELETE FROM quiz_results")
    suspend fun deleteQuizResults()

    @Query("DELETE FROM achievements")
    suspend fun deleteAchievements()

    @Query("DELETE FROM activity_log")
    suspend fun deleteActivity()

    @Insert
    suspend fun insertEntries(items: List<MemorizationEntryEntity>)

    @Insert
    suspend fun insertSchedules(items: List<ReviewScheduleEntity>)

    @Insert
    suspend fun insertSessions(items: List<ReviewSessionEntity>)

    @Insert
    suspend fun insertGoals(items: List<GoalEntity>)

    @Insert
    suspend fun insertConfusions(items: List<ConfusionEntity>)

    @Insert
    suspend fun insertQuizResults(items: List<QuizResultEntity>)

    @Insert
    suspend fun insertAchievements(items: List<AchievementEntity>)

    @Insert
    suspend fun insertActivity(items: List<ActivityEntity>)
}
