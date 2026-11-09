package com.qurancompanion.app.data.repository

import com.qurancompanion.app.data.local.AyahEntity
import com.qurancompanion.app.data.local.ConfusionEntity
import com.qurancompanion.app.data.local.QuizResultEntity
import com.qurancompanion.app.data.local.QuranDatabase
import com.qurancompanion.app.data.similarity.ArabicText
import com.qurancompanion.app.domain.model.Achievement
import com.qurancompanion.app.domain.model.QuestionResult
import com.qurancompanion.app.domain.model.QuranConstants
import com.qurancompanion.app.domain.model.QuizBuildResult
import com.qurancompanion.app.domain.model.QuizFinish
import com.qurancompanion.app.domain.model.QuizQuestion
import com.qurancompanion.app.domain.model.QuizType
import com.qurancompanion.app.domain.model.ReviewRating
import com.qurancompanion.app.domain.repository.QuizRepository
import com.qurancompanion.app.domain.repository.ReviewRepository
import com.qurancompanion.app.domain.repository.SimilarityRepository
import com.qurancompanion.app.domain.util.EpochDay
import com.qurancompanion.app.ui.common.toArabicIndic
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class QuizRepositoryImpl(
    db: QuranDatabase,
    private val review: ReviewRepository,
    private val similarity: SimilarityRepository,
) : QuizRepository {
    private val quranDao = db.quranDao()
    private val insights = db.insightsDao()

    override suspend fun buildQuiz(type: QuizType, fromSurah: Int, toSurah: Int, count: Int): QuizBuildResult =
        withContext(Dispatchers.Default) {
            // The comprehensive test ignores the chosen range and draws from everything memorized.
            val pool = if (type == QuizType.COMPREHENSIVE) {
                insights.memorizedAyahs(1, QuranConstants.TOTAL_SURAHS)
            } else {
                insights.memorizedAyahs(fromSurah, toSurah)
            }
            if (pool.isEmpty()) {
                return@withContext QuizBuildResult.Failure("لا توجد آيات محفوظة في هذا النطاق. سجّل حفظك أولًا.")
            }
            val random = Random(System.nanoTime())
            val questions = when (type) {
                QuizType.RECITE -> pool.shuffled(random).take(count).map {
                    QuizQuestion(type, it.id, it.surahId, it.ayahNumber, prompt = "", answer = it.text)
                }
                QuizType.COMPLETE -> pool
                    .filter { ArabicText.tokenize(it.text).original.size >= 5 }
                    .shuffled(random).take(count).map {
                        val words = ArabicText.tokenize(it.text).original
                        QuizQuestion(type, it.id, it.surahId, it.ayahNumber, prompt = words.take(3).joinToString(" "), answer = it.text)
                    }
                QuizType.NEXT_AYAH -> buildNext(pool, count, random)
                QuizType.SIMILAR -> buildSimilar(pool, count, random)
                QuizType.SURAH_CHOICE -> buildSurahChoice(pool, count, random)
                QuizType.RANDOM_START -> buildRandomStart(pool, count, random)
                QuizType.COMPREHENSIVE -> buildComprehensive(pool, count, random)
            }
            if (questions.isEmpty()) {
                QuizBuildResult.Failure(
                    when (type) {
                        QuizType.SIMILAR -> "لا توجد في محفوظك آيات لها متشابهات مناسبة للاختبار حاليًا."
                        QuizType.NEXT_AYAH -> "لا توجد آيات محفوظة يمكن سؤالك عن الآية التي تليها."
                        else -> "لا توجد آيات مناسبة لهذا الاختبار في النطاق المحدد."
                    },
                )
            } else {
                QuizBuildResult.Success(questions)
            }
        }

    private suspend fun buildNext(pool: List<AyahEntity>, count: Int, random: Random): List<QuizQuestion> {
        val out = ArrayList<QuizQuestion>()
        for (a in pool.shuffled(random)) {
            if (out.size >= count) break
            val next = quranDao.getAyah(a.id + 1)
            if (next == null || next.surahId != a.surahId) continue
            val surahSize = quranDao.surahAyahCount(a.surahId) ?: continue
            val firstId = a.id - a.ayahNumber + 1
            val distractorIds = LinkedHashSet<Int>()
            var guard = 0
            while (distractorIds.size < 3 && guard++ < 40) {
                val id = if (surahSize >= 6) firstId + random.nextInt(surahSize) else 1 + random.nextInt(6236)
                if (id != next.id && id != a.id) distractorIds.add(id)
            }
            val distractors = distractorIds.mapNotNull { quranDao.getAyah(it) }
            val options = (listOf(next) + distractors).map { snippet(it.text, 10) }
            if (options.distinct().size != options.size || options.size < 3) continue
            val order = options.indices.shuffled(random)
            val shuffled = order.map { options[it] }
            out.add(
                QuizQuestion(
                    QuizType.NEXT_AYAH, a.id, a.surahId, a.ayahNumber,
                    prompt = a.text, answer = options[0],
                    options = shuffled, correctIndex = order.indexOf(0),
                ),
            )
        }
        return out
    }

    private suspend fun buildSimilar(pool: List<AyahEntity>, count: Int, random: Random): List<QuizQuestion> {
        similarity.ensureIndexed()
        val memorized = pool.associateBy { it.id }
        val pairs = insights.allPairs().filter { it.score >= 0.6 }
        val partners = HashMap<Int, MutableList<Int>>()
        for (p in pairs) {
            partners.getOrPut(p.ayahA) { ArrayList() }.add(p.ayahB)
            partners.getOrPut(p.ayahB) { ArrayList() }.add(p.ayahA)
        }
        val bases = memorized.keys.filter { partners.containsKey(it) }.shuffled(random)
        val out = ArrayList<QuizQuestion>()
        for (baseId in bases) {
            if (out.size >= count) break
            val base = memorized.getValue(baseId)
            val baseTokens = ArabicText.tokenize(base.text)
            val candidates = ArrayList<Triple<Int, Int, List<String>>>() // partner id, prefix length, partner original words
            for (pid in partners.getValue(baseId).distinct()) {
                val partner = quranDao.getAyah(pid) ?: continue
                val tokens = ArabicText.tokenize(partner.text)
                val prefix = ArabicText.commonPrefixLength(baseTokens.normalized, tokens.normalized)
                if (prefix < 2) continue
                candidates.add(Triple(pid, prefix, tokens.original))
            }
            if (candidates.isEmpty()) continue
            val prefix = candidates.minOf { it.second }
            if (prefix >= baseTokens.original.size) continue
            val prefixWords = baseTokens.original.take(prefix)
            val baseSuffix = snippet(baseTokens.original.drop(prefix).joinToString(" "), 10)
            val optionTexts = arrayListOf(baseSuffix)
            val optionIds = arrayListOf(baseId)
            for ((pid, _, words) in candidates.shuffled(random)) {
                if (optionTexts.size >= 4) break
                val suffix = snippet(words.drop(prefix).joinToString(" "), 10)
                if (suffix.isNotBlank() && suffix !in optionTexts) {
                    optionTexts.add(suffix)
                    optionIds.add(pid)
                }
            }
            if (optionTexts.size < 2) continue
            val order = optionTexts.indices.shuffled(random)
            out.add(
                QuizQuestion(
                    QuizType.SIMILAR, baseId, base.surahId, base.ayahNumber,
                    prompt = prefixWords.joinToString(" "), answer = baseSuffix,
                    options = order.map { optionTexts[it] },
                    correctIndex = order.indexOf(0),
                    optionAyahIds = order.map { optionIds[it] },
                ),
            )
        }
        return out
    }

    /** "Which surah is this ayah from?" Ayahs repeated word for word in another surah are skipped. */
    private suspend fun buildSurahChoice(pool: List<AyahEntity>, count: Int, random: Random): List<QuizQuestion> {
        val surahs = quranDao.allSurahs()
        if (surahs.size < 4) return emptyList()
        val occurrences = HashMap<String, Int>()
        for (a in quranDao.allAyahs()) {
            val key = ArabicText.tokenize(a.text).normalized.joinToString(" ")
            occurrences[key] = (occurrences[key] ?: 0) + 1
        }
        val out = ArrayList<QuizQuestion>()
        for (a in pool.shuffled(random)) {
            if (out.size >= count) break
            val key = ArabicText.tokenize(a.text).normalized.joinToString(" ")
            if ((occurrences[key] ?: 0) > 1) continue
            val correct = surahs.firstOrNull { it.id == a.surahId } ?: continue
            val distractors = surahs.filter { it.id != a.surahId }.shuffled(random).take(3)
            val names = (listOf(correct) + distractors).map { it.nameAr }
            val order = names.indices.shuffled(random)
            out.add(
                QuizQuestion(
                    QuizType.SURAH_CHOICE, a.id, a.surahId, a.ayahNumber,
                    prompt = snippet(a.text, 14), answer = correct.nameAr,
                    options = order.map { names[it] }, correctIndex = order.indexOf(0),
                ),
            )
        }
        return out
    }

    /** A random starting point in the user's memorized ayahs: recite three ayahs from there. */
    private suspend fun buildRandomStart(pool: List<AyahEntity>, count: Int, random: Random): List<QuizQuestion> {
        val out = ArrayList<QuizQuestion>()
        for (a in pool.shuffled(random)) {
            if (out.size >= count) break
            val passage = arrayListOf(a)
            var nextId = a.id + 1
            while (passage.size < 3) {
                val next = quranDao.getAyah(nextId) ?: break
                if (next.surahId != a.surahId) break
                passage.add(next)
                nextId++
            }
            out.add(
                QuizQuestion(
                    QuizType.RANDOM_START, a.id, a.surahId, a.ayahNumber,
                    prompt = ArabicText.tokenize(a.text).original.take(2).joinToString(" "),
                    answer = passage.joinToString(" ") { it.text + " \uFD3F${it.ayahNumber.toArabicIndic()}\uFD3E" },
                ),
            )
        }
        return out
    }

    /**
     * A random place in everything the user memorized: the ayah is shown and the following ayahs
     * (only ones that are also memorized, in the same surah) are to be recited.
     */
    private fun buildComprehensive(pool: List<AyahEntity>, count: Int, random: Random): List<QuizQuestion> {
        val byId = pool.associateBy { it.id }
        val out = ArrayList<QuizQuestion>()
        for (a in pool.shuffled(random)) {
            if (out.size >= count) break
            val following = ArrayList<AyahEntity>()
            var id = a.id + 1
            while (following.size < 3) {
                val next = byId[id] ?: break
                if (next.surahId != a.surahId) break
                following.add(next)
                id++
            }
            if (following.isEmpty()) continue
            out.add(
                QuizQuestion(
                    QuizType.COMPREHENSIVE, a.id, a.surahId, a.ayahNumber,
                    prompt = a.text,
                    answer = following.joinToString(" ") { it.text + " \uFD3F${it.ayahNumber.toArabicIndic()}\uFD3E" },
                ),
            )
        }
        return out
    }

    override suspend fun recordConfusion(correctAyahId: Int, chosenAyahId: Int) {
        if (correctAyahId == chosenAyahId) return
        val a = minOf(correctAyahId, chosenAyahId)
        val b = maxOf(correctAyahId, chosenAyahId)
        val existing = insights.getConfusion(a, b)
        insights.upsertConfusion(ConfusionEntity(a, b, (existing?.times ?: 0) + 1, System.currentTimeMillis()))
    }

    override suspend fun finishQuiz(type: QuizType, results: List<QuestionResult>, rating: ReviewRating): QuizFinish {
        val now = System.currentTimeMillis()
        insights.insertQuiz(
            QuizResultEntity(
                timestamp = now,
                day = EpochDay.today(),
                type = type.name,
                total = results.size,
                correct = results.count { it.correct },
                rating = rating.name,
            ),
        )
        val byEntry = HashMap<Long, MutableList<QuestionResult>>()
        for (r in results) {
            val entryId = insights.entryIdOfAyah(r.ayahId) ?: continue
            byEntry.getOrPut(entryId) { ArrayList() }.add(r)
        }
        val achievements = LinkedHashSet<Achievement>()
        for ((entryId, list) in byEntry) {
            val wrong = list.count { !it.correct }
            val effective = if (wrong > 0 && rating.ordinal < ReviewRating.AVERAGE.ordinal) ReviewRating.AVERAGE else rating
            val outcome = review.applyReview(entryId, effective, wrong, "اختبار", null, "QUIZ")
            achievements.addAll(outcome.newAchievements)
        }
        return QuizFinish(byEntry.size, achievements.toList())
    }

    private fun snippet(text: String, words: Int): String {
        val all = ArabicText.tokenize(text).original
        return if (all.size <= words) all.joinToString(" ") else all.take(words).joinToString(" ") + " …"
    }
}
