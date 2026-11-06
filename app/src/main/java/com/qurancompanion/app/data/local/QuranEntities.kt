package com.qurancompanion.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "surahs")
data class SurahEntity(
    @PrimaryKey val id: Int,
    val nameAr: String,
    val nameTranslit: String,
    val nameEn: String,
    val ayahCount: Int,
    val revelationType: String,
    val startPage: Int,
    val firstAyahId: Int,
)

/** [id] is the global ayah number (1..6236) in mushaf order. */
@Entity(
    tableName = "ayahs",
    indices = [
        Index(value = ["surahId", "ayahNumber"], unique = true),
        Index(value = ["page"]),
        Index(value = ["juz"]),
    ],
)
data class AyahEntity(
    @PrimaryKey val id: Int,
    val surahId: Int,
    val ayahNumber: Int,
    val text: String,
    val juz: Int,
    val page: Int,
)

@Entity(tableName = "juz")
data class JuzEntity(
    @PrimaryKey val id: Int,
    val startSurah: Int,
    val startAyah: Int,
    val startPage: Int,
    val firstAyahId: Int,
)
