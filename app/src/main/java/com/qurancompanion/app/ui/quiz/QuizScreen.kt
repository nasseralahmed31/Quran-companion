package com.qurancompanion.app.ui.quiz

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qurancompanion.app.domain.model.QuizQuestion
import com.qurancompanion.app.domain.model.QuizType
import com.qurancompanion.app.domain.model.ReviewRating
import com.qurancompanion.app.ui.achievements.AchievementUnlockedDialog
import com.qurancompanion.app.ui.common.toArabicIndic
import com.qurancompanion.app.ui.components.GlassCard
import com.qurancompanion.app.ui.components.NumberField
import com.qurancompanion.app.ui.components.ScreenHeader
import com.qurancompanion.app.ui.theme.rememberQuranFontFamily

@Composable
fun QuizScreen(initialType: QuizType?, onBack: () -> Unit) {
    val vm: QuizViewModel = viewModel(
        key = "quiz_${initialType?.name.orEmpty()}",
        factory = QuizViewModel.factory(initialType),
    )
    val s by vm.state.collectAsStateWithLifecycle()
    val names by vm.surahNames.collectAsStateWithLifecycle()
    val fontFamily = rememberQuranFontFamily()

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenHeader("اختبار الحفظ", onBack)
        when (s.phase) {
            QuizPhase.SETUP -> SetupView(s, vm)
            QuizPhase.LOADING -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            QuizPhase.ACTIVE -> {
                val q = s.current
                if (q != null) QuestionView(s, q, names[q.surahId].orEmpty(), vm, fontFamily)
            }
            QuizPhase.RESULT -> ResultView(s, vm, onBack)
        }
    }

    val achievements = s.finish?.newAchievements.orEmpty()
    if (achievements.isNotEmpty()) {
        AchievementUnlockedDialog(achievements, onDismiss = { vm.restart() })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SetupView(s: QuizUiState, vm: QuizViewModel) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("نوع الاختبار", style = MaterialTheme.typography.titleMedium)
        QuizType.entries.forEach { type ->
            GlassCard(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.selectable(selected = s.type == type, role = Role.RadioButton, onClick = { vm.setType(type) }),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = s.type == type, onClick = null)
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(type.label, style = MaterialTheme.typography.titleMedium)
                        Text(
                            type.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        Text("نطاق السور (من آيات محفوظك فقط)", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField(s.fromSurah, vm::setFrom, "من السورة", Modifier.weight(1f), maxLength = 3)
            NumberField(s.toSurah, vm::setTo, "إلى السورة", Modifier.weight(1f), maxLength = 3)
        }
        Text("عدد الأسئلة", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(5, 10, 20).forEach { n ->
                FilterChip(selected = s.count == n, onClick = { vm.setCount(n) }, label = { Text(n.toArabicIndic()) })
            }
        }
        s.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
        Button(onClick = vm::start, modifier = Modifier.fillMaxWidth()) { Text("ابدأ الاختبار") }
        Text(
            "ملاحظة: التطبيق لا يستمع إلى تلاوتك ولا يقيّمها صوتيًا؛ التقييم في التسميع والإكمال بيدك، " +
                "وأما اختيار الآية التالية والمتشابهات فيُصحَّح تلقائيًا.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun QuestionView(s: QuizUiState, q: QuizQuestion, surahName: String, vm: QuizViewModel, fontFamily: FontFamily?) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        LinearProgressIndicator(
            progress = { (s.index + 1) / s.questions.size.toFloat() },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "السؤال ${(s.index + 1).toArabicIndic()} من ${s.questions.size.toArabicIndic()} • ${q.type.label}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        when (q.type) {
            QuizType.RECITE -> {
                GlassCard(Modifier.fillMaxWidth()) {
                    Text("سورة $surahName", style = MaterialTheme.typography.titleLarge)
                    Text("الآية ${q.ayahNumber.toArabicIndic()}", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "سمِّع الآية من حفظك ثم اكشف النص.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                SelfGradedArea(s, q.answer, vm, fontFamily)
            }
            QuizType.COMPLETE -> {
                Text("أكمل الآية من سورة $surahName:", style = MaterialTheme.typography.titleMedium)
                QuranText(q.prompt + " …", fontFamily)
                SelfGradedArea(s, q.answer, vm, fontFamily)
            }
            QuizType.NEXT_AYAH -> {
                Text("سورة $surahName • الآية ${q.ayahNumber.toArabicIndic()}", style = MaterialTheme.typography.titleMedium)
                QuranText(q.prompt, fontFamily)
                Text("ما الآية التي تليها؟", style = MaterialTheme.typography.titleMedium)
                OptionsList(q, s.chosen, vm, fontFamily)
                NextButton(s, vm)
            }
            QuizType.SIMILAR -> {
                Text("أكمل الآية (من سورة $surahName):", style = MaterialTheme.typography.titleMedium)
                QuranText(q.prompt + " …", fontFamily)
                OptionsList(q, s.chosen, vm, fontFamily)
                NextButton(s, vm)
            }
        }
    }
}

@Composable
private fun QuranText(text: String, fontFamily: FontFamily?) {
    Text(
        text = text,
        fontSize = 24.sp,
        lineHeight = 44.sp,
        fontFamily = fontFamily,
        textAlign = TextAlign.Justify,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SelfGradedArea(s: QuizUiState, answer: String, vm: QuizViewModel, fontFamily: FontFamily?) {
    if (!s.revealed) {
        OutlinedButton(onClick = vm::reveal, modifier = Modifier.fillMaxWidth()) { Text("إظهار النص") }
    } else {
        QuranText(answer, fontFamily)
        Text("هل أصبت؟", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { vm.grade(true) }, modifier = Modifier.weight(1f)) { Text("أصبت") }
            OutlinedButton(onClick = { vm.grade(false) }, modifier = Modifier.weight(1f)) { Text("أخطأت") }
        }
    }
}

@Composable
private fun OptionsList(q: QuizQuestion, chosen: Int, vm: QuizViewModel, fontFamily: FontFamily?) {
    val good = Color(0xFF22C55E)
    val bad = MaterialTheme.colorScheme.error
    q.options.forEachIndexed { i, text ->
        val answered = chosen != -1
        val accent = when {
            !answered -> MaterialTheme.colorScheme.outline
            i == q.correctIndex -> good
            i == chosen -> bad
            else -> MaterialTheme.colorScheme.outline
        }
        Surface(
            onClick = { vm.choose(i) },
            enabled = !answered,
            shape = RoundedCornerShape(16.dp),
            color = if (answered && (i == q.correctIndex || i == chosen)) accent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            border = BorderStroke(1.dp, accent),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text,
                fontSize = 20.sp,
                lineHeight = 36.sp,
                fontFamily = fontFamily,
                modifier = Modifier.padding(14.dp),
            )
        }
    }
}

@Composable
private fun NextButton(s: QuizUiState, vm: QuizViewModel) {
    if (s.chosen != -1) {
        val last = s.index + 1 >= s.questions.size
        Button(onClick = vm::next, modifier = Modifier.fillMaxWidth()) { Text(if (last) "عرض النتيجة" else "التالي") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResultView(s: QuizUiState, vm: QuizViewModel, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        GlassCard(Modifier.fillMaxWidth()) {
            Text("نتيجتك", style = MaterialTheme.typography.titleMedium)
            Text(
                "${s.correctCount.toArabicIndic()} / ${s.results.size.toArabicIndic()}",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text("قيّم أداءك (اقتُرح التقييم من نتيجتك ويمكنك تعديله)", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReviewRating.entries.forEach { r ->
                FilterChip(selected = s.rating == r, onClick = { vm.setRating(r) }, label = { Text(r.label) })
            }
        }
        val finish = s.finish
        if (finish == null) {
            Button(onClick = vm::apply, enabled = !s.finishing, modifier = Modifier.fillMaxWidth()) {
                Text("تطبيق النتيجة على الإتقان وجدول المراجعة")
            }
            s.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        } else {
            GlassCard(Modifier.fillMaxWidth()) {
                Text(
                    "حُدِّث ${finish.updatedPassages.toArabicIndic()} مقطع في جدول المراجعة.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = vm::restart, modifier = Modifier.weight(1f)) { Text("اختبار جديد") }
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("خروج") }
        }
    }
}
