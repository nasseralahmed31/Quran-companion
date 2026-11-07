package com.qurancompanion.app.ui.map

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qurancompanion.app.domain.model.MapStatus
import com.qurancompanion.app.domain.model.SurahMapEntry
import com.qurancompanion.app.ui.common.toArabicIndic
import com.qurancompanion.app.ui.components.ScreenHeader

private fun statusColor(status: MapStatus): Color = when (status) {
    MapStatus.MASTERED -> Color(0xFF22C55E)
    MapStatus.NEEDS_REVIEW -> Color(0xFFEAB308)
    MapStatus.WEAK -> Color(0xFFEF4444)
    MapStatus.NOT_MEMORIZED -> Color(0xFF9CA3AF)
}

private fun statusLabel(status: MapStatus): String = when (status) {
    MapStatus.MASTERED -> "متقنة"
    MapStatus.NEEDS_REVIEW -> "تحتاج مراجعة"
    MapStatus.WEAK -> "ضعيفة"
    MapStatus.NOT_MEMORIZED -> "لم تُحفظ بعد"
}

@Composable
fun MapScreen(
    onBack: () -> Unit,
    onOpenReader: (page: Int, ayahId: Int) -> Unit,
    viewModel: MapViewModel = viewModel(factory = MapViewModel.Factory),
) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf<SurahMapEntry?>(null) }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenHeader("خريطة الحفظ", onBack)
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MapStatus.entries.forEach { st ->
                    LegendItem("${statusLabel(st)} (${entries.count { it.status == st }.toArabicIndic()})", statusColor(st))
                }
            }
            Text(
                "لون السورة يعكس أضعف مستوى بين آياتها المحفوظة، والشريط السفلي نسبة ما حفظته منها.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(entries, key = { it.surahId }) { entry -> Tile(entry) { selected = entry } }
        }
    }

    selected?.let { e ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text("سورة ${e.name}") },
            text = {
                val percent = if (e.ayahCount == 0) 0 else e.memorized * 100 / e.ayahCount
                Text(
                    "الحالة: ${statusLabel(e.status)}\n" +
                        "المحفوظ: ${e.memorized.toArabicIndic()} من ${e.ayahCount.toArabicIndic()} آية (${percent.toArabicIndic()}٪)",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    selected = null
                    onOpenReader(e.startPage, 0)
                }) { Text("فتح في المصحف") }
            },
            dismissButton = { TextButton(onClick = { selected = null }) { Text("إغلاق") } },
        )
    }
}

@Composable
private fun Tile(entry: SurahMapEntry, onClick: () -> Unit) {
    val color = statusColor(entry.status)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.2f),
        border = BorderStroke(1.dp, color),
    ) {
        Column(Modifier.padding(horizontal = 6.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(entry.surahId.toArabicIndic(), style = MaterialTheme.typography.labelMedium, color = color)
            Text(entry.name, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            LinearProgressIndicator(
                progress = { if (entry.ayahCount == 0) 0f else entry.memorized / entry.ayahCount.toFloat() },
                color = color,
                trackColor = Color.Transparent,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}
