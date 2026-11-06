package com.qurancompanion.app.domain.repository

import com.qurancompanion.app.domain.model.ProgressSummary
import kotlinx.coroutines.flow.Flow

interface ProgressRepository {
    fun observeSummary(): Flow<ProgressSummary>
}
