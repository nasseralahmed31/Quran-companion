package com.qurancompanion.app.ui.backup

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.qurancompanion.app.QuranCompanionApp
import com.qurancompanion.app.data.backup.BackupFormatException
import com.qurancompanion.app.domain.repository.BackupRepository
import com.qurancompanion.app.domain.repository.SettingsRepository
import com.qurancompanion.app.notifications.ReminderScheduler
import com.qurancompanion.app.ui.common.appContainer
import com.qurancompanion.app.ui.common.toArabicIndic
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class BackupUiState(
    val busy: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false,
)

class BackupViewModel(
    private val resolver: ContentResolver,
    private val backup: BackupRepository,
    private val settings: SettingsRepository,
    private val scheduler: ReminderScheduler,
) : ViewModel() {

    private val _state = MutableStateFlow(BackupUiState())
    val state: StateFlow<BackupUiState> = _state.asStateFlow()

    fun suggestedFileName(): String =
        "quran-companion-backup-" + SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) + ".json"

    fun export(uri: Uri) {
        viewModelScope.launch {
            _state.update { BackupUiState(busy = true) }
            try {
                val json = backup.export()
                withContext(Dispatchers.IO) {
                    val out = resolver.openOutputStream(uri, "wt") ?: throw IOException("تعذّر فتح الملف للكتابة")
                    out.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                }
                _state.update { BackupUiState(message = "تم حفظ النسخة الاحتياطية بنجاح.") }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { BackupUiState(message = "تعذّر حفظ النسخة: ${e.message}", isError = true) }
            }
        }
    }

    fun import(uri: Uri) {
        viewModelScope.launch {
            _state.update { BackupUiState(busy = true) }
            try {
                val text = withContext(Dispatchers.IO) {
                    val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: throw IOException("تعذّر فتح الملف للقراءة")
                    if (bytes.size > MAX_BYTES) throw BackupFormatException("الملف أكبر من أن يكون نسخة احتياطية")
                    bytes.toString(Charsets.UTF_8)
                }
                val summary = backup.import(text)
                scheduler.sync(settings.settings.first())
                _state.update {
                    BackupUiState(
                        message = "تمت الاستعادة: ${summary.entries.toArabicIndic()} سجل حفظ، " +
                            "${summary.reviewSessions.toArabicIndic()} جلسة مراجعة، ${summary.goals.toArabicIndic()} هدف.",
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: BackupFormatException) {
                _state.update { BackupUiState(message = "${e.message}. لم يتغير شيء في بياناتك.", isError = true) }
            } catch (e: Exception) {
                _state.update { BackupUiState(message = "تعذّرت الاستعادة: ${e.message}", isError = true) }
            }
        }
    }

    companion object {
        private const val MAX_BYTES = 20_000_000

        val Factory = viewModelFactory {
            initializer {
                val app = this[androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as QuranCompanionApp
                val c = appContainer()
                BackupViewModel(app.contentResolver, c.backupRepository, c.settingsRepository, c.reminderScheduler)
            }
        }
    }
}
