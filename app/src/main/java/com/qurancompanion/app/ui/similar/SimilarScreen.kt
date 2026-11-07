package com.qurancompanion.app.ui.similar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import com.qurancompanion.app.data.similarity.ArabicText
import com.qurancompanion.app.domain.model.Ayah
import com.qurancompanion.app.domain.model.Confusion
import com.qurancompanion.app.domain.model.SimilarGroup
import com.qurancompanion.app.ui.common.toArabicIndic
import com.qurancompanion.app.ui.components.GlassCard
import com.qurancompanion.app.ui.components.Pill
import com.qurancompanion.app.ui.components.ScreenHeader

private fun ref(ayah: Ayah?, names: Map<Int, String>): String =
    if (ayah == null) "" else "${names[ayah.surahId].orEmpty()} ${ayah.ayahNumber.toArabicIndic()}"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimilarScreen(
    onBack: () -> Unit,
    onOpenGroup: (Int) -> Unit,
    onOpenQuiz: () -> Unit,
    viewModel: SimilarViewModel = viewModel(factory = SimilarViewModel.Factory),
) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val shown = remember(s.groups, s.memorized, s.onlyMemorized) {
        if (s.onlyMemorized) s.groups.filter { g -> g.ayahIds.any { it in s.memorized } } else s.groups
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenHeader("كاشف المتشابهات", onBack)
        when {
            s.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator()
                    Text(
                        "جارٍ تحليل الآيات المتشابهة من نص المصحف (مرة واحدة فقط)…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            !s.ready -> Text(
                "نص المصحف غير محمّل بعد. افتح تبويب المصحف للتحقق من الحالة.",
                modifier = Modifier.padding(16.dp),
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    GlassCard(Modifier.fillMaxWidth()) {
                        Text(
                            "${s.groups.size.toArabicIndic()} مجموعة آيات متشابهة لفظيًا",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            "تُحسب من نص المصحف نفسه بمقارنة الكلمات، فهي مؤشر لمساعدتك على الانتباه وليست حكمًا تفسيريًا.",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        Button(onClick = onOpenQuiz, modifier = Modifier.padding(top = 8.dp)) { Text("اختبار المتشابهات") }
                    }
                }
                item {
                    FilterChip(
                        selected = s.onlyMemorized,
                        onClick = viewModel::toggleOnlyMemorized,
                        label = { Text("ما يخص محفوظي فقط") },
                    )
                }
                if (s.confusions.isNotEmpty()) {
                    item { Text("ما تخلط بينه", style = MaterialTheme.typography.titleMedium) }
                    items(s.confusions, key = { "c${it.ayahA}-${it.ayahB}" }) { c ->
                        ConfusionRow(c, s) { s.groupIdByAyah[c.ayahA]?.let(onOpenGroup) }
                    }
                    item { Text("المجموعات", style = MaterialTheme.typography.titleMedium) }
                }
                if (shown.isEmpty()) {
                    item {
                        Text(
                            if (s.onlyMemorized) "لا توجد متشابهات تخص ما سجّلته من حفظ حتى الآن." else "لا توجد نتائج.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(shown, key = { it.id }) { g -> GroupCard(g, s) { onOpenGroup(g.id) } }
            }
        }
    }
}

@Composable
private fun ConfusionRow(c: Confusion, s: SimilarUiState, onClick: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth(), onClick = onClick, borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)) {
        Text(
            "${ref(s.ayahs[c.ayahA], s.surahNames)}  ↔  ${ref(s.ayahs[c.ayahB], s.surahNames)}",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            "اختلطت عليك ${c.times.toArabicIndic()} مرة في الاختبارات",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GroupCard(g: SimilarGroup, s: SimilarUiState, onClick: () -> Unit) {
    val first = s.ayahs[g.ayahIds.first()]
    val preview = first?.let { ArabicText.tokenize(it.text).original.take(8).joinToString(" ") + " …" }.orEmpty()
    val memorizedCount = g.ayahIds.count { it in s.memorized }
    GlassCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Text(preview, style = MaterialTheme.typography.titleMedium)
        Text(
            g.ayahIds.take(3).joinToString(" • ") { ref(s.ayahs[it], s.surahNames) } +
                if (g.ayahIds.size > 3) " …" else "",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill("${g.ayahIds.size.toArabicIndic()} مواضع", MaterialTheme.colorScheme.secondary)
            if (memorizedCount > 0) Pill("${memorizedCount.toArabicIndic()} محفوظة", MaterialTheme.colorScheme.primary)
        }
    }
}
