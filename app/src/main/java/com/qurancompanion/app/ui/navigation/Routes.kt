package com.qurancompanion.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector

object Routes {
    const val HOME = "home"
    const val QURAN = "quran"
    const val MEMORIZE = "memorize"
    const val REVIEW = "review"
    const val MORE = "more"
    const val STATS = "stats"
    const val GOALS = "goals"
    const val QUIZ = "quiz"
    const val ACHIEVEMENTS = "achievements"
    const val ACTIVITY = "activity"
    const val SETTINGS = "settings"
    const val BACKUP = "backup"
    const val MAP = "map"
    const val SIMILAR = "similar"
    const val GOAL_FORM = "goal_form"

    const val READER = "reader/{page}?ayah={ayah}"
    const val MEMORIZE_FORM = "memorize_form?id={id}&start={start}&end={end}"
    const val REVIEW_SESSION = "review_session/{entryId}"
    const val SIMILAR_GROUP = "similar_group/{id}"
    const val QUIZ_PATTERN = "quiz?type={type}"

    fun reader(page: Int, ayahId: Int = 0) = "reader/$page?ayah=$ayahId"
    fun memorizeForm(id: Long = 0L, start: Int = 0, end: Int = 0) = "memorize_form?id=$id&start=$start&end=$end"
    fun reviewSession(entryId: Long) = "review_session/$entryId"
    fun similarGroup(id: Int) = "similar_group/$id"
    fun quiz(type: String) = "quiz?type=$type"
}

data class AppDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val phase: String = "",
)

val TopLevelDestinations = listOf(
    AppDestination(Routes.HOME, "الرئيسية", Icons.Rounded.Home),
    AppDestination(Routes.QURAN, "المصحف", Icons.Rounded.MenuBook),
    AppDestination(Routes.MEMORIZE, "الحفظ", Icons.Rounded.AutoStories),
    AppDestination(Routes.REVIEW, "المراجعة", Icons.Rounded.Replay),
    AppDestination(Routes.MORE, "المزيد", Icons.Rounded.MoreHoriz),
)

val MoreDestinations = listOf(
    AppDestination(Routes.STATS, "الإحصائيات", Icons.Rounded.BarChart),
    AppDestination(Routes.GOALS, "الأهداف ومساعد التوزيع", Icons.Rounded.Flag),
    AppDestination(Routes.MAP, "خريطة الحفظ", Icons.Rounded.GridView),
    AppDestination(Routes.QUIZ, "الاختبارات", Icons.Rounded.Quiz),
    AppDestination(Routes.SIMILAR, "كاشف المتشابهات", Icons.Rounded.ContentCopy),
    AppDestination(Routes.ACHIEVEMENTS, "الإنجازات", Icons.Rounded.EmojiEvents),
    AppDestination(Routes.ACTIVITY, "سجل النشاط", Icons.Rounded.History),
    AppDestination(Routes.SETTINGS, "الإعدادات", Icons.Rounded.Settings, "المرحلة السابعة"),
    AppDestination(Routes.BACKUP, "النسخ الاحتياطي", Icons.Rounded.Backup, "المرحلة السابعة"),
)
