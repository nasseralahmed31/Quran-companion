package com.qurancompanion.app.ui.review

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.qurancompanion.app.domain.model.ReviewItem
import com.qurancompanion.app.domain.model.ReviewStatus
import com.qurancompanion.app.domain.util.ReviewOrdering
import com.qurancompanion.app.ui.common.formatEpochDay
import com.qurancompanion.app.ui.common.masteryColor
import com.qurancompanion.app.ui.common.rangeLabel
import com.qurancompanion.app.ui.common.toArabicIndic
import com.qurancompanion.app.ui.components.GlassCard
import com.qurancompanion.app.ui.components.NumberField
import com.qurancompanion.app.ui.components.Pill
import com.qurancompanion.app.ui.theme.Gold

@Composable
fun statusColor(status: ReviewStatus): Color = when (status) {
    ReviewStatus.OVERDUE -> MaterialTheme.colorScheme.error
    ReviewStatus.DUE_TODAY -> MaterialTheme.colorScheme.secondary
    ReviewStatus.UPCOMING -> MaterialTheme.colorScheme.primary
    ReviewStatus.DONE -> Color(0xFF22C55E)
    ReviewStatus.LATER -> MaterialTheme.colorScheme.onSurfaceVariant
}

private sealed interface ReviewRow {
    data class Header(val group: Int) : ReviewRow
    data class Entry(val item: ReviewItem) : ReviewRow
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    onStart: (Long) -> Unit,
    viewModel: ReviewViewModel = viewModel(factory = ReviewViewModel.Factory),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val names by viewModel.surahNames.collectAsStateWithLifecycle()
    val rolloverNote by viewModel.rolloverNote.collectAsStateWithLifecycle()
    var filter by rememberSaveable { mutableIntStateOf(0) }
    var rescheduleTarget by remember { mutableStateOf<ReviewItem?>(null) }

    fun count(vararg statuses: ReviewStatus) = items.count { it.status in statuses }
    val dueNow = remember(items) {
        items.filter { it.status == ReviewStatus.OVERDUE || it.status == ReviewStatus.DUE_TODAY }
    }
    val needsFirst = remember(dueNow) { dueNow.count { ReviewOrdering.needsReview(it.mastery) } }
    val shown = remember(items, filter) {
        when (filter) {
            0 -> items.filter { it.status == ReviewStatus.OVERDUE || it.status == ReviewStatus.DUE_TODAY }
            1 -> items.filter { it.status == ReviewStatus.UPCOMING }
            2 -> items.filter { it.status == ReviewStatus.DONE }
            else -> items
        }
    }
    // The list is already ordered (needs-review first); headers just mark where each section starts.
    val rows = remember(shown) {
        val out = mutableListOf<ReviewRow>()
        var last = -1
        for (item in shown) {
            val group = ReviewOrdering.group(item)
            if (group != last) {
                out.add(ReviewRow.Header(group))
                last = group
            }
            out.add(ReviewRow.Entry(item))
        }
        out
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("المراجعة", style = MaterialTheme.typography.headlineMedium) }
        item {
            GlassCard(Modifier.fillMaxWidth(), borderColor = Gold.copy(alpha = 0.7f)) {
                Text("ماذا أراجع اليوم؟", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (dueNow.isEmpty()) {
                        "لا توجد مراجعات مستحقة الآن."
                    } else {
                        "${dueNow.size.toArabicIndic()} مقطع (${dueNow.sumOf { it.ayahCount }.toArabicIndic()} آية) بانتظار المراجعة" +
                            if (needsFirst > 0) {
                                "، منها ${needsFirst.toArabicIndic()} تحتاج مراجعة وتأتي في أول القائمة."
                            } else {
                                "."
                            }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        rolloverNote?.let { note ->
            item {
                GlassCard(Modifier.fillMaxWidth()) {
                    Text(note, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "يمكنك إيقاف هذه الميزة من الإعدادات.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = filter == 0, onClick = { filter = 0 },
                    label = { Text("اليوم والمتأخرة (${count(ReviewStatus.OVERDUE, ReviewStatus.DUE_TODAY).toArabicIndic()})") },
                )
                FilterChip(
                    selected = filter == 1, onClick = { filter = 1 },
                    label = { Text("قريبة (${count(ReviewStatus.UPCOMING).toArabicIndic()})") },
                )
                FilterChip(
                    selected = filter == 2, onClick = { filter = 2 },
                    label = { Text("مكتملة (${count(ReviewStatus.DONE).toArabicIndic()})") },
                )
                FilterChip(
                    selected = filter == 3, onClick = { filter = 3 },
                    label = { Text("الكل (${items.size.toArabicIndic()})") },
                )
            }
        }
        if (rows.isEmpty()) {
            item {
                Text(
                    if (items.isEmpty()) {
                        "تدخل المقاطع المحفوظة في جدول المراجعة تلقائيًا. سجّل حفظًا بحالة «تم الحفظ» لتبدأ."
                    } else {
                        "لا توجد عناصر في هذا التصنيف."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(
            rows,
            key = { row ->
                when (row) {
                    is ReviewRow.Header -> "header-${row.group}"
                    is ReviewRow.Entry -> "entry-${row.item.entryId}"
                }
            },
        ) { row ->
            when (row) {
                is ReviewRow.Header -> SectionHeader(row.group)
                is ReviewRow.Entry -> ReviewCard(
                    item = row.item,
                    surahName = names[row.item.surahId].orEmpty(),
                    highlight = ReviewOrdering.group(row.item) == ReviewOrdering.GROUP_NEEDS_REVIEW,
                    onStart = { onStart(row.item.entryId) },
                    onReschedule = { rescheduleTarget = row.item },
                )
            }
        }
    }

    rescheduleTarget?.let { target ->
        RescheduleDialog(
            onDismiss = { rescheduleTarget = null },
            onSave = { days ->
                viewModel.reschedule(target.entryId, days)
                rescheduleTarget = null
            },
        )
    }
}

@Composable
private fun SectionHeader(group: Int) {
    Column(Modifier.padding(top = 4.dp)) {
        Text(
            ReviewOrdering.sectionLabel(group),
            style = MaterialTheme.typography.titleMedium,
            color = if (group == ReviewOrdering.GROUP_NEEDS_REVIEW) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        if (group == ReviewOrdering.GROUP_NEEDS_REVIEW) {
            Text(
                "ضعيفة أو علّمتها «تحتاج مراجعة»؛ تُراجَع قبل غيرها حتى لو تأخر غيرها أكثر.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ReviewCard(
    item: ReviewItem,
    surahName: String,
    highlight: Boolean,
    onStart: () -> Unit,
    onReschedule: () -> Unit,
) {
    GlassCard(
        Modifier.fillMaxWidth(),
        borderColor = if (highlight) {
            MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
        } else {
            MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        },
    ) {
        Text("سورة $surahName", style = MaterialTheme.typography.titleMedium)
        Text(
            rangeLabel(item.fromAyah, item.toAyah),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val statusText = if (item.status == ReviewStatus.OVERDUE) {
                "${item.status.label} ${item.daysOverdue.toArabicIndic()} يوم"
            } else {
                item.status.label
            }
            Pill(statusText, statusColor(item.status))
            Pill(item.mastery.label, masteryColor(item.mastery))
        }
        Text(
            "الموعد: ${formatEpochDay(item.nextReviewDay)} • الفاصل: ${item.intervalDays.toArabicIndic()} يوم",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
        Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = onStart) { Text("ابدأ المراجعة") }
            TextButton(onClick = onReschedule) { Text("تعديل الموعد") }
        }
    }
}

@Composable
private fun RescheduleDialog(onDismiss: () -> Unit, onSave: (Int) -> Unit) {
    var days by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تعديل موعد المراجعة") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(days, { days = it }, "بعد كم يومًا من اليوم؟", Modifier.fillMaxWidth(), maxLength = 3)
                Text(
                    "اكتب ٠ لتكون المراجعة اليوم.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(enabled = days.isNotEmpty(), onClick = { onSave(days.toIntOrNull() ?: 0) }) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
    )
}
