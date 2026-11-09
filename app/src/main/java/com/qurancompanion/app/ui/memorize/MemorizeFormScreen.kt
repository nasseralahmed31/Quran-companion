package com.qurancompanion.app.ui.memorize

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qurancompanion.app.domain.model.MasteryLevel
import com.qurancompanion.app.domain.model.MemorizationState
import com.qurancompanion.app.domain.util.EpochDay
import com.qurancompanion.app.ui.achievements.AchievementUnlockedDialog
import com.qurancompanion.app.ui.common.formatEpochDay
import com.qurancompanion.app.ui.common.toArabicIndic
import com.qurancompanion.app.ui.components.GlassCard
import com.qurancompanion.app.ui.components.NumberField
import com.qurancompanion.app.ui.components.ScreenHeader
import com.qurancompanion.app.ui.components.SurahPickerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemorizeFormScreen(entryId: Long, presetStart: Int, presetEnd: Int, onDone: () -> Unit) {
    val vm: MemorizeFormViewModel = viewModel(
        key = "memorize_form_${entryId}_${presetStart}_$presetEnd",
        factory = MemorizeFormViewModel.factory(entryId, presetStart, presetEnd),
    )
    val s by vm.state.collectAsStateWithLifecycle()
    var showPicker by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(s.finished, s.newAchievements) {
        if (s.finished && s.newAchievements.isEmpty()) onDone()
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
        ScreenHeader(if (s.isEdit) "تعديل سجل الحفظ" else "تسجيل حفظ جديد", onDone)

        when {
            !s.loaded -> CircularProgressIndicator(Modifier.padding(24.dp))
            s.notFound -> Text("هذا السجل لم يعد موجودًا.", modifier = Modifier.padding(16.dp))
            else -> Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (!s.isEdit && s.mode != FormMode.RANGE) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = s.mode == FormMode.SURAH, onClick = { vm.setMode(FormMode.SURAH) }, label = { Text("حسب السورة") })
                        FilterChip(selected = s.mode == FormMode.PAGES, onClick = { vm.setMode(FormMode.PAGES) }, label = { Text("حسب الصفحات") })
                    }
                }

                when (s.mode) {
                    FormMode.SURAH -> {
                        GlassCard(Modifier.fillMaxWidth(), onClick = { showPicker = true }) {
                            Text("السورة", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                s.selectedSurah?.let { "سورة ${it.nameAr}" } ?: "اختر السورة",
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NumberField(s.from, vm::onFrom, "من الآية", Modifier.weight(1f), maxLength = 3)
                            NumberField(s.to, vm::onTo, "إلى الآية", Modifier.weight(1f), maxLength = 3)
                        }
                        s.selectedSurah?.let {
                            Text(
                                "عدد آيات السورة: ${it.ayahCount.toArabicIndic()}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (s.overlapCount > 0) {
                            Text(
                                "${s.overlapCount.toArabicIndic()} من هذه الآيات مسجّلة من قبل، وسيُحدَّث وضعها بهذا السجل.",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    }
                    FormMode.PAGES -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NumberField(s.pageFrom, vm::onPageFrom, "من الصفحة", Modifier.weight(1f), maxLength = 3)
                            NumberField(s.pageTo, vm::onPageTo, "إلى الصفحة", Modifier.weight(1f), maxLength = 3)
                        }
                        RangeSummary(s.rangeSummary, "مثال: إن حفظت من الصفحة ١ إلى ١٧ فأدخل ١ و١٧، وإن عبر النطاق عدة سور فسيُسجَّل لكل سورة سجل مستقل.")
                    }
                    FormMode.RANGE -> RangeSummary(s.rangeSummary, "مقدار اليوم من خطة هدفك.")
                }

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("حفظ سابق (منذ زمن)", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "حفظته قبل استخدام التطبيق؛ يُحسب في نسبة حفظك لكنه لا يدخل في وتيرتك اليومية ولا سلسلة الالتزام ولا تقدير ختم الحفظ.",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = s.isPrevious, onCheckedChange = vm::onPrevious)
                }
                if (!s.isPrevious) {
                    Text("تاريخ الحفظ: ${formatEpochDay(s.epochDay)}", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { vm.shiftDay(-1) }) { Text("اليوم السابق") }
                        OutlinedButton(onClick = { vm.shiftDay(1) }, enabled = s.epochDay < EpochDay.today()) { Text("اليوم التالي") }
                    }
                }

                Text("حالة الحفظ", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MemorizationState.entries.forEach { st ->
                        FilterChip(selected = s.state == st, onClick = { vm.onState(st) }, label = { Text(st.label) })
                    }
                }

                if (s.state == MemorizationState.MEMORIZED) {
                    Text("مستوى الإتقان", style = MaterialTheme.typography.titleMedium)
                    MasteryLevel.selectableWhenMemorized.forEach { level ->
                        Row(
                            Modifier.fillMaxWidth().clickable { vm.onMastery(level) },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = s.mastery == level, onClick = { vm.onMastery(level) })
                            Text(level.label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }

                OutlinedTextField(
                    value = s.note,
                    onValueChange = vm::onNote,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    label = { Text("ملاحظات شخصية (اختياري)") },
                )

                s.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }

                Button(onClick = vm::save, enabled = !s.saving, modifier = Modifier.fillMaxWidth()) {
                    Text(if (s.isEdit) "حفظ التعديلات" else "حفظ السجل")
                }
                if (s.isEdit) {
                    OutlinedButton(
                        onClick = { confirmDelete = true },
                        enabled = !s.saving,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("حذف السجل", color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }

    if (showPicker) {
        SurahPickerDialog(
            surahs = s.surahs,
            onPick = {
                vm.onSurah(it)
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("حذف السجل؟") },
            text = { Text("سيُحذف هذا السجل وتُحدَّث حالة الآيات المرتبطة به وجدول مراجعته.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    vm.delete()
                }) { Text("حذف", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("إلغاء") } },
        )
    }
    if (s.finished && s.newAchievements.isNotEmpty()) {
        AchievementUnlockedDialog(s.newAchievements, onDismiss = onDone)
    }
}

@Composable
private fun RangeSummary(summary: String?, hint: String) {
    GlassCard(Modifier.fillMaxWidth()) {
        Text(summary ?: "أدخل النطاق ليظهر ملخصه هنا", style = MaterialTheme.typography.titleMedium)
        Text(
            hint,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
