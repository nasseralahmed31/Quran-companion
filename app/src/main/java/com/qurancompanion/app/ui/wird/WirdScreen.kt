package com.qurancompanion.app.ui.wird

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.qurancompanion.app.domain.model.GoalStatus
import com.qurancompanion.app.domain.model.WirdToday
import com.qurancompanion.app.ui.common.toArabicIndic
import com.qurancompanion.app.ui.components.GlassCard
import com.qurancompanion.app.ui.components.NumberField
import com.qurancompanion.app.ui.components.Pill
import com.qurancompanion.app.ui.components.ScreenHeader
import kotlinx.coroutines.delay

@Composable
private fun khatmStatusColor(status: GoalStatus): Color = when (status) {
    GoalStatus.COMPLETED, GoalStatus.AHEAD -> Color(0xFF22C55E)
    GoalStatus.ON_TRACK -> MaterialTheme.colorScheme.primary
    GoalStatus.BEHIND -> Color(0xFFF59E0B)
    GoalStatus.OVERDUE -> MaterialTheme.colorScheme.error
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WirdScreen(
    onBack: () -> Unit,
    onOpenReader: (page: Int) -> Unit,
    viewModel: WirdViewModel = viewModel(factory = WirdViewModel.Factory),
) {
    val today by viewModel.today.collectAsStateWithLifecycle()
    val khatm by viewModel.khatm.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    var showTargets by remember { mutableStateOf(false) }
    var showLog by remember { mutableStateOf(false) }
    var confirmCancel by remember { mutableStateOf(false) }
    var daysText by rememberSaveable { mutableStateOf("30") }

    LaunchedEffect(message) {
        if (message != null) {
            delay(3000)
            viewModel.clearMessage()
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenHeader("ورد القرآن والختمة", onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            val wird = today
            GlassCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("ورد اليوم", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    TextButton(onClick = { showTargets = true }) { Text("تعديل الأهداف") }
                }
                if (wird != null) {
                    if (!wird.hasAnyTarget) {
                        Text(
                            "حدّد وردك اليومي للتلاوة والحفظ والمراجعة، أو ابدأ ختمة من البطاقة التالية.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    WirdLine("التلاوة", wird.readPages, wird.readTarget, "صفحة", if (wird.readTargetFromKhatm) "(من خطة الختمة)" else null)
                    WirdLine("الحفظ", wird.memorizedAyahs, wird.memorizeTarget, "آية", null)
                    WirdLine("المراجعة", wird.reviewedAyahs, wird.reviewTarget, "آية", null)
                    if (wird.isComplete) {
                        Text(
                            "أتممت وردك اليوم، بارك الله فيك.",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
                OutlinedButton(onClick = { showLog = true }, modifier = Modifier.padding(top = 8.dp)) {
                    Text("سجّل قراءة يدويًا")
                }
                Text(
                    "تُحسب الصفحة مقروءة إذا بقيت عليها في المصحف نحو ١٥ ثانية.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }

            GlassCard(Modifier.fillMaxWidth()) {
                Text("ختمة التلاوة", style = MaterialTheme.typography.titleMedium)
                val k = khatm
                if (k == null) {
                    Text(
                        "خطّط لختمة القرآن كاملًا (٦٠٤ صفحة) خلال مدة تحددها؛ يوزّع التطبيق الصفحات على الأيام ويحسب المطلوب منك يوميًا بحسب ما قرأته فعلًا.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(30, 60, 90, 180).forEach { d ->
                            FilterChip(
                                selected = daysText == d.toString(),
                                onClick = { daysText = d.toString() },
                                label = { Text("${d.toArabicIndic()} يوم") },
                            )
                        }
                    }
                    NumberField(daysText, { daysText = it }, "المدة بالأيام", Modifier.fillMaxWidth().padding(top = 8.dp), maxLength = 4)
                    Button(
                        onClick = {
                            val d = daysText.toIntOrNull()
                            if (d != null && d in 1..1000) viewModel.startKhatm(d) else viewModel.showMessage("المدة بين ١ و ١٠٠٠ يوم")
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    ) { Text("ابدأ ختمة") }
                } else {
                    LinearProgressIndicator(progress = { k.fraction }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                    Row(
                        Modifier.padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            "قرأت ${k.pagesRead.toArabicIndic()} / ${k.totalPages.toArabicIndic()} صفحة",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Pill(k.status.label, khatmStatusColor(k.status))
                    }
                    Text(
                        "المتبقي ${k.remainingPages.toArabicIndic()} صفحة • ${k.daysLeft.toArabicIndic()} يوم • " +
                            "المطلوب اليوم ≈ ${k.requiredToday.toArabicIndic()} صفحة",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    // The plan splits pages 1..604 evenly over the days (DayPlan ids are page numbers here).
                    val plan = k.todayPlan
                    if (plan != null) {
                        Text(
                            "ورد اليوم المقترح: من صفحة ${plan.fromAyahId.toArabicIndic()} إلى صفحة ${plan.toAyahId.toArabicIndic()}",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 10.dp),
                        )
                        Button(onClick = { onOpenReader(plan.fromAyahId) }, modifier = Modifier.padding(top = 6.dp)) {
                            Text("افتح المصحف عند الصفحة ${plan.fromAyahId.toArabicIndic()}")
                        }
                    } else {
                        Text(
                            "انتهت مدة الختمة؛ يمكنك إلغاؤها وبدء ختمة جديدة.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 10.dp),
                        )
                    }
                    Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = { showLog = true }) { Text("سجّل قراءة") }
                        TextButton(onClick = { confirmCancel = true }) {
                            Text("إلغاء الختمة", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            message?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }

    if (showTargets) {
        TargetsDialog(
            wird = today,
            khatmActive = khatm != null,
            onDismiss = { showTargets = false },
            onSave = { read, memorize, review ->
                viewModel.setTargets(read, memorize, review)
                showTargets = false
            },
        )
    }
    if (showLog) {
        LogPagesDialog(
            onDismiss = { showLog = false },
            onSave = { from, to ->
                viewModel.logPages(from, to)
                showLog = false
            },
        )
    }
    if (confirmCancel) {
        AlertDialog(
            onDismissRequest = { confirmCancel = false },
            title = { Text("إلغاء الختمة؟") },
            text = { Text("تُحذف خطة الختمة فقط، ويبقى سجل قراءتك كما هو.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmCancel = false
                    viewModel.cancelKhatm()
                }) { Text("إلغاء الختمة", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmCancel = false }) { Text("تراجع") } },
        )
    }
}

@Composable
private fun WirdLine(label: String, done: Int, target: Int, unit: String, note: String?) {
    Column(Modifier.padding(vertical = 6.dp)) {
        if (target <= 0) {
            Text(
                "$label: لم يُحدَّد هدف",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Row {
                Text(
                    label + (note?.let { " $it" } ?: ""),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Text("${done.toArabicIndic()} / ${target.toArabicIndic()} $unit", style = MaterialTheme.typography.bodyMedium)
            }
            LinearProgressIndicator(
                progress = { (done / target.toFloat()).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun TargetsDialog(
    wird: WirdToday?,
    khatmActive: Boolean,
    onDismiss: () -> Unit,
    onSave: (read: Int, memorize: Int, review: Int) -> Unit,
) {
    var read by rememberSaveable { mutableStateOf(wird?.manualReadTarget?.takeIf { it > 0 }?.toString().orEmpty()) }
    var memorize by rememberSaveable { mutableStateOf(wird?.memorizeTarget?.takeIf { it > 0 }?.toString().orEmpty()) }
    var review by rememberSaveable { mutableStateOf(wird?.reviewTarget?.takeIf { it > 0 }?.toString().orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("أهداف الورد اليومي") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (khatmActive) {
                    Text(
                        "التلاوة تتبع خطة الختمة الجارية، وهذا الهدف يُستعمل بعد إلغائها.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                NumberField(read, { read = it }, "تلاوة: صفحات في اليوم", Modifier.fillMaxWidth(), maxLength = 3)
                NumberField(memorize, { memorize = it }, "حفظ: آيات في اليوم", Modifier.fillMaxWidth(), maxLength = 4)
                NumberField(review, { review = it }, "مراجعة: آيات في اليوم", Modifier.fillMaxWidth(), maxLength = 4)
                Text(
                    "اترك الحقل فارغًا إن لم تُرد هدفًا له.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(read.toIntOrNull() ?: 0, memorize.toIntOrNull() ?: 0, review.toIntOrNull() ?: 0) }) {
                Text("حفظ")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
    )
}

@Composable
private fun LogPagesDialog(onDismiss: () -> Unit, onSave: (from: Int, to: Int) -> Unit) {
    var from by rememberSaveable { mutableStateOf("") }
    var to by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تسجيل قراءة اليوم") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "لما قرأته خارج التطبيق. تُسجَّل الصفحات في قراءة اليوم.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(from, { from = it }, "من الصفحة", Modifier.weight(1f), maxLength = 3)
                    NumberField(to, { to = it }, "إلى الصفحة", Modifier.weight(1f), maxLength = 3)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val a = from.toIntOrNull()
                val b = to.toIntOrNull()
                if (a != null && b != null) onSave(a, b)
            }) { Text("تسجيل") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
    )
}
