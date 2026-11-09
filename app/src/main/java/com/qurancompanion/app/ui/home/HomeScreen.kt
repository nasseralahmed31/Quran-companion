package com.qurancompanion.app.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.ViewModule
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qurancompanion.app.domain.model.ProgressSummary
import com.qurancompanion.app.domain.model.WirdToday
import com.qurancompanion.app.ui.common.toArabicIndic
import com.qurancompanion.app.ui.components.GlassCard
import com.qurancompanion.app.ui.navigation.Routes
import com.qurancompanion.app.ui.theme.Emerald700
import com.qurancompanion.app.ui.theme.Emerald900
import com.qurancompanion.app.ui.theme.Gold

@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val lastReadPage by viewModel.lastReadPage.collectAsStateWithLifecycle()
    val wirdToday by viewModel.wirdToday.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Header() }
        if (lastReadPage > 0) {
            item { ContinueReadingCard(lastReadPage) { onNavigate(Routes.reader(lastReadPage)) } }
        }
        item { ProgressHero(summary) }
        item { StatsGrid(summary) }
        item { TodayGoalCard(summary) { onNavigate(Routes.MEMORIZE) } }
        item { WirdCard(wirdToday) { onNavigate(Routes.WIRD) } }
        item { ReviewCard(summary) { onNavigate(Routes.REVIEW) } }
        item { QuickActions(onNavigate) }
    }
}

@Composable
private fun Header() {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text("السلام عليكم ورحمة الله", style = MaterialTheme.typography.headlineMedium)
        Text(
            "رفيق القرآن",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

@Composable
private fun ProgressHero(s: ProgressSummary) {
    val animated by animateFloatAsState(s.fraction, tween(900), label = "memorization-progress")
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(Emerald700, Emerald900)))
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Box(Modifier.size(112.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 12.dp.toPx()
                    val topLeft = Offset(stroke / 2, stroke / 2)
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    drawArc(
                        color = Color.White.copy(alpha = 0.15f),
                        startAngle = 0f, sweepAngle = 360f, useCenter = false,
                        topLeft = topLeft, size = arcSize,
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                    if (animated > 0f) {
                        drawArc(
                            color = Gold,
                            startAngle = -90f, sweepAngle = 360f * animated, useCenter = false,
                            topLeft = topLeft, size = arcSize,
                            style = Stroke(stroke, cap = StrokeCap.Round),
                        )
                    }
                }
                Text("${s.percent}%", color = Color.White, style = MaterialTheme.typography.titleLarge)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("تقدّم حفظ القرآن", color = Color.White, style = MaterialTheme.typography.titleLarge)
                Text(
                    "${s.memorizedAyahs} / ${ProgressSummary.TOTAL_AYAHS} آية",
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Composable
private fun StatsGrid(s: ProgressSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(Icons.Rounded.CheckCircle, "السور المكتملة", "${s.completedSurahs} / ${ProgressSummary.TOTAL_SURAHS}", Modifier.weight(1f))
            StatCard(Icons.Rounded.Book, "الأجزاء", "${s.memorizedJuz} / ${ProgressSummary.TOTAL_JUZ}", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(Icons.Rounded.ViewModule, "الصفحات", "${s.memorizedPages} / ${ProgressSummary.TOTAL_PAGES}", Modifier.weight(1f))
            StatCard(Icons.Rounded.LocalFireDepartment, "سلسلة الالتزام", "${s.streakDays} يوم", Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatCard(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TodayGoalCard(s: ProgressSummary, onClick: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.Today, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("هدف اليوم", style = MaterialTheme.typography.titleMedium)
        }
        if (s.todayTargetAyahs <= 0) {
            Text(
                "لم تحدد هدفًا يوميًا بعد.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        } else {
            LinearProgressIndicator(
                progress = { s.todayFraction },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
            Text(
                "المطلوب حفظه اليوم: ${s.todayTargetAyahs} آية • تم: ${s.todayDoneAyahs}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun ReviewCard(s: ProgressSummary, onClick: () -> Unit) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        borderColor = Gold.copy(alpha = 0.7f),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(Icons.Rounded.Replay, contentDescription = null, tint = Gold)
            Column(Modifier.weight(1f)) {
                Text("ماذا أراجع اليوم؟", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (s.dueReviewAyahs == 0) "لا توجد آيات مستحقة للمراجعة الآن"
                    else "${s.dueReviewAyahs} آية مستحقة للمراجعة",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private data class QuickAction(val label: String, val icon: ImageVector, val route: String)

private val quickActions = listOf(
    QuickAction("بدء الحفظ", Icons.Rounded.AutoStories, Routes.MEMORIZE),
    QuickAction("بدء المراجعة", Icons.Rounded.Replay, Routes.REVIEW),
    QuickAction("اختبار الحفظ", Icons.Rounded.Quiz, Routes.QUIZ),
    QuickAction("عرض المصحف", Icons.Rounded.MenuBook, Routes.QURAN),
    QuickAction("الإحصائيات", Icons.Rounded.BarChart, Routes.STATS),
)

@Composable
private fun QuickActions(onNavigate: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("اختصارات سريعة", style = MaterialTheme.typography.titleMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(quickActions) { action ->
                GlassCard(modifier = Modifier.width(104.dp), onClick = { onNavigate(action.route) }) {
                    Icon(
                        action.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                    Text(
                        action.label,
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ContinueReadingCard(page: Int, onClick: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(Icons.Rounded.AutoStories, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text("متابعة القراءة", style = MaterialTheme.typography.titleMedium)
                Text(
                    "آخر صفحة: ${page.toArabicIndic()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun WirdCard(wird: WirdToday?, onClick: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Text("ورد اليوم", style = MaterialTheme.typography.titleMedium)
        if (wird == null || !wird.hasAnyTarget) {
            Text(
                "حدّد وردك اليومي للتلاوة والحفظ والمراجعة، أو ابدأ ختمة.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        } else {
            Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                WirdRow("التلاوة", wird.readPages, wird.readTarget, "صفحة")
                WirdRow("الحفظ", wird.memorizedAyahs, wird.memorizeTarget, "آية")
                WirdRow("المراجعة", wird.reviewedAyahs, wird.reviewTarget, "آية")
            }
        }
    }
}

@Composable
private fun WirdRow(label: String, done: Int, target: Int, unit: String) {
    if (target <= 0) return
    Column {
        Row {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text("${done.toArabicIndic()} / ${target.toArabicIndic()} $unit", style = MaterialTheme.typography.bodyMedium)
        }
        LinearProgressIndicator(
            progress = { (done / target.toFloat()).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        )
    }
}
