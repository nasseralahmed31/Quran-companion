package com.qurancompanion.app.data.repository

import androidx.room.withTransaction
import com.qurancompanion.app.data.local.ConfusionEntity
import com.qurancompanion.app.data.local.QuranDatabase
import com.qurancompanion.app.data.local.SimilarPairEntity
import com.qurancompanion.app.data.similarity.AyahText
import com.qurancompanion.app.data.similarity.SimilarityEngine
import com.qurancompanion.app.domain.model.Confusion
import com.qurancompanion.app.domain.model.QuranConstants
import com.qurancompanion.app.domain.model.SimilarGroup
import com.qurancompanion.app.domain.model.SimilarPair
import com.qurancompanion.app.domain.repository.SimilarityRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class SimilarityRepositoryImpl(private val db: QuranDatabase) : SimilarityRepository {
    private val quranDao = db.quranDao()
    private val insights = db.insightsDao()
    private val mutex = Mutex()

    @Volatile
    private var cachedGroups: Map<Double, List<SimilarGroup>> = emptyMap()

    override suspend fun ensureIndexed(): Boolean = mutex.withLock {
        withContext(Dispatchers.Default) {
            if (insights.pairCount() > 0) return@withContext true
            val ayahs = quranDao.allAyahs()
            if (ayahs.size != QuranConstants.TOTAL_AYAHS) return@withContext false
            val pairs = SimilarityEngine().compute(ayahs.map { AyahText(it.id, it.text) })
            db.withTransaction {
                insights.deletePairs()
                pairs.chunked(500).forEach { chunk ->
                    insights.insertPairs(chunk.map { SimilarPairEntity(it.a, it.b, it.score) })
                }
            }
            cachedGroups = emptyMap()
            true
        }
    }

    override suspend fun loadGroups(minScore: Double): List<SimilarGroup> = withContext(Dispatchers.Default) {
        cachedGroups[minScore]?.let { return@withContext it }
        val pairs = insights.allPairs().filter { it.score >= minScore }
        val parent = HashMap<Int, Int>()
        fun find(x: Int): Int {
            var root = x
            while ((parent[root] ?: root) != root) root = parent.getValue(root)
            var cur = x
            while ((parent[cur] ?: cur) != root) {
                val next = parent.getValue(cur)
                parent[cur] = root
                cur = next
            }
            return root
        }
        for (p in pairs) {
            parent.putIfAbsent(p.ayahA, p.ayahA)
            parent.putIfAbsent(p.ayahB, p.ayahB)
            val ra = find(p.ayahA)
            val rb = find(p.ayahB)
            if (ra != rb) parent[maxOf(ra, rb)] = minOf(ra, rb)
        }
        val members = HashMap<Int, MutableSet<Int>>()
        for (id in parent.keys) members.getOrPut(find(id)) { sortedSetOf<Int>() }.add(id)
        val scoreSum = HashMap<Int, Double>()
        val scoreCount = HashMap<Int, Int>()
        for (p in pairs) {
            val root = find(p.ayahA)
            scoreSum[root] = (scoreSum[root] ?: 0.0) + p.score
            scoreCount[root] = (scoreCount[root] ?: 0) + 1
        }
        val groups = members.entries
            .filter { it.value.size >= 2 }
            .map { (root, ids) ->
                SimilarGroup(root, ids.toList(), (scoreSum[root] ?: 0.0) / (scoreCount[root] ?: 1))
            }
            .sortedWith(compareByDescending<SimilarGroup> { it.ayahIds.size }.thenBy { it.id })
        cachedGroups = cachedGroups + (minScore to groups)
        groups
    }

    override suspend fun loadPairs(minScore: Double): List<SimilarPair> = withContext(Dispatchers.Default) {
        insights.allPairs().filter { it.score >= minScore }.map { SimilarPair(it.ayahA, it.ayahB, it.score) }
    }

    override fun observeConfusions(): Flow<List<Confusion>> =
        insights.observeConfusions().map { list -> list.map { it.toDomain() } }

    override fun observeMemorizedAyahIds(): Flow<Set<Int>> =
        insights.observeMemorizedAyahIds().map { it.toSet() }
}

private fun ConfusionEntity.toDomain() = Confusion(ayahA, ayahB, times, lastAt)
