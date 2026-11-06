package com.qurancompanion.app.domain.model

/** Whether the user is still learning a passage or has memorized it. Separate from mastery. */
enum class MemorizationState(val label: String) {
    IN_PROGRESS("قيد الحفظ"),
    MEMORIZED("تم الحفظ"),
}

/** How well a passage is known. Ayahs with no record are implicitly [NEW]. */
enum class MasteryLevel(val label: String) {
    NEW("جديد"),
    IN_PROGRESS("قيد الحفظ"),
    GOOD("جيد"),
    MASTERED("متقن"),
    NEEDS_REVIEW("يحتاج إلى مراجعة"),
    WEAK("ضعيف ويحتاج إلى إعادة الحفظ");

    companion object {
        /** Levels the user can pick once a passage is marked as memorized. */
        val selectableWhenMemorized = listOf(GOOD, MASTERED, NEEDS_REVIEW, WEAK)
    }
}

data class MemorizationEntry(
    val id: Long = 0,
    val surahId: Int,
    val fromAyah: Int,
    val toAyah: Int,
    val memorizedOn: Long,
    val state: MemorizationState,
    val mastery: MasteryLevel,
    val note: String = "",
) {
    val ayahCount: Int get() = toAyah - fromAyah + 1

    /** Mastery as shown/stored: a passage still being learned is always "in progress". */
    val displayMastery: MasteryLevel
        get() = if (state == MemorizationState.IN_PROGRESS) MasteryLevel.IN_PROGRESS else mastery
}

enum class ActivityType(val label: String) {
    ADDED("تسجيل حفظ"),
    EDITED("تعديل سجل حفظ"),
    DELETED("حذف سجل حفظ"),
}

data class ActivityItem(
    val id: Long,
    val type: ActivityType,
    val day: Long,
    val timestamp: Long,
    val surahId: Int?,
    val fromAyah: Int?,
    val toAyah: Int?,
    val detail: String,
)

enum class AchievementMetric { MEMORIZED_AYAHS, COMPLETED_SURAHS, MEMORIZED_JUZ, STREAK_DAYS, REVIEWED_AYAHS }

enum class Achievement(
    val key: String,
    val title: String,
    val description: String,
    val metric: AchievementMetric,
    val target: Int,
) {
    FIRST_AYAH("first_ayah", "أول آية محفوظة", "سجّلت حفظ أول آية لك", AchievementMetric.MEMORIZED_AYAHS, 1),
    FIRST_SURAH("first_surah", "أول سورة مكتملة", "أتممت حفظ سورة كاملة", AchievementMetric.COMPLETED_SURAHS, 1),
    FIRST_WEEK("first_week", "أول أسبوع من الالتزام", "سبعة أيام متتالية من الحفظ", AchievementMetric.STREAK_DAYS, 7),
    REVIEW_100("review_100", "مراجعة ١٠٠ آية", "أتممت مراجعة مئة آية", AchievementMetric.REVIEWED_AYAHS, 100),
    FIRST_JUZ("first_juz", "حفظ أول جزء", "أتممت حفظ جزء كامل", AchievementMetric.MEMORIZED_JUZ, 1),
    THIRTY_DAYS("thirty_days", "٣٠ يومًا من الاستمرار", "ثلاثون يومًا متتالية من الحفظ", AchievementMetric.STREAK_DAYS, 30),
}

data class AchievementProgress(
    val achievement: Achievement,
    val current: Int,
    val unlockedAt: Long?,
) {
    val isUnlocked: Boolean get() = unlockedAt != null
    val fraction: Float get() = (current / achievement.target.toFloat()).coerceIn(0f, 1f)
}

/** How an ayah is highlighted in the mushaf. */
enum class AyahMark { MEMORIZED, IN_PROGRESS, NEEDS_REVIEW }

data class SaveResult(val newAchievements: List<Achievement>)
