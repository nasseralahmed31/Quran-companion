package com.qurancompanion.app.domain.util

import java.util.TimeZone

/** Local-calendar day numbers (days since 1970-01-01) without java.time, so API 24 works. */
object EpochDay {
    private const val DAY_MS = 86_400_000L

    fun today(): Long = fromMillis(System.currentTimeMillis())

    fun fromMillis(millis: Long): Long =
        Math.floorDiv(millis + TimeZone.getDefault().getOffset(millis), DAY_MS)
}
