package com.qurancompanion.app.notifications

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.qurancompanion.app.domain.model.AppSettings
import com.qurancompanion.app.domain.model.ReminderSetting
import com.qurancompanion.app.domain.model.ReminderType
import com.qurancompanion.app.domain.util.ReminderTime
import java.util.concurrent.TimeUnit

/**
 * One unique work chain per reminder type. Each run posts at most one notification and then
 * schedules the next day's run, so the time of day never drifts. WorkManager keeps the queue
 * across restarts of the phone.
 */
class ReminderScheduler(private val context: Context) {
    private val workManager: WorkManager get() = WorkManager.getInstance(context)

    /** Makes the queued work match [settings]: enabled types are (re)scheduled, others cancelled. */
    fun sync(settings: AppSettings) {
        ReminderType.entries.forEach { type ->
            val setting = settings.reminders[type] ?: return@forEach
            if (setting.enabled) {
                schedule(type, setting, replace = true)
            } else {
                workManager.cancelUniqueWork(workName(type))
            }
        }
    }

    fun schedule(type: ReminderType, setting: ReminderSetting, replace: Boolean) {
        val delay = ReminderTime.delayUntil(setting.hour, setting.minute, System.currentTimeMillis())
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(ReminderWorker.KEY_TYPE to type.name))
            .build()
        workManager.enqueueUniqueWork(
            workName(type),
            if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.APPEND_OR_REPLACE,
            request,
        )
    }

    companion object {
        fun workName(type: ReminderType) = "reminder_${type.name}"
    }
}
