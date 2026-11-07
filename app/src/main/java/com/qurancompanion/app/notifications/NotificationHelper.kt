package com.qurancompanion.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.qurancompanion.app.AppContainer
import com.qurancompanion.app.MainActivity
import com.qurancompanion.app.R
import com.qurancompanion.app.domain.model.ReminderType
import com.qurancompanion.app.domain.model.ReviewStatus
import com.qurancompanion.app.ui.common.toArabicIndic
import kotlinx.coroutines.flow.first

object NotificationHelper {
    const val CHANNEL_ID = "reminders"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(CHANNEL_ID, "التذكيرات", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "تذكيرات الحفظ والمراجعة والهدف اليومي"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** Posts the reminder only if there is something to say right now (no empty or repeated pings). */
    suspend fun maybeNotify(context: Context, type: ReminderType, container: AppContainer) {
        val (title, text) = content(type, container) ?: return
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context, type.ordinal, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .build()
        try {
            manager.notify(NOTIFICATION_BASE_ID + type.ordinal, notification) // same id replaces yesterday's
        } catch (e: SecurityException) {
            // Permission revoked while the worker was queued.
        }
    }

    private suspend fun content(type: ReminderType, container: AppContainer): Pair<String, String>? {
        val summary = container.progressRepository.observeSummary().first()
        return when (type) {
            ReminderType.MEMORIZE ->
                if (summary.todayDoneAyahs > 0) {
                    null
                } else {
                    "وقت الحفظ" to if (summary.todayTargetAyahs > 0) {
                        "هدفك اليوم ${summary.todayTargetAyahs.toArabicIndic()} آية. ابدأ الآن."
                    } else {
                        "حان وقت ورد الحفظ اليومي."
                    }
                }
            ReminderType.REVIEW ->
                if (summary.dueReviewAyahs <= 0) {
                    null
                } else {
                    "وقت المراجعة" to
                        "عندك ${summary.dueReviewAyahs.toArabicIndic()} آية مستحقة للمراجعة، وتبدأ بما يحتاج مراجعة أولًا."
                }
            ReminderType.OVERDUE -> {
                val overdue = container.reviewRepository.observeItems().first().count { it.status == ReviewStatus.OVERDUE }
                if (overdue <= 0) null else "مراجعات متأخرة" to "${overdue.toArabicIndic()} مقطع تأخرت مراجعته، ابدأ بالأضعف منها."
            }
            ReminderType.GOAL ->
                if (summary.todayTargetAyahs <= 0 || summary.todayDoneAyahs >= summary.todayTargetAyahs) {
                    null
                } else {
                    "هدف اليوم" to "بقي ${(summary.todayTargetAyahs - summary.todayDoneAyahs).toArabicIndic()} آية لتُتم هدفك اليومي."
                }
        }
    }

    private const val NOTIFICATION_BASE_ID = 1000
}
