package com.qurancompanion.app.data.repository

import com.qurancompanion.app.data.local.AyahEntity
import com.qurancompanion.app.data.local.ConfusionEntity
import com.qurancompanion.app.data.local.QuizResultEntity
import com.qurancompanion.app.data.local.QuranDatabase
import com.qurancompanion.app.data.similarity.ArabicText
import com.qurancompanion.app.domain.model.Achievement
import com.qurancompanion.app.domain.model.QuestionResult
import com.qurancompanion.app.domain.model.QuizBuildResult
import com.qurancompanion.app.domain.model.QuizFinish
import com.qurancompanion.app.domain.model.QuizQuestion
import com.qurancompanion.app.domain.model.QuizType
import com.qurancompanion.app.domain.model.ReviewRating
import com.qurancompanion.app.domain.repository.QuizRepository
import com.qurancompanion.app.domain.repository.ReviewRepository
import com.qurancompanion.app.domain.repository.SimilarityRepository
import com.qurancompanion.app.domain.util.EpochDay
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
            val pool = insights.memorizedAyahs(fromSurah, toSurah)
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
