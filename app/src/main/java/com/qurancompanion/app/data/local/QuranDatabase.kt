package com.qurancompanion.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * No destructive-migration fallback on purpose: later phases add user-data tables
 * through explicit migrations so memorization data is never lost.
 */
@Database(
    entities = [SurahEntity::class, AyahEntity::class, JuzEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class QuranDatabase : RoomDatabase() {
    abstract fun quranDao(): QuranDao

    companion object {
        fun create(context: Context): QuranDatabase =
            Room.databaseBuilder(context, QuranDatabase::class.java, "quran_companion.db").build()
    }
}
