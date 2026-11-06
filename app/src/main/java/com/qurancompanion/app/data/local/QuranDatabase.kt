package com.qurancompanion.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

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
    ],
    version = 2,
    exportSchema = false,
)
abstract class QuranDatabase : RoomDatabase() {
    abstract fun quranDao(): QuranDao
    abstract fun memorizationDao(): MemorizationDao

    companion object {
        fun create(context: Context): QuranDatabase =
            Room.databaseBuilder(context, QuranDatabase::class.java, "quran_companion.db")
                .fallbackToDestructiveMigrationFrom(1)
                .build()
    }
}
