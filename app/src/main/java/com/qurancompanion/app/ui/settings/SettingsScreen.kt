package com.qurancompanion.app.ui.settings

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qurancompanion.app.domain.model.ColorTheme
import com.qurancompanion.app.domain.model.ReminderSetting
import com.qurancompanion.app.domain.model.ReminderType
import com.qurancompanion.app.domain.model.ThemeMode
import com.qurancompanion.app.ui.common.toArabicDigits
import com.qurancompanion.app.ui.components.GlassCard
import com.qurancompanion.app.ui.components.ScreenHeader
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var permissionDenied by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionDenied = !granted
    }

    fun requestPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenHeader("الإعدادات والتذكيرات", onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Section("المظهر") {
                Text("الوضع", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = settings.themeMode == mode,
                            onClick = { viewModel.setThemeMode(mode) },
                            label = { Text(mode.label) },
                        )
                    }
                }
                Text(
                    "اللون",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp),
                )
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ColorTheme.entries.forEach { theme ->
                        FilterChip(
                            selected = settings.colorTheme == theme,
                            onClick = { viewModel.setColorTheme(theme) },
                            label = { Text(theme.label) },
                        )
                    }
                }
            }

            Section("التذكيرات") {
                ReminderType.entries.forEach { type ->
                    val setting = settings.reminders.getValue(type)
                    ReminderRow(
                        type = type,
                        setting = setting,
                        onToggle = { enabled ->
                            viewModel.setReminderEnabled(type, enabled)
                            if (enabled) requestPermissionIfNeeded()
                        },
                        onPickTime = {
                            TimePickerDialog(
                                context,
                                { _, hour, minute -> viewModel.setReminderTime(type, hour, minute) },
                                setting.hour,
                                setting.minute,
                                true,
                            ).show()
                        },
                    )
                }
                if (permissionDenied) {
                    Text(
                        "لم يُمنح إذن الإشعارات، فلن تصلك التذكيرات. يمكنك السماح به من إعدادات النظام للتطبيق.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Text(
                    "كل تذكير يصل مرة واحدة يوميًا على الأكثر، ولا يصل إن لم يكن هناك ما يُذكَّر به. " +
                        "تستمر التذكيرات بعد إعادة تشغيل الهاتف، وقد يتأخر وصولها قليلًا بحسب توفير البطارية؛ " +
                        "فإن لم تصلك فاستثنِ التطبيق من تحسين البطارية في إعدادات النظام.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            Section("حول") {
                Text(
                    "نص القرآن: النص العثماني من مشروع تنزيل (tanzil.net). يعمل التطبيق دون إنترنت ودون حساب، وبياناتك على جهازك فقط.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    GlassCard(Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
        content()
    }
}

@Composable
private fun ReminderRow(type: ReminderType, setting: ReminderSetting, onToggle: (Boolean) -> Unit, onPickTime: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(type.label, style = MaterialTheme.typography.titleMedium)
            Text(
                type.description,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onPickTime) {
                Text("الوقت: " + String.format(Locale.US, "%02d:%02d", setting.hour, setting.minute).toArabicDigits())
            }
        }
        Switch(checked = setting.enabled, onCheckedChange = onToggle)
    }
}
