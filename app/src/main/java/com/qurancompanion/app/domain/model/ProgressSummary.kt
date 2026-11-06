package com.qurancompanion.app.domain.model

data class ProgressSummary(
    val memorizedAyahs: Int = 0,
    val completedSurahs: Int = 0,
    val memorizedJuz: Int = 0,
    val memorizedPages: Int = 0,
    val streakDays: Int = 0,
    val todayTargetAyahs: Int = 0,
    val todayDoneAyahs: Int = 0,
    val dueReviewAyahs: Int = 0,
) {
    val fraction: Float
        get() = (memorizedAyahs / TOTAL_AYAHS.toFloat()).coerceIn(0f, 1f)

    val percent: Int
        get() = (fraction * 100).toInt()

    val todayFraction: Float
        get() = if (todayTargetAyahs <= 0) 0f else (todayDoneAyahs / todayTargetAyahs.toFloat()).coerceIn(0f, 1f)

    companion object {
        const val TOTAL_AYAHS = 6236
        const val TOTAL_SURAHS = 114
        const val TOTAL_JUZ = 30
        const val TOTAL_PAGES = 604
    }
}
