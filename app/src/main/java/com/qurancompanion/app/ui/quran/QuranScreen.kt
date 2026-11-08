package com.qurancompanion.app.ui.quran

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qurancompanion.app.domain.model.Juz
import com.qurancompanion.app.domain.model.QuranConstants
import com.qurancompanion.app.domain.model.QuranLoadResult
import com.qurancompanion.app.domain.model.Surah
import com.qurancompanion.app.ui.common.toArabicIndic
import com.qurancompanion.app.ui.components.GlassCard
import com.qurancompanion.app.ui.components.NumberField
import kotlinx.coroutines.launch

@Composable
fun QuranScreen(
    onOpenReader: (page: Int, ayahId: Int) -> Unit,
    viewModel: QuranViewModel = viewModel(factory = QuranViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Text(
            "المصحف الشريف",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
        )
        when (val s = state) {
            QuranUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is QuranUiState.Problem -> ProblemView(s.result)
            is QuranUiState.Ready -> ReadyContent(s, viewModel, onOpenReader)
        }
    }
}

@Composable
private fun ProblemView(result: QuranLoadResult) {
    val (title, body) = when (result) {
        QuranLoadResult.MissingFiles -> "ملفات نص المصحف غير موجودة" to
            "لم يُضمَّن نص القرآن في هذه النسخة. شغّل scripts/download_quran_data.sh ثم أعد بناء التطبيق. التفاصيل في DATA_SOURCES.md."
        is QuranLoadResult.Invalid -> "فشل التحقق من بيانات القرآن" to
            "لم يُستورد أي نص لأن الملفات لا تطابق الأعداد المعتمدة:\n${result.message}"
        is QuranLoadResult.Failed -> "تعذّر تحميل قاعدة البيانات" to result.message
        QuranLoadResult.Ready -> "" to ""
    }
    GlassCard(Modifier.fillMaxWidth().padding(16.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReadyContent(
    state: QuranUiState.Ready,
    viewModel: QuranViewModel,
    onOpenReader: (Int, Int) -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    val surahNames = remember(state.surahs) { state.surahs.associate { it.id to it.nameAr } }
    val filtered = remember(state.surahs, query) { filterSurahs(state.surahs, query) }
    val uriHandler = LocalUriHandler.current
    val lastRead by viewModel.lastReadPage.collectAsStateWithLifecycle()

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (lastRead > 0) {
            item {
                Button(onClick = { onOpenReader(lastRead, 0) }, modifier = Modifier.fillMaxWidth()) {
                    Text("متابعة القراءة من صفحة ${lastRead.toArabicIndic()}")
                }
            }
        }
        item { JumpCard(state.surahs, viewModel, onOpenReader) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = tab == 0, onClick = { tab = 0 }, label = { Text("السور") })
                FilterChip(selected = tab == 1, onClick = { tab = 1 }, label = { Text("الأجزاء") })
            }
        }
        if (tab == 0) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("ابحث باسم السورة أو رقمها") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                )
            }
            items(filtered, key = { it.id }) { s -> SurahRow(s) { onOpenReader(s.startPage, 0) } }
            if (filtered.isEmpty()) {
                item {
                    Text("لا توجد نتائج", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(state.juz, key = { it.id }) { j ->
                JuzRow(j, surahNames[j.startSurah].orEmpty()) { onOpenReader(j.startPage, 0) }
            }
        }
        item {
            Text(
                "نص القرآن: النص العثماني وفق مصحف المدينة من مشروع تنزيل (tanzil.net)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .clickable { uriHandler.openUri("https://tanzil.net") },
            )
        }
    }
}

private fun filterSurahs(all: List<Surah>, raw: String): List<Surah> {
    val q = raw.trim()
    if (q.isEmpty()) return all
    val number = q.toIntOrNull()
    return all.filter { s ->
        (number != null && s.id == number) ||
            s.nameAr.contains(q) ||
            s.nameTranslit.contains(q, ignoreCase = true) ||
            s.nameEn.contains(q, ignoreCase = true)
    }
}

@Composable
private fun SurahRow(s: Surah, onClick: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            NumberBadge(s.id)
            Column(Modifier.weight(1f)) {
                Text("سورة ${s.nameAr}", style = MaterialTheme.typography.titleMedium)
                val type = if (s.revelationLabel.isEmpty()) "" else "${s.revelationLabel} • "
                Text(
                    "$type${s.ayahCount.toArabicIndic()} آية • صفحة ${s.startPage.toArabicIndic()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun JuzRow(j: Juz, startSurahName: String, onClick: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            NumberBadge(j.id)
            Column(Modifier.weight(1f)) {
                Text("الجزء ${j.id.toArabicIndic()}", style = MaterialTheme.typography.titleMedium)
                Text(
                    "يبدأ من سورة $startSurahName آية ${j.startAyah.toArabicIndic()} • صفحة ${j.startPage.toArabicIndic()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun NumberBadge(number: Int) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            number.toArabicIndic(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun JumpCard(
    surahs: List<Surah>,
    viewModel: QuranViewModel,
    onOpenReader: (Int, Int) -> Unit,
) {
    var page by rememberSaveable { mutableStateOf("") }
    var surah by rememberSaveable { mutableStateOf("") }
    var ayah by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    GlassCard(Modifier.fillMaxWidth()) {
        Text("انتقال سريع", style = MaterialTheme.typography.titleMedium)
        Row(
            Modifier.padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            NumberField(page, { page = it; error = "" }, "رقم الصفحة", Modifier.weight(1f))
            Button(onClick = {
                val p = page.toIntOrNull()
                if (p == null || p !in 1..QuranConstants.TOTAL_PAGES) {
                    error = "رقم الصفحة بين ١ و ٦٠٤"
                } else {
                    onOpenReader(p, 0)
                }
            }) { Text("اذهب") }
        }
        Row(
            Modifier.padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            NumberField(surah, { surah = it; error = "" }, "رقم السورة", Modifier.weight(1f))
            NumberField(ayah, { ayah = it; error = "" }, "رقم الآية", Modifier.weight(1f))
            Button(onClick = {
                val s = surah.toIntOrNull()
                val a = ayah.toIntOrNull()
                val target = surahs.firstOrNull { it.id == s }
                when {
                    s == null || a == null || target == null -> error = "أدخل رقم سورة صحيح بين ١ و ١١٤ ورقم آية"
                    a !in 1..target.ayahCount ->
                        error = "سورة ${target.nameAr} فيها ${target.ayahCount.toArabicIndic()} آية"
                    else -> scope.launch {
                        val r = viewModel.resolveAyah(s, a)
                        if (r == null) error = "تعذّر العثور على الآية" else onOpenReader(r.first, r.second)
                    }
                }
            }) { Text("اذهب") }
        }
        if (error.isNotEmpty()) {
            Text(
                error,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
