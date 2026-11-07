package com.qurancompanion.app.ui.quran

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qurancompanion.app.domain.model.Ayah
import com.qurancompanion.app.domain.model.AyahMark
import com.qurancompanion.app.domain.model.QuranConstants
import com.qurancompanion.app.ui.common.MarkColors
import com.qurancompanion.app.ui.common.appendAyahs
import com.qurancompanion.app.ui.common.rememberMarkColors
import com.qurancompanion.app.ui.common.toArabicIndic
import com.qurancompanion.app.ui.components.ScreenHeader
import com.qurancompanion.app.ui.theme.Emerald700
import com.qurancompanion.app.ui.theme.Emerald900
import com.qurancompanion.app.ui.theme.Gold
import com.qurancompanion.app.ui.theme.rememberQuranFontFamily

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
    val fontFamily = rememberQuranFontFamily()
    var mushafMode by rememberSaveable { mutableStateOf(true) }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenHeader("المصحف الشريف", onBack) {
            Text(
                "صفحة ${(pagerState.currentPage + 1).toArabicIndic()} / ${total.toArabicIndic()}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            TextButton(onClick = { mushafMode = !mushafMode }) { Text(if (mushafMode) "وضع القراءة" else "وضع المصحف") }
        }
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { index ->
            QuranPage(
                page = index + 1,
                highlightAyahId = highlightAyahId,
                surahNames = surahNames,
                viewModel = viewModel,
                mushafMode = mushafMode,
                fontFamily = fontFamily,
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
    mushafMode: Boolean,
    fontFamily: FontFamily?,
) {
    val flow = remember(page) { viewModel.observePage(page) }
    val ayahs by flow.collectAsStateWithLifecycle(initialValue = emptyList())
    val marksFlow = remember(page) { viewModel.observeMarks(page) }
    val marks by marksFlow.collectAsStateWithLifecycle(initialValue = emptyMap())

    if (mushafMode) {
        MushafPage(page, ayahs, highlightAyahId, marks, surahNames, fontFamily)
    } else {
        ScrollPage(page, ayahs, highlightAyahId, marks, surahNames, fontFamily)
    }
}

/**
 * One mushaf page per screen: the 604-page division of the Madinah mushaf, with the text scaled to
 * fill a framed page. (The exact 15-line layout needs the King Fahd Complex page fonts, which are
 * not bundled; line breaks here follow the screen width.)
 */
@Composable
private fun MushafPage(
    page: Int,
    ayahs: List<Ayah>,
    highlightAyahId: Int,
    marks: Map<Int, AyahMark>,
    surahNames: Map<Int, String>,
    fontFamily: FontFamily?,
) {
    val colors = rememberMarkColors()
    val gold = MaterialTheme.colorScheme.secondary
    val onBackground = MaterialTheme.colorScheme.onBackground
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current

    BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)) {
        val inset = 14.dp
        val widthPx = with(density) { (maxWidth - inset * 2 - 4.dp).roundToPx() }
        val heightPx = with(density) { (maxHeight - inset * 2 - 40.dp).roundToPx() }
        val text = remember(ayahs, highlightAyahId, marks, colors, surahNames) {
            buildMushafText(ayahs, highlightAyahId, marks, colors, surahNames, gold)
        }
        val fontSize = remember(text, widthPx, heightPx, fontFamily) {
            fitFontSize(measurer, text, widthPx, heightPx, fontFamily)
        }
        Box(
            Modifier
                .fillMaxSize()
                .border(BorderStroke(2.dp, gold.copy(alpha = 0.6f)), RoundedCornerShape(18.dp))
                .padding(inset),
        ) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = text,
                        style = TextStyle(
                            fontSize = fontSize.sp,
                            lineHeight = (fontSize * 1.9f).sp,
                            fontFamily = fontFamily,
                            color = onBackground,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (ayahs.isNotEmpty()) {
                    MarksLegendOrFooter(page, ayahs.first().juz, marks.isNotEmpty())
                }
            }
        }
    }
}

@Composable
private fun MarksLegendOrFooter(page: Int, juz: Int, showLegend: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (showLegend) MarksLegend()
        Text(
            "الجزء ${juz.toArabicIndic()}  •  ${page.toArabicIndic()}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun buildMushafText(
    ayahs: List<Ayah>,
    highlightAyahId: Int,
    marks: Map<Int, AyahMark>,
    colors: MarkColors,
    names: Map<Int, String>,
    gold: Color,
): AnnotatedString = buildAnnotatedString {
    for (run in ayahs.splitBySurah()) {
        if (run.first().ayahNumber == 1) {
            withStyle(ParagraphStyle(textAlign = TextAlign.Center)) {
                withStyle(SpanStyle(color = gold, fontWeight = FontWeight.Bold)) {
                    append("سورة ${names[run.first().surahId].orEmpty()}")
                }
            }
        }
        withStyle(ParagraphStyle(textAlign = TextAlign.Justify)) {
            appendAyahs(run, highlightAyahId, marks, colors)
        }
    }
}

/** Largest font size (up to 30sp) at which the whole page text fits in the available box. */
private fun fitFontSize(
    measurer: TextMeasurer,
    text: AnnotatedString,
    widthPx: Int,
    heightPx: Int,
    family: FontFamily?,
): Float {
    if (text.isEmpty() || widthPx <= 0 || heightPx <= 0) return 24f
    var size = 30f
    while (size > 14f) {
        val style = TextStyle(fontSize = size.sp, lineHeight = (size * 1.9f).sp, fontFamily = family)
        val result = measurer.measure(text = text, style = style, constraints = Constraints(maxWidth = widthPx))
        if (result.size.height <= heightPx) return size
        size -= 0.5f
    }
    return 14f
}

@Composable
private fun ScrollPage(
    page: Int,
    ayahs: List<Ayah>,
    highlightAyahId: Int,
    marks: Map<Int, AyahMark>,
    surahNames: Map<Int, String>,
    fontFamily: FontFamily?,
) {
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
            AyahRun(run, highlightAyahId, marks, fontFamily)
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
private fun AyahRun(run: List<Ayah>, highlightAyahId: Int, marks: Map<Int, AyahMark>, fontFamily: FontFamily?) {
    val colors = rememberMarkColors()
    val annotated = remember(run, highlightAyahId, marks, colors) {
        buildAnnotatedString { appendAyahs(run, highlightAyahId, marks, colors) }
    }
    Text(
        text = annotated,
        fontSize = 26.sp,
        lineHeight = 52.sp,
        fontFamily = fontFamily,
        textAlign = TextAlign.Justify,
        color = MaterialTheme.colorScheme.onBackground,
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
private fun LegendItem(label: String, color: Color) {
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
