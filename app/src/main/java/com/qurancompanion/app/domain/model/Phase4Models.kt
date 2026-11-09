package com.qurancompanion.app.domain.model

import com.qurancompanion.app.domain.util.GoalPlanner

// ---------- Review ----------
enum class ReviewRating(val label: String) {
    EXCELLENT("ممتاز"),
    GOOD("جيد"),
    AVERAGE("متوسط"),
    WEAK("ضعيف"),
}

enum class ReviewStatus(val label: String) {
    OVERDUE("متأخرة"),
    DUE_TODAY("مستحقة اليوم"),
    UPCOMING("قريبة"),
    DONE("مكتملة"),
    LATER("لاحقة"),
}

data class ReviewItem(
    val entryId: Long,
    val surahId: Int,
    val fromAyah: Int,
    val toAyah: Int,
    val mastery: MasteryLevel,
    val nextReviewDay: Long,
    val intervalDays: Int,
    val repetitions: Int,
    val lapses: Int,
    val lastReviewDay: Long,
    val status: ReviewStatus,
    val daysOverdue: Int,
) {
    val ayahCount: Int get() = toAyah - fromAyah + 1
}

data class ReviewOutcome(
    val nextReviewDay: Long,
    val intervalDays: Int,
    val newMastery: MasteryLevel,
    val suggestRememorize: Boolean,
    val newAchievements: List<Achievement>,
    val adaptationNote: String? = null,
)

// ---------- Goals ----------
enum class GoalKind { RANGE, DAILY }

enum class GoalUnit(val label: String) {
    AYAHS("آيات"),
    PAGES("صفحات"),
}

data class Goal(
    val id: Long = 0,
    val name: String,
    val kind: GoalKind,
    val startAyahId: Int,
    val endAyahId: Int,
    val startDay: Long,
    val endDay: Long,
    val dailyAmount: Int,
    val unit: GoalUnit,
)

enum class GoalStatus(val label: String) {
    COMPLETED("مكتمل"),
    AHEAD("متقدم عن الخطة"),
    ON_TRACK("على المسار"),
    BEHIND("متأخر عن الخطة"),
    OVERDUE("انتهت المدة"),
}

data class GoalProgress(
    val goal: Goal,
    val doneAyahs: Int,
    val totalAyahs: Int,
    val todayAmount: Double,
    val today: Long,
) {
    private val isRange: Boolean get() = goal.kind == GoalKind.RANGE

    val fraction: Float
        get() = if (isRange) {
            if (totalAyahs <= 0) 0f else (doneAyahs / totalAyahs.toFloat()).coerceIn(0f, 1f)
        } else {
            if (goal.dailyAmount <= 0) 0f else (todayAmount / goal.dailyAmount).toFloat().coerceIn(0f, 1f)
        }

    val remainingAyahs: Int get() = (totalAyahs - doneAyahs).coerceAtLeast(0)
    val daysLeft: Int get() = GoalPlanner.daysLeft(goal.endDay, today)
    val requiredPerDay: Int get() = GoalPlanner.requiredPerDay(remainingAyahs, daysLeft)
    val status: GoalStatus?
        get() = if (isRange) GoalPlanner.status(doneAyahs, totalAyahs, goal.startDay, goal.endDay, today) else null
}

data class DayPlan(val dayIndex: Int, val fromAyahId: Int, val toAyahId: Int) {
    val ayahCount: Int get() = toAyahId - fromAyahId + 1
}

// ---------- Similar ayahs & quizzes ----------
data class SimilarGroup(val id: Int, val ayahIds: List<Int>, val avgScore: Double)

data class Confusion(val ayahA: Int, val ayahB: Int, val times: Int, val lastAt: Long)

enum class QuizType(val label: String, val description: String, val selfGraded: Boolean) {
    RECITE("تسميع مع إخفاء النص", "يظهر اسم السورة ورقم الآية فتسمّعها من حفظك ثم تكشف النص", true),
    COMPLETE("إكمال الآية", "تظهر بداية الآية وتكملها من حفظك", true),
    NEXT_AYAH("الآية التالية", "تُعرض آية وتختار الآية التي تليها", false),
    SIMILAR("المتشابهات", "تُعرض بداية آية لها متشابهات وتختار تكملتها الصحيحة", false),
    SURAH_CHOICE("اختيار السورة الصحيحة", "تُعرض آية وتختار السورة التي هي منها", false),
    RANDOM_START("ابدأ من هنا", "يختار التطبيق موضع بداية عشوائيًا من محفوظك وتتلو منه ثلاث آيات", true),
    COMPREHENSIVE("اختبار عشوائي شامل", "يختار التطبيق موضعًا عشوائيًا من كامل محفوظك ويطلب منك متابعة الآيات التالية", true),
}

data class QuizQuestion(
    val type: QuizType,
    val ayahId: Int,
    val surahId: Int,
    val ayahNumber: Int,
    val prompt: String,
    val answer: String,
    val options: List<String> = emptyList(),
    val correctIndex: Int = -1,
    val optionAyahIds: List<Int> = emptyList(),
)

data class QuestionResult(val ayahId: Int, val correct: Boolean)

sealed interface QuizBuildResult {
    data class Success(val questions: List<QuizQuestion>) : QuizBuildResult
    data class Failure(val message: String) : QuizBuildResult
}

data class QuizFinish(val updatedPassages: Int, val newAchievements: List<Achievement>)

// ---------- Stats & map ----------
data class DayCount(val day: Long, val total: Int)

data class StatsOverview(
    val summary: ProgressSummary,
    val memorizationRecords: Int,
    val reviewSessions: Int,
    val quizSessions: Int,
    val avgDailyAyahs: Double,
    val weeklyConsistencyPercent: Int,
    val monthlyConsistencyPercent: Int,
    val memorizedPerDay: List<DayCount>,
    val reviewedPerDay: List<DayCount>,
    val cumulativeMemorized: List<DayCount>,
    val masteryAyahs: Map<MasteryLevel, Int>,
    val activeDays: Set<Long>,
    val today: Long,
    val paceAyahsPerDay: Double = 0.0,
    val projectedKhatmDay: Long? = null,
)

enum class MapStatus { MASTERED, NEEDS_REVIEW, WEAK, NOT_MEMORIZED }

data class SurahMapEntry(
    val surahId: Int,
    val name: String,
    val ayahCount: Int,
    val memorized: Int,
    val status: MapStatus,
    val startPage: Int,
)

data class SimilarPair(val a: Int, val b: Int, val score: Double)

// ---------- Daily wird and recitation khatmah ----------
data class WirdToday(
    val readPages: Int,
    val readTarget: Int,
    val readTargetFromKhatm: Boolean,
    val memorizedAyahs: Int,
    val memorizeTarget: Int,
    val reviewedAyahs: Int,
    val reviewTarget: Int,
    /** The manual recitation target (the khatmah, when running, overrides [readTarget]). */
    val manualReadTarget: Int = 0,
) {
    val hasAnyTarget: Boolean get() = readTarget > 0 || memorizeTarget > 0 || reviewTarget > 0
    val isComplete: Boolean
        get() = hasAnyTarget && readPages >= readTarget && memorizedAyahs >= memorizeTarget && reviewedAyahs >= reviewTarget
}

/** A plan to recite the whole mushaf (604 pages) within [days] days, counted by pages actually read. */
data class KhatmState(
    val startDay: Long,
    val days: Int,
    val pagesRead: Int,
    val readToday: Int,
    val today: Long,
) {
    val endDay: Long get() = startDay + days - 1
    val totalPages: Int get() = QuranConstants.TOTAL_PAGES
    val remainingPages: Int get() = (totalPages - pagesRead).coerceAtLeast(0)
    val daysLeft: Int get() = GoalPlanner.daysLeft(endDay, today)

    /** Pages to read today; constant through the day because today's reading is added back. */
    val requiredToday: Int
        get() = GoalPlanner.requiredPerDay((remainingPages + minOf(readToday, pagesRead)).coerceAtMost(totalPages), daysLeft)

    val status: GoalStatus get() = GoalPlanner.status(pagesRead, totalPages, startDay, endDay, today)

    /** The pages planned for today (even split of the mushaf over the days), or null once the period is over. */
    val todayPlan: DayPlan?
        get() = GoalPlanner.schedule(1, totalPages, days).firstOrNull { it.dayIndex == (today - startDay + 1).toInt() }

    val fraction: Float get() = (pagesRead / totalPages.toFloat()).coerceIn(0f, 1f)
}

data class RolloverSummary(val overdueMoved: Int, val shiftDays: Int)
