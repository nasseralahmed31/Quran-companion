package com.qurancompanion.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** One memorization record: a range of ayahs in a surah. [state]/[mastery] hold enum names. */
@Entity(tableName = "memorization_entries", indices = [Index(value = ["surahId"])])
data class MemorizationEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val surahId: Int,
    val fromAyah: Int,
    val toAyah: Int,
    val memorizedOn: Long,
    val state: String,
    val mastery: String,
    val note: String,
    val createdAt: Long,
    val updatedAt: Long,
)

/** Current state of one ayah, derived from the newest record covering it. */
@Entity(tableName = "ayah_progress", indices = [Index(value = ["memorizedOn"])])
data class AyahProgressEntity(
    @PrimaryKey val ayahId: Int,
    val entryId: Long,
    val state: String,
    val mastery: String,
    val memorizedOn: Long,
)

@Entity(tableName = "activity_log", indices = [Index(value = ["day"])])
data class ActivityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val day: Long,
    val timestamp: Long,
    val surahId: Int?,
    val fromAyah: Int?,
    val toAyah: Int?,
    val detail: String,
)

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val achievementKey: String,
    val unlockedAt: Long,
)
