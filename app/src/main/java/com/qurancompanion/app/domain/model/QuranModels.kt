package com.qurancompanion.app.domain.model

data class Surah(
    val id: Int,
    val nameAr: String,
    val nameTranslit: String,
    val nameEn: String,
    val ayahCount: Int,
    val revelationType: String,
    val startPage: Int,
    val firstAyahId: Int,
) {
    val revelationLabel: String
        get() = when {
            revelationType.startsWith("med", ignoreCase = true) -> "مدنية"
            revelationType.startsWith("mec", ignoreCase = true) -> "مكية"
            else -> ""
        }
}

data class Ayah(
    val id: Int,
    val surahId: Int,
    val ayahNumber: Int,
    val text: String,
    val juz: Int,
    val page: Int,
)

data class Juz(
    val id: Int,
    val startSurah: Int,
    val startAyah: Int,
    val startPage: Int,
    val firstAyahId: Int,
)

sealed interface QuranLoadResult {
    data object Ready : QuranLoadResult
    data object MissingFiles : QuranLoadResult
    data class Invalid(val message: String) : QuranLoadResult
    data class Failed(val message: String) : QuranLoadResult
}

/** Extra marks shown in the mushaf on the ayah that starts a hizb quarter / carries a sajda. */
enum class AyahMarker { QUARTER, SAJDA }
