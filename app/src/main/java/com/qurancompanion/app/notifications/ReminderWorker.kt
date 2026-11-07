package com.qurancompanion.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.qurancompanion.app.QuranCompanionApp
import com.qurancompanion.app.domain.model.ReminderType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val type = inputData.getString(KEY_TYPE)
            ?.let { name -> runCatching { ReminderType.valueOf(name) }.getOrNull() }
            ?: return Result.failure()
        val container = (applicationContext as? QuranCompanionApp)?.container ?: return Result.failure()

        val setting = container.settingsRepository.settings.first().reminders[type] ?: return Result.success()
        if (!setting.enabled) return Result.success() // turned off meanwhile: the chain simply ends

        try {
            NotificationHelper.maybeNotify(applicationContext, type, container)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // A failed notification must not break tomorrow's reminder.
        }
        container.reminderScheduler.schedule(type, setting, replace = false)
        return Result.success()
    }

    companion object {
        const val KEY_TYPE = "reminder_type"
    }
}
