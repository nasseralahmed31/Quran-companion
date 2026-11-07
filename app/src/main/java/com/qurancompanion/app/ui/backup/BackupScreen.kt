package com.qurancompanion.app.ui.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qurancompanion.app.ui.components.GlassCard
import com.qurancompanion.app.ui.components.ScreenHeader

@Composable
fun BackupScreen(
    onBack: () -> Unit,
    viewModel: BackupViewModel = viewModel(factory = BackupViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var pendingImport by remember { mutableStateOf<Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) viewModel.export(uri)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pendingImport = uri
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenHeader("النسخ الاحتياطي والاستعادة", onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            GlassCard(Modifier.fillMaxWidth()) {
                Text("تصدير نسخة احتياطية", style = MaterialTheme.typography.titleMedium)
                Text(
                    "يحفظ سجلات حفظك ومراجعاتك واختباراتك وأهدافك وإنجازاتك وإعداداتك في ملف JSON تختار مكانه. " +
                        "لا يتضمن نص القرآن، فهو مضمَّن في التطبيق.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                Button(
                    onClick = { exportLauncher.launch(viewModel.suggestedFileName()) },
                    enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("تصدير الآن") }
            }

            GlassCard(Modifier.fillMaxWidth()) {
                Text("استعادة من نسخة احتياطية", style = MaterialTheme.typography.titleMedium)
                Text(
                    "تستبدل الاستعادة بياناتك الحالية بما في الملف. يُتحقق من الملف أولًا، وإن لم يكن صالحًا فلا يتغير شيء.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("*/*")) },
                    enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("اختيار ملف للاستعادة") }
            }

            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            state.message?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                "ملف النسخة غير مشفّر ويحتوي ملاحظاتك الشخصية، فاحفظه في مكان تثق به.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    pendingImport?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("استبدال بياناتك الحالية؟") },
            text = { Text("ستحل بيانات الملف المختار محل ما في التطبيق الآن. لا يمكن التراجع، فصدّر نسخة من وضعك الحالي أولًا إن لزم.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingImport = null
                    viewModel.import(uri)
                }) { Text("استعادة") }
            },
            dismissButton = { TextButton(onClick = { pendingImport = null }) { Text("إلغاء") } },
        )
    }
}
