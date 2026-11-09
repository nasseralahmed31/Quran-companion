package com.qurancompanion.app.data.backup

import com.qurancompanion.app.data.local.AchievementEntity
import com.qurancompanion.app.data.local.ActivityEntity
import com.qurancompanion.app.data.local.ConfusionEntity
import com.qurancompanion.app.data.local.GoalEntity
import com.qurancompanion.app.data.local.MemorizationEntryEntity
import com.qurancompanion.app.data.local.QuizResultEntity
import com.qurancompanion.app.data.local.ReadingLogEntity
import com.qurancompanion.app.data.local.ReviewScheduleEntity
import com.qurancompanion.app.data.local.ReviewSessionEntity
import com.qurancompanion.app.domain.model.GoalKind
import com.qurancompanion.app.domain.model.GoalUnit
import com.qurancompanion.app.domain.model.MasteryLevel
import com.qurancompanion.app.domain.model.MemorizationState
import com.qurancompanion.app.domain.model.QuranConstants
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class BackupFormatException(message: String) : Exception(message)

data class BackupReminder(val type: String, val enabled: Boolean, val hour: Int, val minute: Int)

data class BackupSettings(
    val dailyTarget: Int,
    val themeMode: String,
    val colorTheme: String,
    val reminders: List<BackupReminder>,
    val lastReadPage: Int = 0,
    val bookmarks: List<Int> = emptyList(),
    val wirdReadPages: Int = 0,
    val wirdReviewAyahs: Int = 0,
    val khatmStartDay: Long = 0,
    val khatmDays: Int = 0,
)

/** Everything personal. ayah_progress is derived from [entries] and rebuilt after a restore. */
data class BackupData(
    val createdAt: Long,
    val settings: BackupSettings,
    val entries: List<MemorizationEntryEntity>,
    val schedules: List<ReviewScheduleEntity>,
    val sessions: List<ReviewSessionEntity>,
    val goals: List<GoalEntity>,
    val confusions: List<ConfusionEntity>,
    val quizResults: List<QuizResultEntity>,
    val achievements: List<AchievementEntity>,
    val activity: List<ActivityEntity>,
    val readingLog: List<ReadingLogEntity> = emptyList(),
)

object BackupCodec {
    const val FORMAT = "quran-companion-backup"
    const val VERSION = 1

    fun encode(d: BackupData): String {
        val root = JSONObject()
        root.put("format", FORMAT)
        root.put("version", VERSION)
        root.put("createdAt", d.createdAt)

        val settings = JSONObject()
        settings.put("dailyTarget", d.settings.dailyTarget)
        settings.put("themeMode", d.settings.themeMode)
        settings.put("colorTheme", d.settings.colorTheme)
        settings.put("lastReadPage", d.settings.lastReadPage)
        settings.put("bookmarks", JSONArray(d.settings.bookmarks))
        settings.put("wirdReadPages", d.settings.wirdReadPages)
        settings.put("wirdReviewAyahs", d.settings.wirdReviewAyahs)
        settings.put("khatmStartDay", d.settings.khatmStartDay)
        settings.put("khatmDays", d.settings.khatmDays)
        settings.put(
            "reminders",
            array(d.settings.reminders) { r ->
                JSONObject().put("type", r.type).put("enabled", r.enabled).put("hour", r.hour).put("minute", r.minute)
            },
        )
        root.put("settings", settings)

        root.put(
            "entries",
            array(d.entries) { e ->
                JSONObject().put("id", e.id).put("surahId", e.surahId).put("fromAyah", e.fromAyah).put("toAyah", e.toAyah)
                    .put("memorizedOn", e.memorizedOn).put("state", e.state).put("mastery", e.mastery)
                    .put("note", e.note).put("createdAt", e.createdAt).put("updatedAt", e.updatedAt)
                    .put("isPrevious", e.isPrevious)
            },
        )
        root.put(
            "schedules",
            array(d.schedules) { s ->
                JSONObject().put("entryId", s.entryId).put("nextReviewDay", s.nextReviewDay).put("intervalDays", s.intervalDays)
                    .put("ease", s.ease).put("repetitions", s.repetitions).put("lapses", s.lapses)
                    .put("lastReviewDay", s.lastReviewDay)
            },
        )
        root.put(
            "sessions",
            array(d.sessions) { s ->
                JSONObject().put("id", s.id).put("entryId", s.entryId).put("surahId", s.surahId).put("fromAyah", s.fromAyah)
                    .put("toAyah", s.toAyah).put("reviewedOn", s.reviewedOn).put("timestamp", s.timestamp)
                    .put("rating", s.rating).put("errorCount", s.errorCount).put("note", s.note)
                    .put("nextReviewDay", s.nextReviewDay).put("source", s.source)
            },
        )
        root.put(
            "goals",
            array(d.goals) { g ->
                JSONObject().put("id", g.id).put("name", g.name).put("kind", g.kind).put("startAyahId", g.startAyahId)
                    .put("endAyahId", g.endAyahId).put("startDay", g.startDay).put("endDay", g.endDay)
                    .put("dailyAmount", g.dailyAmount).put("unit", g.unit).put("createdAt", g.createdAt)
            },
        )
        root.put(
            "confusions",
            array(d.confusions) { c ->
                JSONObject().put("ayahA", c.ayahA).put("ayahB", c.ayahB).put("times", c.times).put("lastAt", c.lastAt)
            },
        )
        root.put(
            "quizResults",
            array(d.quizResults) { q ->
                JSONObject().put("id", q.id).put("timestamp", q.timestamp).put("day", q.day).put("type", q.type)
                    .put("total", q.total).put("correct", q.correct).put("rating", q.rating)
            },
        )
        root.put(
            "achievements",
            array(d.achievements) { a -> JSONObject().put("key", a.achievementKey).put("unlockedAt", a.unlockedAt) },
        )
        root.put(
            "activity",
            array(d.activity) { a ->
                JSONObject().put("id", a.id).put("type", a.type).put("day", a.day).put("timestamp", a.timestamp)
                    .put("surahId", a.surahId ?: JSONObject.NULL).put("fromAyah", a.fromAyah ?: JSONObject.NULL)
                    .put("toAyah", a.toAyah ?: JSONObject.NULL).put("detail", a.detail)
            },
        )
        root.put("readingLog", array(d.readingLog) { r -> JSONObject().put("page", r.page).put("day", r.day) })
        return root.toString()
    }

    /** Parses and validates; throws [BackupFormatException] with an Arabic message if anything is wrong. */
    fun decode(json: String): BackupData {
        val root = try {
            JSONObject(json)
        } catch (e: JSONException) {
            throw BackupFormatException("الملف ليس نسخة احتياطية صالحة")
        }
        if (root.optString("format") != FORMAT) {
            throw BackupFormatException("هذا الملف ليس نسخة احتياطية من رفيق القرآن")
        }
        val version = root.optInt("version", 0)
        if (version < 1 || version > VERSION) {
            throw BackupFormatException("إصدار النسخة غير مدعوم؛ حدّث التطبيق ثم أعد المحاولة")
        }

        val data = try {
            val s = root.getJSONObject("settings")
            val reminders = s.optJSONArray("reminders")?.let { arr ->
                objects(arr).map { BackupReminder(it.getString("type"), it.getBoolean("enabled"), it.getInt("hour"), it.getInt("minute")) }
            }.orEmpty()
            BackupData(
                createdAt = root.optLong("createdAt", 0L),
                settings = BackupSettings(
                    dailyTarget = s.optInt("dailyTarget", 0),
                    themeMode = s.optString("themeMode", ""),
                    colorTheme = s.optString("colorTheme", ""),
                    reminders = reminders,
                    lastReadPage = s.optInt("lastReadPage", 0),
                    bookmarks = s.optJSONArray("bookmarks")?.let { arr -> (0 until arr.length()).map { arr.getInt(it) } }.orEmpty(),
                    wirdReadPages = s.optInt("wirdReadPages", 0),
                    wirdReviewAyahs = s.optInt("wirdReviewAyahs", 0),
                    khatmStartDay = s.optLong("khatmStartDay", 0L),
                    khatmDays = s.optInt("khatmDays", 0),
                ),
                entries = objects(root.getJSONArray("entries")).map {
                    MemorizationEntryEntity(
                        id = it.getLong("id"), surahId = it.getInt("surahId"), fromAyah = it.getInt("fromAyah"),
                        toAyah = it.getInt("toAyah"), memorizedOn = it.getLong("memorizedOn"), state = it.getString("state"),
                        mastery = it.getString("mastery"), note = it.getString("note"),
                        createdAt = it.getLong("createdAt"), updatedAt = it.getLong("updatedAt"),
                        isPrevious = it.optBoolean("isPrevious", false),
                    )
                },
                schedules = objects(root.getJSONArray("schedules")).map {
                    ReviewScheduleEntity(
                        entryId = it.getLong("entryId"), nextReviewDay = it.getLong("nextReviewDay"),
                        intervalDays = it.getInt("intervalDays"), ease = it.getDouble("ease"),
                        repetitions = it.getInt("repetitions"), lapses = it.getInt("lapses"),
                        lastReviewDay = it.getLong("lastReviewDay"),
                    )
                },
                sessions = objects(root.getJSONArray("sessions")).map {
                    ReviewSessionEntity(
                        id = it.getLong("id"), entryId = it.getLong("entryId"), surahId = it.getInt("surahId"),
                        fromAyah = it.getInt("fromAyah"), toAyah = it.getInt("toAyah"), reviewedOn = it.getLong("reviewedOn"),
                        timestamp = it.getLong("timestamp"), rating = it.getString("rating"),
                        errorCount = it.getInt("errorCount"), note = it.getString("note"),
                        nextReviewDay = it.getLong("nextReviewDay"), source = it.getString("source"),
                    )
                },
                goals = objects(root.getJSONArray("goals")).map {
                    GoalEntity(
                        id = it.getLong("id"), name = it.getString("name"), kind = it.getString("kind"),
                        startAyahId = it.getInt("startAyahId"), endAyahId = it.getInt("endAyahId"),
                        startDay = it.getLong("startDay"), endDay = it.getLong("endDay"),
                        dailyAmount = it.getInt("dailyAmount"), unit = it.getString("unit"), createdAt = it.getLong("createdAt"),
                    )
                },
                confusions = objects(root.getJSONArray("confusions")).map {
                    ConfusionEntity(it.getInt("ayahA"), it.getInt("ayahB"), it.getInt("times"), it.getLong("lastAt"))
                },
                quizResults = objects(root.getJSONArray("quizResults")).map {
                    QuizResultEntity(
                        id = it.getLong("id"), timestamp = it.getLong("timestamp"), day = it.getLong("day"),
                        type = it.getString("type"), total = it.getInt("total"), correct = it.getInt("correct"),
                        rating = it.getString("rating"),
                    )
                },
                achievements = objects(root.getJSONArray("achievements")).map {
                    AchievementEntity(it.getString("key"), it.getLong("unlockedAt"))
                },
                activity = objects(root.getJSONArray("activity")).map {
                    ActivityEntity(
                        id = it.getLong("id"), type = it.getString("type"), day = it.getLong("day"),
                        timestamp = it.getLong("timestamp"),
                        surahId = if (it.isNull("surahId")) null else it.getInt("surahId"),
                        fromAyah = if (it.isNull("fromAyah")) null else it.getInt("fromAyah"),
                        toAyah = if (it.isNull("toAyah")) null else it.getInt("toAyah"),
                        detail = it.getString("detail"),
                    )
                },
                readingLog = root.optJSONArray("readingLog")
                    ?.let { arr -> objects(arr).map { ReadingLogEntity(it.getInt("page"), it.getLong("day")) } }
                    .orEmpty(),
            )
        } catch (e: JSONException) {
            throw BackupFormatException("بيانات النسخة ناقصة أو تالفة")
        }
        validate(data)
        return data
    }

    private fun validate(d: BackupData) {
        val states = MemorizationState.entries.map { it.name }.toSet()
        val masteries = MasteryLevel.entries.map { it.name }.toSet()
        val ids = HashSet<Long>()
        for (e in d.entries) {
            if (e.id <= 0 || !ids.add(e.id)) throw BackupFormatException("معرّف سجل حفظ غير صالح أو مكرر")
            if (e.surahId !in 1..QuranConstants.TOTAL_SURAHS || e.fromAyah < 1 || e.toAyah < e.fromAyah) {
                throw BackupFormatException("سجل حفظ بنطاق آيات غير صالح")
            }
            if (e.state !in states || e.mastery !in masteries) throw BackupFormatException("سجل حفظ بحالة غير معروفة")
        }
        if (d.schedules.any { it.entryId !in ids }) throw BackupFormatException("جدول مراجعة يشير إلى سجل غير موجود")
        val kinds = GoalKind.entries.map { it.name }.toSet()
        val units = GoalUnit.entries.map { it.name }.toSet()
        if (d.goals.any { it.kind !in kinds || it.unit !in units }) throw BackupFormatException("هدف بنوع غير معروف")
        if (d.readingLog.any { it.page !in 1..QuranConstants.TOTAL_PAGES }) throw BackupFormatException("سجل قراءة بصفحة غير صالحة")
    }

    private fun <T> array(items: List<T>, build: (T) -> JSONObject): JSONArray {
        val out = JSONArray()
        items.forEach { out.put(build(it)) }
        return out
    }

    private fun objects(arr: JSONArray): List<JSONObject> = (0 until arr.length()).map { arr.getJSONObject(it) }
}
