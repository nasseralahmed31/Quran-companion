
package com.qurancompanion.app.ui.review

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qurancompanion.app.domain.model.ReviewRating
import com.qurancompanion.app.ui.achievements.AchievementUnlockedDialog
import com.qurancompanion.app.ui.common.flowingAyahText
import com.qurancompanion.app.ui.common.formatEpochDay
import com.qurancompanion.app.ui.common.rangeLabel
import com.qurancompanion.app.ui.common.toArabicIndic
import com.qurancompanion.app.ui.components.GlassCard
import com.qurancompanion.app.ui.components.NumberField
import com.qurancompanion.app.ui.components.ScreenHeader
import com.qurancompanion.app.ui.theme.rememberQuranFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewSessionScreen(
    entryId: Long,
    onDone: () -> Unit,
    onEditEntry: (Long) -> Unit
) {
    val vm: ReviewSessionViewModel = viewModel(
        key = "review_session_$entryId",
        factory = ReviewSessionViewModel.factory(entryId),
    )

    val s by vm.state.collectAsStateWithLifecycle()
    val fontFamily = rememberQuranFontFamily()
    val markerColor = MaterialTheme.colorScheme.secondary

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
    ) {
        ScreenHeader("مراجعة مقطع", onDone)

        val item = s.item

        when {
            s.loading -> {
                CircularProgressIndicator(
                    Modifier.padding(24.dp)
                )
            }

            s.missing || item == null -> {
                Text(
                    "هذا المقطع لم يعد في جدول المراجعة.",
                    modifier = Modifier.padding(16.dp)
                )
            }

            else -> Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                GlassCard(Modifier.fillMaxWidth()) {
                    Text(
                        "سورة ${s.surahName}",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        rangeLabel(item.fromAyah, item.toAyah),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Text(
                        "اقرأ المقطع من حفظك أولًا، ثم اكشف النص للتحقق.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }

                OutlinedButton(
                    onClick = vm::reveal,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (s.revealed) "إخفاء النص" else "إظهار النص"
                    )
                }

                if (s.revealed) {
                    Text(
                        text = flowingAyahText(s.ayahs, markerColor),
                        fontSize = 24.sp,
                        lineHeight = 46.sp,
                        fontFamily = fontFamily,
                        textAlign = TextAlign.Justify,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (s.outcome == null) {
                    Text(
                        "كيف كان أداؤك؟",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ReviewRating.entries.forEach { r ->
                            FilterChip(
                                selected = s.rating == r,
                                onClick = { vm.setRating(r) },
                                label = { Text(r.label) }
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "عدد الأخطاء",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(
                            onClick = { vm.changeErrors(-1) }
                        ) {
                            Icon(
                                Icons.Rounded.Remove,
                                contentDescription = "إنقاص"
                            )
                        }

                        Text(
                            s.errors.toArabicIndic(),
                            style = MaterialTheme.typography.titleMedium
                        )

                        IconButton(
                            onClick = { vm.changeErrors(1) }
                        ) {
                            Icon(
                                Icons.Rounded.Add,
                                contentDescription = "زيادة"
                            )
                        }
                    }

                    OutlinedTextField(
                        value = s.note,
                        onValueChange = vm::setNote,
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        label = { Text("ملاحظات (اختياري)") },
                    )

                    NumberField(
                        s.manualDays,
                        vm::setManualDays,
                        "تحديد موعد المراجعة القادمة يدويًا: بعد كم يوم؟ (اختياري)",
                        Modifier.fillMaxWidth(),
                        maxLength = 3,
                    )

                    s.error?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Button(
                        onClick = vm::save,
                        enabled = !s.saving,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("حفظ المراجعة")
                    }

                } else {
                    // Explicit null check: outcome is now safely non-null.
                    val outcome = s.outcome
<<<<<<< HEAD

                    if (outcome != null) {
                        GlassCard(Modifier.fillMaxWidth()) {
=======
                    GlassCard(Modifier.fillMaxWidth()) {
                        Text("تم تسجيل المراجعة", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "المراجعة القادمة: ${formatEpochDay(outcome.nextReviewDay)} (بعد ${outcome.intervalDays.toArabicIndic()} يوم)",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        Text(
                            "مستوى الإتقان الآن: ${outcome.newMastery.label}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        outcome.adaptationNote?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                    if (outcome.suggestRememorize) {
                        GlassCard(Modifier.fillMaxWidth(), borderColor = MaterialTheme.colorScheme.error) {
                            Text("يُنصح بإعادة حفظ هذا المقطع", style = MaterialTheme.typography.titleMedium)
>>>>>>> ce11b72 (v3)
                            Text(
                                "تم تسجيل المراجعة",
                                style = MaterialTheme.typography.titleMedium
                            )

                            Text(
                                "المراجعة القادمة: ${formatEpochDay(outcome.nextReviewDay)} (بعد ${outcome.intervalDays.toArabicIndic()} يوم)",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 4.dp),
                            )

                            Text(
                                "مستوى الإتقان الآن: ${outcome.newMastery.label}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        if (outcome.suggestRememorize) {
                            GlassCard(
                                Modifier.fillMaxWidth(),
                                borderColor = MaterialTheme.colorScheme.error
                            ) {
                                Text(
                                    "يُنصح بإعادة حفظ هذا المقطع",
                                    style = MaterialTheme.typography.titleMedium
                                )

                                Text(
                                    "تكرر الضعف فيه. يمكنك تعديل السجل وإعادته إلى «قيد الحفظ».",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )

                                OutlinedButton(
                                    onClick = { onEditEntry(entryId) },
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Text("فتح السجل للتعديل")
                                }
                            }
                        }

                        Button(
                            onClick = onDone,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("تم")
                        }
                    }
                }
            }
        }
    }

    val achievements = s.outcome?.newAchievements.orEmpty()

    if (achievements.isNotEmpty()) {
        AchievementUnlockedDialog(
            achievements,
            onDismiss = onDone
        )
    }
}
