package com.qurancompanion.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Spaced-repetition state of one memorization record (only for records marked as memorized). */
@Entity(tableName = "review_schedules", indices = [Index(value = ["nextReviewDay"])])
data class ReviewScheduleEntity(
    @PrimaryKey val entryId: Long,
    val nextReviewDay: Long,
    val intervalDays: Int,
    val ease: Double,
    val repetitions: Int,
    val lapses: Int,
    val lastReviewDay: Long,
)

@Entity(tableName = "review_sessions", indices = [Index(value = ["reviewedOn"])])
data class ReviewSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entryId: Long,
    val surahId: Int,
    val fromAyah: Int,
    val toAyah: Int,
    val reviewedOn: Long,
    val timestamp: Long,
    val rating: String,
    val errorCount: Int,
    val note: String,
    val nextReviewDay: Long,
    val source: String,
)

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kind: String,
    val startAyahId: Int,
    val endAyahId: Int,
    val startDay: Long,
    val endDay: Long,
    val dailyAmount: Int,
    val unit: String,
    val createdAt: Long,
)

@Entity(tableName = "similar_pairs", primaryKeys = ["ayahA", "ayahB"], indices = [Index(value = ["ayahB"])])
data class SimilarPairEntity(
    val ayahA: Int,
    val ayahB: Int,
    val score: Double,
)

@Entity(tableName = "confusions", primaryKeys = ["ayahA", "ayahB"])
data class ConfusionEntity(
    val ayahA: Int,
    val ayahB: Int,
    val times: Int,
    val lastAt: Long,
)

@Entity(tableName = "quiz_results", indices = [Index(value = ["day"])])
data class QuizResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val day: Long,
    val type: String,
    val total: Int,
    val correct: Int,
    val rating: String,
)

/** A mushaf page the user actually read on a given day (feeds the daily wird and the khatmah). */
@Entity(tableName = "reading_log", primaryKeys = ["page", "day"], indices = [Index(value = ["day"])])
data class ReadingLogEntity(
    val page: Int,
    val day: Long,
)
