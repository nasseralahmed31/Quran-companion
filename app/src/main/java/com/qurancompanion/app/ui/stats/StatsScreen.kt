package com.qurancompanion.app.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qurancompanion.app.domain.model.MasteryLevel
import com.qurancompanion.app.domain.model.QuranConstants
import com.qurancompanion.app.domain.model.StatsOverview
import com.qurancompanion.app.ui.common.masteryColor
import com.qurancompanion.app.ui.common.oneDecimal
import com.qurancompanion.app.ui.common.toArabicIndic
import com.qurancompanion.app.ui.common.weekdayIndex
import com.qurancompanion.app.ui.common.weekdayShort
import com.qurancompanion.app.ui.components.GlassCard
import com.qurancompanion.app.ui.components.ScreenHeader

@Composable
fun StatsScreen(onBack: () -> Unit, viewModel: StatsViewModel = viewModel(factory = StatsViewModel.Factory)) {
    val overview by viewModel.overview.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenHeader("الإحصائيات", onBack)
        val data = overview
        if (data == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            StatsContent(data)
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    GlassCard(Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 10.dp))
        content()
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    GlassCard(modifier) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StatsContent(o: StatsOverview) {
    val s = o.summary
    val memorizedByDay = remember(o.memorizedPerDay) { o.memorizedPerDay.associate { it.day to it.total } }
    val reviewedByDay = remember(o.reviewedPerDay) { o.reviewedPerDay.associate { it.day to it.total } }
    val primary = MaterialTheme.colorScheme.primary
    val gold = MaterialTheme.colorScheme.secondary

    val tiles = listOf(
        "آيات محفوظة" to "${s.memorizedAyahs.toArabicIndic()} / ${QuranConstants.TOTAL_AYAHS.toArabicIndic()}",
        "نسبة حفظ القرآن" to "${(s.fraction * 1000).toInt().div(10.0).oneDecimal()}٪",
        "سور مكتملة" to "${s.completedSurahs.toArabicIndic()} / ${QuranConstants.TOTAL_SURAHS.toArabicIndic()}",
        "أجزاء مكتملة" to "${s.memorizedJuz.toArabicIndic()} / ${QuranConstants.TOTAL_JUZ.toArabicIndic()}",
        "صفحات محفوظة" to "${s.memorizedPages.toArabicIndic()} / ${QuranConstants.TOTAL_PAGES.toArabicIndic()}",
        "آيات تمت مراجعتها" to s.reviewedAyahs.toArabicIndic(),
        "جلسات الحفظ" to o.memorizationRecords.toArabicIndic(),
        "جلسات المراجعة" to o.reviewSessions.toArabicIndic(),
        "الاختبارات" to o.quizSessions.toArabicIndic(),
        "متوسط الحفظ اليومي (٣٠ يومًا)" to "${o.avgDailyAyahs.oneDecimal()} آية",
        "سلسلة الالتزام" to "${s.streakDays.toArabicIndic()} يوم",
    )

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        tiles.chunked(2).forEach { pair ->
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pair.forEach { (label, value) -> StatTile(label, value, Modifier.weight(1f)) }
                    if (pair.size == 1) Box(Modifier.weight(1f))
                }
            }
        }

        item {
            Section("تطور الحفظ (آخر ٩٠ يومًا)") {
                LineChart(
                    values = o.cumulativeMemorized.map { it.total.toFloat() },
                    lineColor = primary,
                    fillColor = primary.copy(alpha = 0.15f),
                )
                Text(
                    "المجموع التراكمي للآيات المحفوظة. لا تُظهر الأيام الأولى إلا ما سجّلته فعلًا.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }

        item {
            val days = (o.today - 6..o.today).toList()
            Section("نشاط الأسبوع: الحفظ والمراجعة") {
                PairedBars(
                    labels = days.map { weekdayShort(it) },
                    first = days.map { memorizedByDay[it] ?: 0 },
                    second = days.map { reviewedByDay[it] ?: 0 },
                    firstColor = primary,
                    secondColor = gold,
                )
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Legend("حفظ", primary)
                    Legend("مراجعة", gold)
                }
            }
        }

        item {
            Section("معدل الالتزام") {
                ConsistencyRow("هذا الأسبوع", o.weeklyConsistencyPercent)
                ConsistencyRow("هذا الشهر", o.monthlyConsistencyPercent)
                Text(
                    "نسبة الأيام التي سجّلت فيها حفظًا أو مراجعة.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            val palette = listOf(
                MaterialTheme.colorScheme.surfaceVariant,
                primary.copy(alpha = 0.3f),
                primary.copy(alpha = 0.5f),
                primary.copy(alpha = 0.75f),
                primary,
            )
            val levels = remember(o) { heatmapLevels(o, memorizedByDay, reviewedByDay) }
            Section("خريطة الالتزام (آخر ١٦ أسبوعًا)") {
                Heatmap(levels, palette)
                Text(
                    "كل مربع يوم؛ كلما اشتد اللون زاد ما حفظته وراجعته فيه.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }

        item {
            val week = (o.today - 6..o.today)
            val month = (o.today - 29..o.today)
            val memWeek = week.sumOf { memorizedByDay[it] ?: 0 }
            val revWeek = week.sumOf { reviewedByDay[it] ?: 0 }
            val memMonth = month.sumOf { memorizedByDay[it] ?: 0 }
            val revMonth = month.sumOf { reviewedByDay[it] ?: 0 }
            Section("مقارنة الحفظ بالمراجعة (بالآيات)") {
                CompareRow("آخر ٧ أيام", memWeek, revWeek)
                CompareRow("آخر ٣٠ يومًا", memMonth, revMonth)
            }
        }

        item {
            val order = listOf(
                MasteryLevel.MASTERED, MasteryLevel.GOOD, MasteryLevel.NEEDS_REVIEW,
                MasteryLevel.WEAK, MasteryLevel.IN_PROGRESS, MasteryLevel.NEW,
            )
            val colors = order.associateWith { masteryColor(it) }
            Section("توزيع الإتقان (بالآيات)") {
                StackedBar(order.map { (o.masteryAyahs[it] ?: 0) to colors.getValue(it) })
                Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    order.forEach { level ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(colors.getValue(level)))
                            Text(level.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            Text((o.masteryAyahs[level] ?: 0).toArabicIndic(), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Legend(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ConsistencyRow(label: String, percent: Int) {
    Column(Modifier.padding(bottom = 8.dp)) {
        Row {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text("${percent.toArabicIndic()}٪", style = MaterialTheme.typography.bodyMedium)
        }
        LinearProgressIndicator(progress = { percent / 100f }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
    }
}

@Composable
private fun CompareRow(label: String, memorized: Int, reviewed: Int) {
    val maxValue = maxOf(1, memorized, reviewed).toFloat()
    Column(Modifier.padding(bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("حفظ", style = MaterialTheme.typography.labelMedium, modifier = Modifier.size(width = 36.dp, height = 18.dp))
            LinearProgressIndicator(progress = { memorized / maxValue }, modifier = Modifier.weight(1f))
            Text(memorized.toArabicIndic(), style = MaterialTheme.typography.labelMedium)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("مراجعة", style = MaterialTheme.typography.labelMedium, modifier = Modifier.size(width = 36.dp, height = 18.dp))
            LinearProgressIndicator(
                progress = { reviewed / maxValue },
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f),
            )
            Text(reviewed.toArabicIndic(), style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** 16 weeks ending today, Sunday-aligned columns; -1 marks days after today. */
private fun heatmapLevels(o: StatsOverview, memorized: Map<Long, Int>, reviewed: Map<Long, Int>): List<Int> {
    val firstDay = o.today - 16 * 7 + 1
    val start = firstDay - weekdayIndex(firstDay)
    val totalDays = (o.today - start + 1).toInt()
    val cells = ((totalDays + 6) / 7) * 7
    return (0 until cells).map { i ->
        val day = start + i
        when {
            day > o.today -> -1
            else -> {
                val count = (memorized[day] ?: 0) + (reviewed[day] ?: 0)
                when {
                    count >= 30 -> 4
                    count >= 15 -> 3
                    count >= 5 -> 2
                    count >= 1 || day in o.activeDays -> 1
                    else -> 0
                }
            }
        }
    }
}
