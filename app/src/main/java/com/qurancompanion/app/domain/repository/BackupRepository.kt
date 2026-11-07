package com.qurancompanion.app.domain.repository

import com.qurancompanion.app.domain.model.BackupSummary

interface BackupRepository {
    /** All personal data (not the Quran text) as a JSON document. */
    suspend fun export(): String

    /**
     * Validates [json] first and only then replaces the current personal data with it.
     * Throws if the document is invalid, in which case nothing changes.
     */
    suspend fun import(json: String): BackupSummary
}
