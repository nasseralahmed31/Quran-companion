package com.qurancompanion.app.ui.quran

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
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
import com.qurancompanion.app.domain.model.AyahMarker
import com.qurancompanion.app.domain.model.MasteryLevel
import com.qurancompanion.app.domain.model.MemorizationState
import com.qurancompanion.app.domain.model.QuranConstants
import com.qurancompanion.app.ui.achievements.AchievementUnlockedDialog
import com.qurancompanion.app.ui.common.AyahSpan
import com.qurancompanion.app.ui.common.MarkColors
import com.qurancompanion.app.ui.common.appendAyahs
import com.qurancompanion.app.ui.common.rememberMarkColors
import com.qurancompanion.app.ui.common.toArabicIndic
import com.qurancompanion.app.ui.components.ScreenHeader
import com.qurancompanion.app.ui.theme.Emerald700
import com.qurancompanion.app.ui.theme.Emerald900
import com.qurancompanion.app.ui.theme.Gold
import com.qurancompanion.app.ui.theme.rememberQuranFontFamily
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private data class PageText(val text: AnnotatedString, val spans: List<AyahSpan>)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    initialPage: Int,
    highlightAyahId: Int,
    onBack: () -> Unit,
    onOpenSimilarGroup: (Int) -> Unit,
    onOpenForm: (Int) -> Unit,
    viewModel: ReaderViewModel = viewModel(factory = ReaderViewModel.Factory),
) {
    val total = QuranConstants.TOTAL_PAGES
    val pagerState = rememberPagerState(
        initialPage = (initialPage - 1).coerceIn(0, total - 1),
        pageCount = { total },
    )
    val surahNames by viewModel.surahNames.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val markers by viewModel.markers.collectAsStateWithLifecycle()
    val achievements by viewModel.achievements.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val fontFamily = rememberQuranFontFamily()
    val scope = rememberCoroutineScope()

    var mushafMode by rememberSaveable { mutableStateOf(true) }
    var hideMode by rememberSaveable { mutableStateOf(false) }
    var revealed by remember { mutableStateOf(setOf<Int>()) }
    var selected by remember { mutableStateOf<Ayah?>(null) }
    var showBookmarks by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableFloatStateOf(pagerState.currentPage.toFloat()) }

    // Remember where the user stopped (only once a swipe has settled), and keep the slider in sync.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage to pagerState.isScrollInProgress }
            .filter { !it.second }
            .map { it.first }
            .distinctUntilChanged()
            .collect {
                sliderValue = it.toFloat()
                viewModel.saveLastRead(it + 1)
            }
    }
    LaunchedEffect(message) {
        if (message != null) {
            delay(2500)
            viewModel.clearMessage()
        }
    }

    val currentPage = pagerState.currentPage + 1
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        ScreenHeader("المصحف الشريف", onBack) {
            Text(
                "صفحة ${currentPage.toArabicIndic()} / ${total.toArabicIndic()}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            IconButton(onClick = { viewModel.toggleBookmark(currentPage) }) {
                Icon(
                    if (currentPage in bookmarks) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                    contentDescription = "إشارة مرجعية",
                    tint = if (currentPage in bookmarks) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { showBookmarks = true }) {
                Icon(Icons.Rounded.Bookmarks, contentDescription = "الإشارات المرجعية")
            }
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(selected = mushafMode, onClick = { mushafMode = !mushafMode }, label = { Text("وضع المصحف") })
            FilterChip(
                selected = hideMode,
                onClick = {
                    hideMode = !hideMode
                    revealed = emptySet()
                },
                label = { Text("إخفاء للتسميع") },
            )
        }
        if (hideMode) {
            Text(
                "تظهر أول كلمة من كل آية؛ اضغط الآية لتكشفها.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        message?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
            )
        }

        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { index ->
            QuranPage(
                page = index + 1,
                highlightAyahId = highlightAyahId,
                surahNames = surahNames,
                viewModel = viewModel,
                mushafMode = mushafMode,
                fontFamily = fontFamily,
                hideMode = hideMode,
                revealed = revealed,
                markers = markers,
                onAyahClick = { ayah ->
                    if (hideMode && ayah.id !in revealed) revealed = revealed + ayah.id else selected = ayah
                },
            )
        }

        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = {
                scope.launch { pagerState.scrollToPage(sliderValue.roundToInt().coerceIn(0, total - 1)) }
            },
            valueRange = 0f..(total - 1).toFloat(),
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }

    selected?.let { ayah ->
        AyahActionDialog(
            ayah = ayah,
            surahName = surahNames[ayah.surahId].orEmpty(),
            viewModel = viewModel,
            onDismiss = { selected = null },
            onOpenSimilarGroup = onOpenSimilarGroup,
            onOpenForm = onOpenForm,
        )
    }
    if (showBookmarks) {
        BookmarksDialog(
            bookmarks = bookmarks,
            onGo = { page ->
                showBookmarks = false
                scope.launch { pagerState.scrollToPage(page - 1) }
            },
            onRemove = { viewModel.toggleBookmark(it) },
            onDismiss = { showBookmarks = false },
        )
    }
    if (achievements.isNotEmpty()) {
        AchievementUnlockedDialog(achievements, onDismiss = { viewModel.clearAchievements() })
    }
}

private enum class QuickStatus(val label: String, val state: MemorizationState, val mastery: MasteryLevel) {
    IN_PROGRESS("قيد الحفظ", MemorizationState.IN_PROGRESS, MasteryLevel.IN_PROGRESS),
    MEMORIZED("محفوظة", MemorizationState.MEMORIZED, MasteryLevel.GOOD),
    NEEDS_REVIEW("تحتاج مراجعة", MemorizationState.MEMORIZED, MasteryLevel.NEEDS_REVIEW),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AyahActionDialog(
    ayah: Ayah,
    surahName: String,
    viewModel: ReaderViewModel,
    onDismiss: () -> Unit,
    onOpenSimilarGroup: (Int) -> Unit,
    onOpenForm: (Int) -> Unit,
) {
    var status by remember { mutableStateOf(QuickStatus.MEMORIZED) }
    var note by remember { mutableStateOf("") }
    var groupId by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(ayah.id) { groupId = viewModel.similarGroupId(ayah.id) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("سورة $surahName • الآية ${ayah.ayahNumber.toArabicIndic()}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "سجّل حالة هذه الآية مباشرة من الصفحة:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickStatus.entries.forEach { s ->
                        FilterChip(selected = status == s, onClick = { status = s }, label = { Text(s.label) })
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(300) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    label = { Text("ملاحظة (اختياري)") },
                )
                groupId?.let { id ->
                    OutlinedButton(onClick = {
                        onDismiss()
                        onOpenSimilarGroup(id)
                    }) { Text("عرض متشابهاتها") }
                }
                TextButton(onClick = {
                    onDismiss()
                    onOpenForm(ayah.id)
                }) { Text("تسجيل متقدم (تاريخ وإتقان)") }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                viewModel.quickMark(ayah.id, status.state, status.mastery, note)
                onDismiss()
            }) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
    )
}

@Composable
private fun BookmarksDialog(
    bookmarks: Set<Int>,
    onGo: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("الإشارات المرجعية") },
        text = {
            if (bookmarks.isEmpty()) {
                Text("لا توجد إشارات بعد. اضغط أيقونة الإشارة في أعلى الصفحة لإضافة الصفحة الحالية.")
            } else {
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(bookmarks.sorted(), key = { it }) { page ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { onGo(page) }, modifier = Modifier.weight(1f)) {
                                Text("صفحة ${page.toArabicIndic()}")
                            }
                            IconButton(onClick = { onRemove(page) }) {
                                Icon(Icons.Rounded.Close, contentDescription = "حذف الإشارة")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("إغلاق") } },
    )
}

@Composable
private fun QuranPage(
    page: Int,
    highlightAyahId: Int,
    surahNames: Map<Int, String>,
    viewModel: ReaderViewModel,
    mushafMode: Boolean,
    fontFamily: FontFamily?,
    hideMode: Boolean,
    revealed: Set<Int>,
    markers: Map<Int, Set<AyahMarker>>,
    onAyahClick: (Ayah) -> Unit,
) {
    val flow = remember(page) { viewModel.observePage(page) }
    val ayahs by flow.collectAsStateWithLifecycle(initialValue = emptyList())
    val marksFlow = remember(page) { viewModel.observeMarks(page) }
    val marks by marksFlow.collectAsStateWithLifecycle(initialValue = emptyMap())

    if (mushafMode) {
        MushafPage(page, ayahs, highlightAyahId, marks, surahNames, fontFamily, hideMode, revealed, markers, onAyahClick)
    } else {
        ScrollPage(page, ayahs, highlightAyahId, marks, surahNames, fontFamily, hideMode, revealed, markers, onAyahClick)
    }
}

/** A [Text] that reports which ayah was tapped, using the ayah character ranges. */
@Composable
private fun TappableAyahText(
    page: PageText,
    ayahs: List<Ayah>,
    style: TextStyle,
    modifier: Modifier = Modifier,
    onTap: (Ayah) -> Unit,
) {
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    Text(
        text = page.text,
        style = style,
        onTextLayout = { layout = it },
        modifier = modifier.pointerInput(page.spans, ayahs) {
            detectTapGestures { position ->
                val result = layout ?: return@detectTapGestures
                val offset = result.getOffsetForPosition(position)
                val span = page.spans.firstOrNull { offset >= it.start && offset < it.end } ?: return@detectTapGestures
                ayahs.firstOrNull { it.id == span.ayahId }?.let(onTap)
            }
        },
    )
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
    hideMode: Boolean,
    revealed: Set<Int>,
    markers: Map<Int, Set<AyahMarker>>,
    onAyahClick: (Ayah) -> Unit,
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
        // The size is fitted on the full text so hiding ayahs never changes the layout.
        val fitText = remember(ayahs, marks, colors, surahNames, markers) {
            buildMushafText(ayahs, 0, marks, colors, surahNames, gold, false, emptySet(), markers).text
        }
        val shown = remember(ayahs, highlightAyahId, marks, colors, surahNames, hideMode, revealed, markers) {
            buildMushafText(ayahs, highlightAyahId, marks, colors, surahNames, gold, hideMode, revealed, markers)
        }
        val fontSize = remember(fitText, widthPx, heightPx, fontFamily) {
            fitFontSize(measurer, fitText, widthPx, heightPx, fontFamily)
        }
        Box(
            Modifier
                .fillMaxSize()
                .border(BorderStroke(2.dp, gold.copy(alpha = 0.6f)), RoundedCornerShape(18.dp))
                .padding(inset),
        ) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TappableAyahText(
                        page = shown,
                        ayahs = ayahs,
                        style = TextStyle(
                            fontSize = fontSize.sp,
                            lineHeight = (fontSize * 1.9f).sp,
                            fontFamily = fontFamily,
                            color = onBackground,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        onTap = onAyahClick,
                    )
                }
                if (ayahs.isNotEmpty()) {
                    PageFooter(page, ayahs.first().juz, marks.isNotEmpty(), ayahs.any { markers.containsKey(it.id) })
                }
            }
        }
    }
}

@Composable
private fun PageFooter(page: Int, juz: Int, showMarkLegend: Boolean, showMarkerLegend: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (showMarkLegend || showMarkerLegend) MarksLegend(showMarkLegend, showMarkerLegend)
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
    hideMode: Boolean,
    revealed: Set<Int>,
    markers: Map<Int, Set<AyahMarker>>,
): PageText {
    val spans = ArrayList<AyahSpan>()
    val text = buildAnnotatedString {
        for (run in ayahs.splitBySurah()) {
            if (run.first().ayahNumber == 1) {
                withStyle(ParagraphStyle(textAlign = TextAlign.Center)) {
                    withStyle(SpanStyle(color = gold, fontWeight = FontWeight.Bold)) {
                        append("سورة ${names[run.first().surahId].orEmpty()}")
                    }
                }
            }
            withStyle(ParagraphStyle(textAlign = TextAlign.Justify)) {
                appendAyahs(run, highlightAyahId, marks, colors, spans, hideMode, revealed, markers)
            }
        }
    }
    return PageText(text, spans)
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
    hideMode: Boolean,
    revealed: Set<Int>,
    markers: Map<Int, Set<AyahMarker>>,
    onAyahClick: (Ayah) -> Unit,
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
        val hasMarks = marks.isNotEmpty()
        val hasMarkers = ayahs.any { markers.containsKey(it.id) }
        if (hasMarks || hasMarkers) MarksLegend(hasMarks, hasMarkers)
        for (run in ayahs.splitBySurah()) {
            if (run.first().ayahNumber == 1) {
                SurahBanner(surahNames[run.first().surahId].orEmpty())
            }
            AyahRun(run, highlightAyahId, marks, fontFamily, hideMode, revealed, markers, onAyahClick)
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
private fun AyahRun(
    run: List<Ayah>,
    highlightAyahId: Int,
    marks: Map<Int, AyahMark>,
    fontFamily: FontFamily?,
    hideMode: Boolean,
    revealed: Set<Int>,
    markers: Map<Int, Set<AyahMarker>>,
    onAyahClick: (Ayah) -> Unit,
) {
    val colors = rememberMarkColors()
    val built = remember(run, highlightAyahId, marks, colors, hideMode, revealed, markers) {
        val spans = ArrayList<AyahSpan>()
        val text = buildAnnotatedString { appendAyahs(run, highlightAyahId, marks, colors, spans, hideMode, revealed, markers) }
        PageText(text, spans)
    }
    TappableAyahText(
        page = built,
        ayahs = run,
        style = TextStyle(
            fontSize = 26.sp,
            lineHeight = 52.sp,
            fontFamily = fontFamily,
            textAlign = TextAlign.Justify,
            color = MaterialTheme.colorScheme.onBackground,
        ),
        modifier = Modifier.fillMaxWidth(),
        onTap = onAyahClick,
    )
}

@Composable
private fun MarksLegend(showMarks: Boolean, showMarkers: Boolean) {
    val colors = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        if (showMarks) {
            LegendItem("محفوظ", colors.primary)
            LegendItem("قيد الحفظ", colors.secondary)
            LegendItem("يحتاج مراجعة", colors.error)
        }
        if (showMarkers) {
            Text("\u06DE ربع", style = MaterialTheme.typography.labelMedium, color = colors.secondary)
            Text("\u06E9 سجدة", style = MaterialTheme.typography.labelMedium, color = colors.secondary)
        }
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
