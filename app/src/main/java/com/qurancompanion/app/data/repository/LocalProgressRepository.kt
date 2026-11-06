package com.qurancompanion.app.data.repository

import com.qurancompanion.app.domain.model.ProgressSummary
import com.qurancompanion.app.domain.repository.ProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Phase 1: no memorization data exists yet, so the real (empty) state is reported.
 * Phase 2/3 replaces this with a Room-backed implementation computed from stored records.
 */
class LocalProgressRepository : ProgressRepository {
    private val state = MutableStateFlow(ProgressSummary())
    override fun observeSummary(): Flow<ProgressSummary> = state.asStateFlow()
}
