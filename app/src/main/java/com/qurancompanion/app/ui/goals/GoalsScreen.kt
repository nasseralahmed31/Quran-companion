package com.qurancompanion.app.ui.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qurancompanion.app.domain.model.Goal
import com.qurancompanion.app.domain.model.GoalKind
import com.qurancompanion.app.domain.model.GoalProgress
import com.qurancompanion.app.domain.model.GoalStatus
import com.qurancompanion.app.domain.model.GoalUnit
import com.qurancompanion.app.ui.common.formatEpochDay
import com.qurancompanion.app.ui.common.oneDecimal
import com.qurancompanion.app.ui.common.toArabicIndic
import com.qurancompanion.app.ui.components.GlassCard
import com.qurancompanion.app.ui.components.Pill
import com.qurancompanion.app.ui.components.ScreenHeader

@Composable
private fun goalStatusColor(status: GoalStatus): Color = when (status) {
    GoalStatus.COMPLETED, GoalStatus.AHEAD -> Color(0xFF22C55E)
    GoalStatus.ON_TRACK -> MaterialTheme.colorScheme.primary
    GoalStatus.BEHIND -> Color(0xFFF59E0B)
    GoalStatus.OVERDUE -> MaterialTheme.colorScheme.error
}

@Composable
fun GoalsScreen(
    onBack: () -> Unit,
    onNewGoal: () -> Unit,
    onRecord: (startId: Int, endId: Int) -> Unit,
    viewModel: GoalsViewModel = viewModel(factory = GoalsViewModel.Factory),
) {
    val goals by viewModel.progress.collectAsStateWithLifecycle()
    val plans by viewModel.plans.collectAsStateWithLifecycle()
    var expanded by rememberSaveable { mutableStateOf(-1L) }
    var deleting by remember { mutableStateOf<Goal?>(null) }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenHeader("الأهداف", onBack)
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Button(onClick = onNewGoal, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Text("  هدف جديد")
                }
            }
            if (goals.isEmpty()) {
                item {
                    Text(
                        "لا توجد أهداف بعد. يمكنك مثلًا حفظ سورة يوسف خلال ٣٠ يومًا ليوزّع التطبيق الآيات على الأيام، " +
                            "أو تحديد حفظ يومي بالآيات أو الصفحات. ويمكنك إنشاء عدة أهداف معًا.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(goals, key = { it.goal.id }) { gp ->
                GoalCard(
                    gp = gp,
                    isExpanded = expanded == gp.goal.id,
                    plan = plans[gp.goal.id],
                    onTogglePlan = {
                        if (expanded == gp.goal.id) {
                            expanded = -1L
                        } else {
                            expanded = gp.goal.id
                            viewModel.loadPlan(gp.goal)
                        }
                    },
                    onDelete = { deleting = gp.goal },
                    onRecord = onRecord,
                )
            }
        }
    }

    deleting?.let { goal ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("حذف الهدف؟") },
            text = { Text("سيُحذف هدف «${goal.name}» فقط، ولا يتأثر حفظك المسجّل.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(goal.id)
                    deleting = null
                }) { Text("حذف", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("إلغاء") } },
        )
    }
}

@Composable
private fun GoalCard(
    gp: GoalProgress,
    isExpanded: Boolean,
    plan: List<PlanRow>?,
    onTogglePlan: () -> Unit,
    onDelete: () -> Unit,
    onRecord: (Int, Int) -> Unit,
) {
    val goal = gp.goal
    GlassCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(goal.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            val status = gp.status
            if (status != null) Pill(status.label, goalStatusColor(status)) else Pill("يومي", MaterialTheme.colorScheme.secondary)
        }
        LinearProgressIndicator(
            progress = { gp.fraction },
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        )
        if (goal.kind == GoalKind.RANGE) {
            Text(
                "${gp.doneAyahs.toArabicIndic()} / ${gp.totalAyahs.toArabicIndic()} آية (${(gp.fraction * 100).toInt().toArabicIndic()}٪)",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 6.dp),
            )
            Text(
                if (gp.status == GoalStatus.COMPLETED) {
                    "أتممت الهدف، بارك الله فيك."
                } else {
                    "المتبقي ${gp.remainingAyahs.toArabicIndic()} آية • ${gp.daysLeft.toArabicIndic()} يوم • " +
                        "المطلوب يوميًا ≈ ${gp.requiredPerDay.toArabicIndic()} آية • ينتهي ${formatEpochDay(goal.endDay, gp.today)}"
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        } else {
            val amount = if (goal.unit == GoalUnit.PAGES) gp.todayAmount.oneDecimal() else gp.todayAmount.toInt().toArabicIndic()
            Text(
                "اليوم: $amount / ${goal.dailyAmount.toArabicIndic()} ${goal.unit.label}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 6.dp),
            )
            if (goal.unit == GoalUnit.PAGES) {
                Text(
                    "تُحسب الصفحات تقريبيًا بنسبة آيات كل صفحة التي حفظتها اليوم.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (goal.kind == GoalKind.RANGE) {
                OutlinedButton(onClick = onTogglePlan) { Text(if (isExpanded) "إخفاء الجدول" else "جدول التوزيع") }
            }
            TextButton(onClick = onDelete) { Text("حذف", color = MaterialTheme.colorScheme.error) }
        }
        if (isExpanded && goal.kind == GoalKind.RANGE) {
            if (plan == null) {
                Text("جارٍ إعداد الجدول…", style = MaterialTheme.typography.labelMedium)
            } else {
                Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    plan.forEach { row ->
                        val isToday = row.day == gp.today
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "اليوم ${row.dayIndex.toArabicIndic()} • ${formatEpochDay(row.day, gp.today)}" +
                                        if (isToday) " (اليوم)" else "",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isToday) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    "${row.fromLabel} ← ${row.toLabel} (${row.count.toArabicIndic()} آية)",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            if (row.day <= gp.today) {
                                TextButton(onClick = { onRecord(row.fromId, row.toId) }) { Text("سجّل") }
                            }
                        }
                    }
                }
            }
        }
    }
}
