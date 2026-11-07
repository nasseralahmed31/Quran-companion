package com.qurancompanion.app.ui.goals

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qurancompanion.app.domain.model.GoalUnit
import com.qurancompanion.app.ui.common.toArabicIndic
import com.qurancompanion.app.ui.components.GlassCard
import com.qurancompanion.app.ui.components.NumberField
import com.qurancompanion.app.ui.components.ScreenHeader
import com.qurancompanion.app.ui.components.SurahPickerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalFormScreen(onDone: () -> Unit, vm: GoalFormViewModel = viewModel(factory = GoalFormViewModel.Factory)) {
    val s by vm.state.collectAsStateWithLifecycle()
    var showPicker by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(s.saved) { if (s.saved) onDone() }

    Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
        ScreenHeader("هدف جديد", onDone)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("نوع الهدف", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GoalPreset.entries.forEach { p ->
                    FilterChip(selected = s.preset == p, onClick = { vm.setPreset(p) }, label = { Text(p.label) })
                }
            }

            OutlinedTextField(
                value = s.name,
                onValueChange = vm::setName,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("اسم الهدف (اختياري)") },
            )

            when (s.preset) {
                GoalPreset.SURAH -> GlassCard(Modifier.fillMaxWidth(), onClick = { showPicker = true }) {
                    Text("السورة", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(s.selectedSurah?.let { "سورة ${it.nameAr}" } ?: "اختر السورة", style = MaterialTheme.typography.titleMedium)
                }
                GoalPreset.JUZ -> NumberField(s.juzNumber, vm::setJuz, "رقم الجزء (١–٣٠)", Modifier.fillMaxWidth(), maxLength = 2)
                GoalPreset.PAGES -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(s.pageFrom, vm::setPageFrom, "من الصفحة", Modifier.weight(1f), maxLength = 3)
                    NumberField(s.pageTo, vm::setPageTo, "إلى الصفحة", Modifier.weight(1f), maxLength = 3)
                }
                GoalPreset.KHATM -> Text(
                    "ختم حفظ القرآن كاملًا (٦٢٣٦ آية). اختر المدة بالأيام.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                GoalPreset.DAILY -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GoalUnit.entries.forEach { u ->
                            FilterChip(selected = s.unit == u, onClick = { vm.setUnit(u) }, label = { Text(u.label) })
                        }
                    }
                    NumberField(s.dailyAmount, vm::setDailyAmount, "المقدار اليومي", Modifier.fillMaxWidth(), maxLength = 4)
                }
            }

            if (s.preset != GoalPreset.DAILY) {
                NumberField(s.days, vm::setDays, "المدة بالأيام", Modifier.fillMaxWidth(), maxLength = 4)
                val choices = if (s.preset == GoalPreset.KHATM) listOf(90, 180, 365, 730) else listOf(7, 14, 30, 60, 90)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    choices.forEach { d ->
                        FilterChip(
                            selected = s.days == d.toString(),
                            onClick = { vm.setDays(d.toString()) },
                            label = { Text("${d.toArabicIndic()} يوم") },
                        )
                    }
                }
                s.preview?.let {
                    GlassCard(Modifier.fillMaxWidth()) {
                        Text(it, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            s.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = vm::save, enabled = !s.saving, modifier = Modifier.fillMaxWidth()) { Text("إنشاء الهدف") }
        }
    }

    if (showPicker) {
        SurahPickerDialog(
            surahs = s.surahs,
            onPick = {
                vm.setSurah(it)
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }
}
