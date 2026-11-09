package com.qurancompanion.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.qurancompanion.app.domain.model.AppSettings
import com.qurancompanion.app.domain.model.ColorTheme
import com.qurancompanion.app.domain.model.ReminderType
import com.qurancompanion.app.domain.model.ThemeMode
import com.qurancompanion.app.domain.repository.SettingsRepository
import com.qurancompanion.app.notifications.ReminderScheduler
import com.qurancompanion.app.ui.common.appContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val scheduler: ReminderScheduler,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun setColorTheme(theme: ColorTheme) {
        viewModelScope.launch { repository.setColorTheme(theme) }
    }

    fun setAutoShiftReviews(enabled: Boolean) {
        viewModelScope.launch { repository.setAutoShiftReviews(enabled) }
    }

    fun setReminderEnabled(type: ReminderType, enabled: Boolean) {
        viewModelScope.launch {
            val current = repository.settings.first().reminders.getValue(type)
            repository.setReminder(type, current.copy(enabled = enabled))
            scheduler.sync(repository.settings.first())
        }
    }

    fun setReminderTime(type: ReminderType, hour: Int, minute: Int) {
        viewModelScope.launch {
            val current = repository.settings.first().reminders.getValue(type)
            repository.setReminder(type, current.copy(hour = hour, minute = minute))
            scheduler.sync(repository.settings.first())
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = appContainer()
                SettingsViewModel(c.settingsRepository, c.reminderScheduler)
            }
        }
    }
}
