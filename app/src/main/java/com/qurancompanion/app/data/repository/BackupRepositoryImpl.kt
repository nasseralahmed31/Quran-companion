package com.qurancompanion.app.data.repository

import androidx.room.withTransaction
import com.qurancompanion.app.data.backup.BackupCodec
import com.qurancompanion.app.data.backup.BackupData
import com.qurancompanion.app.data.backup.BackupReminder
import com.qurancompanion.app.data.backup.BackupSettings
import com.qurancompanion.app.data.local.QuranDatabase
import com.qurancompanion.app.domain.model.AppSettings
import com.qurancompanion.app.domain.model.BackupSummary
import com.qurancompanion.app.domain.model.ColorTheme
import com.qurancompanion.app.domain.model.QuranConstants
import com.qurancompanion.app.domain.model.ReminderSetting
import com.qurancompanion.app.domain.model.ReminderType
import com.qurancompanion.app.domain.model.ThemeMode
import com.qurancompanion.app.domain.repository.BackupRepository
import com.qurancompanion.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first

class BackupRepositoryImpl(
    private val db: QuranDatabase,
    private val settings: SettingsRepository,
    private val updater: AyahProgressUpdater,
) : BackupRepository {
    private val dao = db.backupDao()

    override suspend fun export(): String {
        val s = settings.settings.first()
        val data = BackupData(
            createdAt = System.currentTimeMillis(),
            settings = BackupSettings(
                dailyTarget = s.dailyTargetAyahs,
                themeMode = s.themeMode.name,
                colorTheme = s.colorTheme.name,
                reminders = s.reminders.map { (type, r) -> BackupReminder(type.name, r.enabled, r.hour, r.minute) },
                lastReadPage = s.lastReadPage,
                bookmarks = s.bookmarks.sorted(),
                wirdReadPages = s.wirdReadPages,
                wirdReviewAyahs = s.wirdReviewAyahs,
                khatmStartDay = s.khatmStartDay,
                khatmDays = s.khatmDays,
            ),
            entries = dao.entries(),
            schedules = dao.schedules(),
            sessions = dao.sessions(),
            goals = dao.goals(),
            confusions = dao.confusions(),
            quizResults = dao.quizResults(),
            achievements = dao.achievements(),
            activity = dao.activity(),
            readingLog = dao.readingLog(),
        )
        return BackupCodec.encode(data)
    }

    override suspend fun import(json: String): BackupSummary {
        val data = BackupCodec.decode(json) // throws before anything is touched if invalid
        db.withTransaction {
            dao.deleteProgress()
            dao.deleteActivity()
            dao.deleteReadingLog()
            dao.deleteAchievements()
            dao.deleteQuizResults()
            dao.deleteConfusions()
            dao.deleteGoals()
            dao.deleteSessions()
            dao.deleteSchedules()
            dao.deleteEntries()

            dao.insertEntries(data.entries)
            dao.insertSchedules(data.schedules)
            dao.insertSessions(data.sessions)
            dao.insertGoals(data.goals)
            dao.insertConfusions(data.confusions)
            dao.insertQuizResults(data.quizResults)
            dao.insertAchievements(data.achievements)
            dao.insertActivity(data.activity)
            dao.insertReadingLog(data.readingLog)

            // Per-ayah progress is derived: rebuild it from the restored records.
            data.entries.forEach { updater.recomputeRange(it.surahId, it.fromAyah, it.toAyah) }
        }
        settings.restore(data.settings.toAppSettings())
        return BackupSummary(data.entries.size, data.sessions.size, data.goals.size)
    }
}

private fun BackupSettings.toAppSettings(): AppSettings {
    val defaults = AppSettings()
    return AppSettings(
        themeMode = runCatching { ThemeMode.valueOf(themeMode) }.getOrDefault(ThemeMode.SYSTEM),
        colorTheme = runCatching { ColorTheme.valueOf(colorTheme) }.getOrDefault(ColorTheme.EMERALD),
        dailyTargetAyahs = dailyTarget.coerceIn(0, QuranConstants.TOTAL_AYAHS),
        lastReadPage = lastReadPage.coerceIn(0, QuranConstants.TOTAL_PAGES),
        bookmarks = bookmarks.filter { it in 1..QuranConstants.TOTAL_PAGES }.toSet(),
        wirdReadPages = wirdReadPages.coerceIn(0, QuranConstants.TOTAL_PAGES),
        wirdReviewAyahs = wirdReviewAyahs.coerceIn(0, QuranConstants.TOTAL_AYAHS),
        khatmStartDay = khatmStartDay.coerceAtLeast(0L),
        khatmDays = khatmDays.coerceIn(0, 1000),
        reminders = ReminderType.entries.associateWith { type ->
            val saved = reminders.firstOrNull { it.type == type.name }
            if (saved == null) {
                defaults.reminders.getValue(type)
            } else {
                ReminderSetting(saved.enabled, saved.hour.coerceIn(0, 23), saved.minute.coerceIn(0, 59))
            }
        },
    )
}
