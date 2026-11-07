package com.qurancompanion.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.qurancompanion.app.domain.model.Surah
import com.qurancompanion.app.ui.common.toArabicIndic

@Composable
fun SurahPickerDialog(surahs: List<Surah>, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(surahs, query) {
        val q = query.trim()
        surahs.filter { q.isEmpty() || it.nameAr.contains(q) || it.id.toString() == q }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("اختر السورة") },
        text = {
            if (surahs.isEmpty()) {
                Text("نص المصحف غير محمّل بعد. افتح تبويب المصحف للتحقق من الحالة.")
            } else {
                Column {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("ابحث باسم السورة أو رقمها") },
                    )
                    LazyColumn(Modifier.heightIn(max = 360.dp)) {
                        items(filtered, key = { it.id }) { surah ->
                            Text(
                                "${surah.id.toArabicIndic()}. ${surah.nameAr}",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.fillMaxWidth().clickable { onPick(surah.id) }.padding(vertical = 12.dp),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("إغلاق") } },
    )
}
