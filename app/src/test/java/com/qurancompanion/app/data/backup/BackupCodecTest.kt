package com.qurancompanion.app.data.backup

import com.qurancompanion.app.data.local.AchievementEntity
import com.qurancompanion.app.data.local.ActivityEntity
import com.qurancompanion.app.data.local.ConfusionEntity
import com.qurancompanion.app.data.local.GoalEntity
import com.qurancompanion.app.data.local.MemorizationEntryEntity
import com.qurancompanion.app.data.local.QuizResultEntity
import com.qurancompanion.app.data.local.ReviewScheduleEntity
import com.qurancompanion.app.data.local.ReviewSessionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCodecTest {
    private fun sample() = BackupData(
        createdAt = 123L,
        settings = BackupSettings(
            dailyTarget = 5, themeMode = "DARK", colorTheme = "SAPPHIRE",
            reminders = listOf(BackupReminder("REVIEW", true, 19, 30)),
        ),
        entries = listOf(
            MemorizationEntryEntity(1, 12, 1, 20, 20000L, "MEMORIZED", "GOOD", "ملاحظة شخصية", 1L, 2L),
            MemorizationEntryEntity(2, 18, 1, 10, 20001L, "IN_PROGRESS", "IN_PROGRESS", "", 3L, 4L),
        ),
        schedules = listOf(ReviewScheduleEntity(1, 20003L, 3, 2.3, 1, 0, 20000L)),
        sessions = listOf(
            ReviewSessionEntity(1, 1, 12, 1, 20, 20003L, 3L, "GOOD", 1, "", 20008L, "REVIEW"),
        ),
        goals = listOf(GoalEntity(1, "هدف", "RANGE", 1, 100, 1L, 30L, 0, "AYAHS", 5L)),
        confusions = listOf(ConfusionEntity(10, 20, 3, 6L)),
        quizResults = listOf(QuizResultEntity(1, 7L, 20001L, "SIMILAR", 10, 8, "GOOD")),
        achievements = listOf(AchievementEntity("first_ayah", 8L)),
        activity = listOf(
            ActivityEntity(1, "ADDED", 20000L, 9L, 12, 1, 20, "جيد"),
            ActivityEntity(2, "DELETED", 20001L, 10L, null, null, null, ""),
        ),
    )

    private fun failsWith(block: () -> Unit): Boolean = try {
        block()
        false
    } catch (e: BackupFormatException) {
        true
    }

    @Test
    fun encodeThenDecodeGivesTheSameData() {
        val data = sample()
        assertEquals(data, BackupCodec.decode(BackupCodec.encode(data)))
    }

    @Test
    fun aFileThatIsNotABackupIsRejected() {
        assertTrue(failsWith { BackupCodec.decode("not json at all") })
        assertTrue(failsWith { BackupCodec.decode("""{"format":"something-else","version":1}""") })
    }

    @Test
    fun aNewerVersionIsRejected() {
        val json = BackupCodec.encode(sample()).replace("\"version\":1", "\"version\":99")
        assertTrue(failsWith { BackupCodec.decode(json) })
    }

    @Test
    fun anInvalidRecordIsRejectedBeforeAnythingIsImported() {
        val bad = sample().copy(entries = listOf(sample().entries[0].copy(surahId = 200)))
        assertTrue(failsWith { BackupCodec.decode(BackupCodec.encode(bad)) })

        val reversed = sample().copy(entries = listOf(sample().entries[0].copy(fromAyah = 9, toAyah = 3)))
        assertTrue(failsWith { BackupCodec.decode(BackupCodec.encode(reversed)) })

        val orphanSchedule = sample().copy(schedules = listOf(sample().schedules[0].copy(entryId = 99)))
        assertTrue(failsWith { BackupCodec.decode(BackupCodec.encode(orphanSchedule)) })
    }

    @Test
    fun readingLogAndWirdSettingsSurviveTheRoundTrip() {
        val data = sample().copy(
            settings = sample().settings.copy(
                lastReadPage = 77, bookmarks = listOf(3, 9),
                wirdReadPages = 5, wirdReviewAyahs = 20, khatmStartDay = 20000L, khatmDays = 30,
            ),
            readingLog = listOf(
                com.qurancompanion.app.data.local.ReadingLogEntity(1, 20000L),
                com.qurancompanion.app.data.local.ReadingLogEntity(2, 20001L),
            ),
        )
        assertEquals(data, BackupCodec.decode(BackupCodec.encode(data)))
    }

    @Test
    fun aReadingLogWithAnInvalidPageIsRejected() {
        val bad = sample().copy(readingLog = listOf(com.qurancompanion.app.data.local.ReadingLogEntity(999, 1L)))
        assertTrue(failsWith { BackupCodec.decode(BackupCodec.encode(bad)) })
    }
}
