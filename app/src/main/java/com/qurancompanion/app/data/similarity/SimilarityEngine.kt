package com.qurancompanion.app.data.similarity

/** Text helpers for comparing ayahs word by word, ignoring diacritics and letter variants. */
object ArabicText {
    data class Tokens(val original: List<String>, val normalized: List<String>)

    fun normalizeWord(word: String): String {
        val sb = StringBuilder(word.length)
        for (c in word) {
            when {
                c in '\u0610'..'\u061A' || c in '\u064B'..'\u065F' || c == '\u0670' ||
                    c in '\u06D6'..'\u06ED' || c == '\u0640' -> Unit
                c == '\u0623' || c == '\u0625' || c == '\u0622' || c == '\u0671' -> sb.append('\u0627')
                c == '\u0649' -> sb.append('\u064A')
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    /** Splits on whitespace; a token made only of marks is glued to the previous word. */
    fun tokenize(text: String): Tokens {
        val original = ArrayList<String>()
        val normalized = ArrayList<String>()
        for (raw in text.trim().split(Regex("\\s+"))) {
            if (raw.isEmpty()) continue
            val n = normalizeWord(raw)
            if (n.isEmpty()) {
                if (original.isNotEmpty()) original[original.size - 1] = original[original.size - 1] + " " + raw
            } else {
                original.add(raw)
                normalized.add(n)
            }
        }
        return Tokens(original, normalized)
    }

    fun commonPrefixLength(a: List<String>, b: List<String>): Int {
        var i = 0
        while (i < a.size && i < b.size && a[i] == b[i]) i++
        return i
    }
}

object WordDiff {
    data class Word(val text: String, val differs: Boolean)

    /** Marks, in each text, the words that are not present in every one of the texts. */
    fun highlight(texts: List<String>): List<List<Word>> {
        val tokens = texts.map { ArabicText.tokenize(it) }
        val common = tokens.map { it.normalized.toSet() }.reduceOrNull { acc, s -> acc intersect s } ?: emptySet()
        return tokens.map { t -> t.original.mapIndexed { i, w -> Word(w, t.normalized[i] !in common) } }
    }
}

data class AyahText(val id: Int, val text: String)
data class SimilarPairResult(val a: Int, val b: Int, val score: Double)

/**
 * Finds lexically similar ayahs from the Quran text itself (no external data): ayahs are turned
 * into sets of 3-word shingles and compared with the Dice coefficient.
 */
class SimilarityEngine(
    private val shingleSize: Int = 3,
    private val maxShingleFrequency: Int = 80,
    private val minWords: Int = 4,
    private val minShared: Int = 2,
    private val minScore: Double = 0.45,
) {
    fun compute(ayahs: List<AyahText>): List<SimilarPairResult> {
        val shingleSets = HashMap<Int, Set<String>>()
        val index = HashMap<String, MutableList<Int>>()
        for (a in ayahs) {
            val words = ArabicText.tokenize(a.text).normalized
            if (words.size < minWords) continue
            val set = LinkedHashSet<String>()
            for (i in 0..(words.size - shingleSize)) {
                set.add(words.subList(i, i + shingleSize).joinToString(" "))
            }
            if (set.isEmpty()) continue
            shingleSets[a.id] = set
            for (s in set) index.getOrPut(s) { ArrayList() }.add(a.id)
        }

        val shared = HashMap<Long, Int>()
        for (ids in index.values) {
            if (ids.size < 2 || ids.size > maxShingleFrequency) continue
            for (i in ids.indices) {
                for (j in (i + 1) until ids.size) {
                    val x = ids[i]
                    val y = ids[j]
                    val key = (minOf(x, y).toLong() shl 32) or maxOf(x, y).toLong()
                    shared[key] = (shared[key] ?: 0) + 1
                }
            }
        }

        val out = ArrayList<SimilarPairResult>()
        for ((key, count) in shared) {
            if (count < minShared) continue
            val a = (key shr 32).toInt()
            val b = (key and 0xFFFFFFFFL).toInt()
            val sizeA = shingleSets.getValue(a).size
            val sizeB = shingleSets.getValue(b).size
            val score = 2.0 * count / (sizeA + sizeB)
            if (score >= minScore) out.add(SimilarPairResult(a, b, score))
        }
        return out.sortedWith(compareBy({ it.a }, { it.b }))
    }
}
