package com.qurancompanion.app.domain.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow

object DayClock {
    /** Emits the current day, re-checking every minute so "today" rolls over at midnight. */
    fun ticker(): Flow<Long> = flow {
        while (true) {
            emit(EpochDay.today())
            delay(60_000)
        }
    }.distinctUntilChanged()
}
