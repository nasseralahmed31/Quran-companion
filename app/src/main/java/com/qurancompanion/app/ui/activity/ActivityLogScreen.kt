package com.qurancompanion.app.ui.activity

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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qurancompanion.app.domain.model.ActivityItem
import com.qurancompanion.app.ui.common.formatEpochDay
import com.qurancompanion.app.ui.common.rangeLabel
import com.qurancompanion.app.ui.components.GlassCard

private sealed interface LogRow {
    data class Header(val day: Long) : LogRow
    data class Entry(val item: ActivityItem) : LogRow
}

@Composable
fun ActivityLogScreen(
    onBack: () -> Unit,
    viewModel: ActivityLogViewModel = viewModel(factory = ActivityLogViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val rows = remember(state.items) {
        val out = mutableListOf<LogRow>()
        var lastDay: Long? = null
        for (item in state.items) {
            if (item.day != lastDay) {
                out.add(LogRow.Header(item.day))
                lastDay = item.day
            }
            out.add(LogRow.Entry(item))
        }
        out
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "رجوع") }
            Text("سجل النشاط", style = MaterialTheme.typography.titleLarge)
        }
        if (rows.isEmpty()) {
            Text(
                "لا يوجد نشاط بعد. سيظهر هنا كل تسجيل أو تعديل أو حذف للحفظ.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(rows) { row ->
                when (row) {
                    is LogRow.Header -> Text(
                        formatEpochDay(row.day),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    is LogRow.Entry -> {
                        val item = row.item
                        val surah = item.surahId?.let { state.surahNames[it] }.orEmpty()
                        val range = if (item.fromAyah != null && item.toAyah != null) rangeLabel(item.fromAyah, item.toAyah) else ""
                        GlassCard(Modifier.fillMaxWidth()) {
                            Text(item.type.label, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "سورة $surah • $range",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (item.detail.isNotBlank()) {
                                Text(item.detail, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}
