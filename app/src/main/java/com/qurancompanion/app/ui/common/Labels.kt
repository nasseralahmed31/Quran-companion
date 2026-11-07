package com.qurancompanion.app.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.qurancompanion.app.domain.model.MasteryLevel
import com.qurancompanion.app.domain.model.MemorizationEntry
import com.qurancompanion.app.domain.util.EpochDay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

fun formatEpochDay(day: Long, today: Long = EpochDay.today()): String = when (day) {
    today -> "اليوم"
    today - 1 -> "أمس"
    else -> {
        val format = SimpleDateFormat("d MMMM yyyy", Locale("ar"))
        format.timeZone = TimeZone.getTimeZone("UTC")
        format.format(Date(day * 86_400_000L))
    }
}

fun rangeLabel(from: Int, to: Int): String =
    if (from == to) "الآية ${from.toArabicIndic()}" else "الآيات ${from.toArabicIndic()}–${to.toArabicIndic()}"

fun MemorizationEntry.rangeLabel(): String = rangeLabel(fromAyah, toAyah)

@Composable
fun masteryColor(level: MasteryLevel): Color = when (level) {
    MasteryLevel.NEW -> MaterialTheme.colorScheme.onSurfaceVariant
    MasteryLevel.IN_PROGRESS -> MaterialTheme.colorScheme.secondary
    MasteryLevel.GOOD -> MaterialTheme.colorScheme.primary
    MasteryLevel.MASTERED -> Color(0xFF22C55E)
    MasteryLevel.NEEDS_REVIEW -> Color(0xFFF59E0B)
    MasteryLevel.WEAK -> MaterialTheme.colorScheme.error
}

/** Short Arabic weekday name for a day number (days since 1970-01-01, a Thursday). */
fun weekdayShort(day: Long): String {
    val names = listOf("أحد", "اثنين", "ثلاثاء", "أربعاء", "خميس", "جمعة", "سبت")
    return names[Math.floorMod(day + 4, 7L).toInt()]
}

/** 0 = Sunday ... 6 = Saturday. */
fun weekdayIndex(day: Long): Int = Math.floorMod(day + 4, 7L).toInt()
