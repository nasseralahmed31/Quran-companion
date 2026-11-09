package com.qurancompanion.app.domain.model

enum class ThemeMode(val label: String) {
    SYSTEM("حسب النظام"),
    LIGHT("نهاري"),
    DARK("ليلي"),
}

enum class ColorTheme(val label: String) {
    EMERALD("الزمردي"),
    SAPPHIRE("الياقوتي"),
    VIOLET("البنفسجي"),
}

enum class ReminderType(
    val label: String,
    val description: String,
    val defaultHour: Int,
    val defaultMinute: Int,
) {
    MEMORIZE("تذكير الحفظ", "تذكير يومي بموعد الحفظ، ولا يصل إن سجّلت حفظًا اليوم", 17, 0),
    REVIEW("تذكير المراجعة", "يصل عند وجود آيات مستحقة للمراجعة", 19, 0),
    OVERDUE("تنبيه المراجعات المتأخرة", "يصل عند وجود مقاطع تأخرت مراجعتها", 9, 0),
    GOAL("هدف اليوم", "يصل إن لم تُكمل هدفك اليومي", 21, 0),
    WIRD("الورد اليومي", "يصل إن بقي شيء من ورد اليوم (تلاوة أو حفظ أو مراجعة)", 20, 30),
}

data class ReminderSetting(val enabled: Boolean, val hour: Int, val minute: Int)

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val colorTheme: ColorTheme = ColorTheme.EMERALD,
    val dailyTargetAyahs: Int = 0,
    val lastReadPage: Int = 0,
    val bookmarks: Set<Int> = emptySet(),
    val wirdReadPages: Int = 0,
    val wirdReviewAyahs: Int = 0,
    val khatmStartDay: Long = 0,
    val khatmDays: Int = 0,
    val autoShiftReviews: Boolean = true,
    val reviewRolloverDay: Long = 0,
    val rolloverNoteDay: Long = 0,
    val rolloverOverdue: Int = 0,
    val rolloverShift: Int = 0,
    val reminders: Map<ReminderType, ReminderSetting> =
        ReminderType.entries.associateWith { ReminderSetting(false, it.defaultHour, it.defaultMinute) },
)

data class BackupSummary(val entries: Int, val reviewSessions: Int, val goals: Int)
