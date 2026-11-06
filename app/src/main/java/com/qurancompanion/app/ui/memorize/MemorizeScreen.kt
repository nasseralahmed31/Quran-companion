package com.qurancompanion.app.ui.memorize

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qurancompanion.app.domain.model.MemorizationEntry
import com.qurancompanion.app.domain.model.MemorizationState
import com.qurancompanion.app.domain.model.QuranConstants
import com.qurancompanion.app.ui.common.formatEpochDay
import com.qurancompanion.app.ui.common.masteryColor
import com.qurancompanion.app.ui.common.rangeLabel
import com.qurancompanion.app.ui.common.toArabicIndic
import com.qurancompanion.app.ui.components.GlassCard
import com.qurancompanion.app.ui.components.NumberField
import com.qurancompanion.app.ui.components.Pill
import kotlinx.coroutines.launch

@Composable
fun MemorizeScreen(
    onOpenForm: (Long) -> Unit,
    onOpenReader: (page: Int, ayahId: Int) -> Unit,
    viewModel: MemorizeViewModel = viewModel(factory = MemorizeViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var showTargetDialog by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("الحفظ", style = MaterialTheme.typography.headlineMedium) }
        item { DailyGoalCard(state, onEdit = { showTargetDialog = true }) }
        item {
            Button(onClick = { onOpenForm(0L) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("تسجيل حفظ جديد")
            }
        }
        item { Text("سجل الحفظ", style = MaterialTheme.typography.titleMedium) }
        if (!state.loading && state.entries.isEmpty()) {
            item {
                Text(
                    "لم تسجّل أي حفظ بعد. ابدأ بتسجيل أول مقطع تحفظه.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(state.entries, key = { it.id }) { entry ->
            EntryCard(
                entry = entry,
                surahName = state.surahNames[entry.surahId].orEmpty(),
                onEdit = { onOpenForm(entry.id) },
                onOpenInMushaf = {
                    scope.launch {
                        viewModel.resolveAyah(entry.surahId, entry.fromAyah)?.let { (page, ayahId) ->
                            onOpenReader(page, ayahId)
                        }
                    }
                },
            )
        }
    }

    if (showTargetDialog) {
        TargetDialog(
            current = state.dailyTarget,
            onDismiss = { showTargetDialog = false },
            onSave = {
                viewModel.setDailyTarget(it)
                showTargetDialog = false
            },
        )
    }
}

@Composable
private fun DailyGoalCard(state: MemorizeUiState, onEdit: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("هدف اليوم", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = onEdit) { Text("تعديل") }
        }
        if (state.dailyTarget <= 0) {
            Text(
                "حدّد عدد الآيات التي تريد حفظها يوميًا.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LinearProgressIndicator(
                progress = { (state.todayDone / state.dailyTarget.toFloat()).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            )
            Text(
                "${state.todayDone.toArabicIndic()} / ${state.dailyTarget.toArabicIndic()} آية محفوظة اليوم",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun EntryCard(
    entry: MemorizationEntry,
    surahName: String,
    onEdit: () -> Unit,
    onOpenInMushaf: () -> Unit,
) {
    GlassCard(Modifier.fillMaxWidth(), onClick = onEdit) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("سورة $surahName", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${entry.rangeLabel()} • ${formatEpochDay(entry.memorizedOn)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onOpenInMushaf) {
                Icon(Icons.Rounded.MenuBook, contentDescription = "فتح في المصحف")
            }
        }
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill(
                entry.state.label,
                if (entry.state == MemorizationState.MEMORIZED) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.secondary
                },
            )
            if (entry.state == MemorizationState.MEMORIZED) {
                Pill(entry.mastery.label, masteryColor(entry.mastery))
            }
        }
        if (entry.note.isNotBlank()) {
            Text(
                entry.note,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun TargetDialog(current: Int, onDismiss: () -> Unit, onSave: (Int) -> Unit) {
    var text by rememberSaveable { mutableStateOf(if (current > 0) current.toString() else "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("هدف الحفظ اليومي") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(text, { text = it }, "عدد الآيات في اليوم", Modifier.fillMaxWidth())
                Text(
                    "اتركه فارغًا لإلغاء الهدف.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave((text.toIntOrNull() ?: 0).coerceIn(0, QuranConstants.TOTAL_AYAHS))
            }) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
    )
}
