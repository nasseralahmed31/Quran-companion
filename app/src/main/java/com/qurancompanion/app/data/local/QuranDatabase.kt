package com.qurancompanion.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.qurancompanion.app.domain.util.EpochDay

/**
 * Version 1 (phase 2) held only the re-importable Quran text, so upgrading from it may rebuild
 * the database. From version 2 on, user data lives here: every later change MUST ship an explicit
 * Migration — never add a destructive fallback for versions >= 2.
 */
@Database(
    entities = [
        SurahEntity::class,
        AyahEntity::class,
        JuzEntity::class,
        MemorizationEntryEntity::class,
        AyahProgressEntity::class,
        ActivityEntity::class,
        AchievementEntity::class,
        ReviewScheduleEntity::class,
        ReviewSessionEntity::class,
        GoalEntity::class,
        SimilarPairEntity::class,
        ConfusionEntity::class,
        QuizResultEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class QuranDatabase : RoomDatabase() {
    abstract fun quranDao(): QuranDao
    abstract fun memorizationDao(): MemorizationDao
    abstract fun reviewDao(): ReviewDao
    abstract fun planningDao(): PlanningDao
    abstract fun insightsDao(): InsightsDao
    abstract fun statsDao(): StatsDao
    abstract fun backupDao(): BackupDao

    companion object {
        /** Adds review scheduling, goals, similar-ayah data, confusions and quiz history. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `review_schedules` (`entryId` INTEGER NOT NULL, " +
                        "`nextReviewDay` INTEGER NOT NULL, `intervalDays` INTEGER NOT NULL, `ease` REAL NOT NULL, " +
                        "`repetitions` INTEGER NOT NULL, `lapses` INTEGER NOT NULL, `lastReviewDay` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`entryId`))",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_review_schedules_nextReviewDay` ON `review_schedules` (`nextReviewDay`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `review_sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`entryId` INTEGER NOT NULL, `surahId` INTEGER NOT NULL, `fromAyah` INTEGER NOT NULL, " +
                        "`toAyah` INTEGER NOT NULL, `reviewedOn` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, " +
                        "`rating` TEXT NOT NULL, `errorCount` INTEGER NOT NULL, `note` TEXT NOT NULL, " +
                        "`nextReviewDay` INTEGER NOT NULL, `source` TEXT NOT NULL)",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_review_sessions_reviewedOn` ON `review_sessions` (`reviewedOn`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `goals` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, `kind` TEXT NOT NULL, `startAyahId` INTEGER NOT NULL, " +
                        "`endAyahId` INTEGER NOT NULL, `startDay` INTEGER NOT NULL, `endDay` INTEGER NOT NULL, " +
                        "`dailyAmount` INTEGER NOT NULL, `unit` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `similar_pairs` (`ayahA` INTEGER NOT NULL, `ayahB` INTEGER NOT NULL, " +
                        "`score` REAL NOT NULL, PRIMARY KEY(`ayahA`, `ayahB`))",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_similar_pairs_ayahB` ON `similar_pairs` (`ayahB`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `confusions` (`ayahA` INTEGER NOT NULL, `ayahB` INTEGER NOT NULL, " +
                        "`times` INTEGER NOT NULL, `lastAt` INTEGER NOT NULL, PRIMARY KEY(`ayahA`, `ayahB`))",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `quiz_results` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`timestamp` INTEGER NOT NULL, `day` INTEGER NOT NULL, `type` TEXT NOT NULL, " +
                        "`total` INTEGER NOT NULL, `correct` INTEGER NOT NULL, `rating` TEXT NOT NULL)",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_quiz_results_day` ON `quiz_results` (`day`)")

                // Records memorized in phase 3 enter the review schedule: weak ones first, confident ones later.
                val today = EpochDay.today()
                db.execSQL(
                    "INSERT INTO review_schedules (entryId, nextReviewDay, intervalDays, ease, repetitions, lapses, lastReviewDay) " +
                        "SELECT id, CASE " +
                        "WHEN mastery IN ('WEAK', 'NEEDS_REVIEW') THEN MAX(memorizedOn + 1, $today) " +
                        "WHEN mastery = 'MASTERED' THEN MAX(memorizedOn + 4, $today + 2) " +
                        "ELSE MAX(memorizedOn + 2, $today + 1) END, 1, 2.3, 0, 0, 0 " +
                        "FROM memorization_entries WHERE state = 'MEMORIZED'",
                )
            }
        }

        fun create(context: Context): QuranDatabase =
            Room.databaseBuilder(context, QuranDatabase::class.java, "quran_companion.db")
                .addMigrations(MIGRATION_2_3)
                .fallbackToDestructiveMigrationFrom(1)
                .build()
    }
}
