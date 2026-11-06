package com.qurancompanion.app.ui.quran

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qurancompanion.app.domain.model.Ayah
import com.qurancompanion.app.domain.model.AyahMark
import com.qurancompanion.app.domain.model.QuranConstants
import com.qurancompanion.app.ui.common.toArabicIndic
import com.qurancompanion.app.ui.theme.Emerald700
import com.qurancompanion.app.ui.theme.Emerald900
import com.qurancompanion.app.ui.theme.Gold

@Composable
fun ReaderScreen(
    initialPage: Int,
    highlightAyahId: Int,
    onBack: () -> Unit,
    viewModel: ReaderViewModel = viewModel(factory = ReaderViewModel.Factory),
) {
    val total = QuranConstants.TOTAL_PAGES
    val pagerState = rememberPagerState(
        initialPage = (initialPage - 1).coerceIn(0, total - 1),
        pageCount = { total },
    )
    val surahNames by viewModel.surahNames.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "رجوع")
            }
            Text("المصحف الشريف", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(
                "صفحة ${(pagerState.currentPage + 1).toArabicIndic()} / ${total.toArabicIndic()}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(end = 12.dp),
            )
        }
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { index ->
            QuranPage(
                page = index + 1,
                highlightAyahId = highlightAyahId,
                surahNames = surahNames,
                viewModel = viewModel,
            )
        }
    }
}

@Composable
private fun QuranPage(
    page: Int,
    highlightAyahId: Int,
    surahNames: Map<Int, String>,
    viewModel: ReaderViewModel,
) {
    val flow = remember(page) { viewModel.observePage(page) }
    val ayahs by flow.collectAsStateWithLifecycle(initialValue = emptyList())
    val marksFlow = remember(page) { viewModel.observeMarks(page) }
    val marks by marksFlow.collectAsStateWithLifecycle(initialValue = emptyMap())

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (ayahs.isNotEmpty()) {
            Text(
                "الجزء ${ayahs.first().juz.toArabicIndic()} • صفحة ${page.toArabicIndic()}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (marks.isNotEmpty()) MarksLegend()
        for (run in ayahs.splitBySurah()) {
            if (run.first().ayahNumber == 1) {
                SurahBanner(surahNames[run.first().surahId].orEmpty())
            }
            AyahRun(run, highlightAyahId, marks)
        }
    }
}

@Composable
private fun SurahBanner(name: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Emerald700, Emerald900)))
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text("سورة $name", color = Gold, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun AyahRun(run: List<Ayah>, highlightAyahId: Int, marks: Map<Int, AyahMark>) {
    val colors = MaterialTheme.colorScheme
    val target = colors.primary.copy(alpha = 0.32f)
    val memorized = colors.primary.copy(alpha = 0.12f)
    val inProgress = colors.secondary.copy(alpha = 0.14f)
    val needsReview = colors.error.copy(alpha = 0.14f)
    val marker = colors.secondary
    val annotated = remember(run, highlightAyahId, marks, target, memorized, inProgress, needsReview, marker) {
        buildAnnotatedString {
            run.forEach { a ->
                val background = if (a.id == highlightAyahId) {
                    target
                } else {
                    when (marks[a.id]) {
                        AyahMark.MEMORIZED -> memorized
                        AyahMark.IN_PROGRESS -> inProgress
                        AyahMark.NEEDS_REVIEW -> needsReview
                        null -> null
                    }
                }
                if (background != null) {
                    withStyle(SpanStyle(background = background)) { append(a.text) }
                } else {
                    append(a.text)
                }
                withStyle(SpanStyle(color = marker)) {
                    append(" \uFD3F${a.ayahNumber.toArabicIndic()}\uFD3E ")
                }
            }
        }
    }
    Text(
        text = annotated,
        fontSize = 26.sp,
        lineHeight = 52.sp,
        textAlign = TextAlign.Justify,
        color = colors.onBackground,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun MarksLegend() {
    val colors = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        LegendItem("محفوظ", colors.primary)
        LegendItem("قيد الحفظ", colors.secondary)
        LegendItem("يحتاج مراجعة", colors.error)
    }
}

@Composable
private fun LegendItem(label: String, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color.copy(alpha = 0.6f)))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun List<Ayah>.splitBySurah(): List<List<Ayah>> {
    val out = mutableListOf<MutableList<Ayah>>()
    for (a in this) {
        if (out.isEmpty() || out.last().first().surahId != a.surahId) {
            out.add(mutableListOf(a))
        } else {
            out.last().add(a)
        }
    }
    return out
}
