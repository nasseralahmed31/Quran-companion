package com.qurancompanion.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Flag
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
    const val READER = "reader/{page}?ayah={ayah}"
    const val MEMORIZE_FORM = "memorize_form?id={id}"

    fun reader(page: Int, ayahId: Int = 0) = "reader/$page?ayah=$ayahId"

    fun memorizeForm(id: Long = 0L) = "memorize_form?id=$id"
}

data class AppDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val phase: String = "",
)

val TopLevelDestinations = listOf(
    AppDestination(Routes.HOME, "الرئيسية", Icons.Rounded.Home),
    AppDestination(Routes.QURAN, "المصحف", Icons.Rounded.MenuBook, "المرحلة الثانية"),
    AppDestination(Routes.MEMORIZE, "الحفظ", Icons.Rounded.AutoStories, "المرحلة الثالثة"),
    AppDestination(Routes.REVIEW, "المراجعة", Icons.Rounded.Replay, "المرحلة الرابعة"),
    AppDestination(Routes.MORE, "المزيد", Icons.Rounded.MoreHoriz),
)

val MoreDestinations = listOf(
    AppDestination(Routes.STATS, "الإحصائيات", Icons.Rounded.BarChart, "المرحلة السادسة"),
    AppDestination(Routes.GOALS, "الأهداف", Icons.Rounded.Flag, "المرحلة السادسة"),
    AppDestination(Routes.QUIZ, "الاختبارات", Icons.Rounded.Quiz, "المرحلة الخامسة"),
    AppDestination(Routes.ACHIEVEMENTS, "الإنجازات", Icons.Rounded.EmojiEvents, "المرحلة الثالثة"),
    AppDestination(Routes.ACTIVITY, "سجل النشاط", Icons.Rounded.History, "المرحلة الثالثة"),
    AppDestination(Routes.SETTINGS, "الإعدادات", Icons.Rounded.Settings, "المرحلة السابعة"),
    AppDestination(Routes.BACKUP, "النسخ الاحتياطي", Icons.Rounded.Backup, "المرحلة السابعة"),
)
