package com.qurancompanion.app.ui.similar

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qurancompanion.app.ui.common.toArabicIndic
import com.qurancompanion.app.ui.components.GlassCard
import com.qurancompanion.app.ui.components.Pill
import com.qurancompanion.app.ui.components.ScreenHeader
import com.qurancompanion.app.ui.theme.rememberQuranFontFamily

@Composable
fun SimilarGroupScreen(groupId: Int, onBack: () -> Unit, onOpenReader: (page: Int, ayahId: Int) -> Unit) {
    val vm: SimilarGroupViewModel = viewModel(
        key = "similar_group_$groupId",
        factory = SimilarGroupViewModel.factory(groupId),
    )
    val s by vm.state.collectAsStateWithLifecycle()
    val fontFamily = rememberQuranFontFamily()
    val highlight = MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenHeader("مواضع التشابه", onBack)
        when {
            s.loading -> CircularProgressIndicator(Modifier.padding(24.dp))
            s.missing -> Text("لم تعد هذه المجموعة موجودة.", modifier = Modifier.padding(16.dp))
            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(
                        "الكلمات المظلَّلة هي ما يختلف بين المواضع؛ وغير المظلَّلة مشتركة بينها كلها.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (s.commonStart.isNotEmpty()) {
                    item {
                        GlassCard(Modifier.fillMaxWidth()) {
                            Text("نقطة الافتراق", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "تتفق كلها في بدايتها: «${s.commonStart}»",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                            s.members.filter { it.nextWord.isNotEmpty() }.forEach { m ->
                                Text(
                                    "ثم تفترق عند: ${s.surahNames[m.ayah.surahId].orEmpty()} ${m.ayah.ayahNumber.toArabicIndic()} ← «${m.nextWord}»",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                items(s.members, key = { it.ayah.id }) { m ->
                    GlassCard(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "سورة ${s.surahNames[m.ayah.surahId].orEmpty()} • الآية ${m.ayah.ayahNumber.toArabicIndic()}",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f),
                            )
                            if (m.ayah.id in s.memorized) Pill("محفوظة", MaterialTheme.colorScheme.primary)
                        }
                        val text = buildAnnotatedString {
                            m.words.forEach { w ->
                                if (w.differs) {
                                    withStyle(SpanStyle(background = highlight, fontWeight = FontWeight.Bold)) { append(w.text) }
                                } else {
                                    append(w.text)
                                }
                                append(" ")
                            }
                        }
                        Text(
                            text = text,
                            fontSize = 22.sp,
                            lineHeight = 42.sp,
                            fontFamily = fontFamily,
                            textAlign = TextAlign.Justify,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        )
                        OutlinedButton(
                            onClick = { onOpenReader(m.ayah.page, m.ayah.id) },
                            modifier = Modifier.padding(top = 8.dp),
                        ) { Text("فتح في المصحف") }
                    }
                }
            }
        }
    }
}
