package com.qurancompanion.app.data.quran

import com.qurancompanion.app.domain.model.QuranConstants
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

class QuranDataException(message: String) : Exception(message)

data class RawAyah(val sura: Int, val aya: Int, val text: String) {
    val key: Int get() = sura * 1000 + aya
}

data class RawSurah(
    val index: Int,
    val ayas: Int,
    val name: String,
    val translit: String,
    val english: String,
    val type: String,
)

data class Position(val index: Int, val sura: Int, val aya: Int) {
    val key: Int get() = sura * 1000 + aya
}

data class QuranMetadata(
    val suras: List<RawSurah>,
    val juz: List<Position>,
    val pages: List<Position>,
)

/**
 * Reads Tanzil's files verbatim: `quran-uthmani.txt` (sura|aya|text lines) and
 * `quran-data.xml` (surah, juz and page boundaries). Nothing is generated here;
 * [validate] refuses to import data that does not match the standard counts.
 */
object QuranSourceParser {

    fun parseText(input: InputStream): List<RawAyah> {
        val result = ArrayList<RawAyah>(QuranConstants.TOTAL_AYAHS + 8)
        input.bufferedReader(Charsets.UTF_8).useLines { lines ->
            var lineNo = 0
            for (raw in lines) {
                lineNo++
                val line = raw.trimStart('\uFEFF').trim()
                if (line.isEmpty() || line.startsWith("#")) continue
                val parts = line.split('|', limit = 3)
                if (parts.size != 3) throw QuranDataException("سطر غير صالح في ملف النص (السطر $lineNo)")
                val sura = parts[0].trim().toIntOrNull()
                val aya = parts[1].trim().toIntOrNull()
                if (sura == null || aya == null) throw QuranDataException("رقم غير صالح في ملف النص (السطر $lineNo)")
                result.add(RawAyah(sura, aya, parts[2].trim()))
            }
        }
        return result
    }

    fun parseMetadata(input: InputStream): QuranMetadata {
        val doc = try {
            DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input)
        } catch (e: Exception) {
            throw QuranDataException("تعذّر قراءة ملف quran-data.xml: ${e.message}")
        }

        fun elements(tag: String): List<Element> {
            val nodes = doc.getElementsByTagName(tag)
            return (0 until nodes.length).mapNotNull { nodes.item(it) as? Element }
        }

        fun Element.intAttr(name: String): Int =
            getAttribute(name).trim().toIntOrNull()
                ?: throw QuranDataException("قيمة غير صالحة للخاصية $name في العنصر $tagName")

        val suras = elements("sura")
            .filter { it.hasAttribute("ayas") }
            .map {
                RawSurah(
                    index = it.intAttr("index"),
                    ayas = it.intAttr("ayas"),
                    name = it.getAttribute("name"),
                    translit = it.getAttribute("tname"),
                    english = it.getAttribute("ename"),
                    type = it.getAttribute("type"),
                )
            }
            .sortedBy { it.index }
        val juz = elements("juz")
            .map { Position(it.intAttr("index"), it.intAttr("sura"), it.intAttr("aya")) }
            .sortedBy { it.index }
        val pages = elements("page")
            .map { Position(it.intAttr("index"), it.intAttr("sura"), it.intAttr("aya")) }
            .sortedBy { it.index }
        return QuranMetadata(suras, juz, pages)
    }

    /** Throws [QuranDataException] listing every mismatch found (first few). */
    fun validate(text: List<RawAyah>, meta: QuranMetadata) {
        val problems = mutableListOf<String>()

        if (text.size != QuranConstants.TOTAL_AYAHS) {
            problems += "عدد الآيات في ملف النص ${text.size} بدل ${QuranConstants.TOTAL_AYAHS}"
        }
        if (meta.suras.size != QuranConstants.TOTAL_SURAHS) {
            problems += "عدد السور في البيانات الوصفية ${meta.suras.size} بدل ${QuranConstants.TOTAL_SURAHS}"
        }
        meta.suras.forEachIndexed { i, s ->
            if (s.index != i + 1) problems += "ترتيب السور غير متسلسل عند السورة ${s.index}"
        }
        val ayaSum = meta.suras.sumOf { it.ayas }
        if (ayaSum != QuranConstants.TOTAL_AYAHS) {
            problems += "مجموع آيات السور في البيانات الوصفية $ayaSum بدل ${QuranConstants.TOTAL_AYAHS}"
        }

        val bySurah = text.groupBy { it.sura }
        for (s in meta.suras) {
            val list = bySurah[s.index].orEmpty()
            if (list.size != s.ayas) {
                problems += "السورة ${s.index}: ${list.size} آية في النص بدل ${s.ayas}"
            } else if (list.map { it.aya } != (1..s.ayas).toList()) {
                problems += "السورة ${s.index}: أرقام الآيات غير متسلسلة"
            }
        }
        if (text.zipWithNext().any { (a, b) -> b.key <= a.key }) {
            problems += "ترتيب الآيات في ملف النص غير تصاعدي"
        }
        if (text.any { it.text.isBlank() }) {
            problems += "توجد آية بنص فارغ"
        }

        val keys = text.mapTo(HashSet()) { it.key }
        fun checkBoundaries(label: String, list: List<Position>, expected: Int) {
            if (list.size != expected) {
                problems += "عدد $label ${list.size} بدل $expected"
                return
            }
            list.forEachIndexed { i, p ->
                if (p.index != i + 1) problems += "ترقيم $label غير متسلسل عند ${p.index}"
                if (p.key !in keys) problems += "بداية $label ${p.index} (${p.sura}:${p.aya}) غير موجودة في النص"
            }
            if (list.first().key != 1001) problems += "$label لا يبدأ من 1:1"
            if (list.zipWithNext().any { (a, b) -> b.key <= a.key }) problems += "حدود $label غير تصاعدية"
        }
        checkBoundaries("الأجزاء", meta.juz, QuranConstants.TOTAL_JUZ)
        checkBoundaries("الصفحات", meta.pages, QuranConstants.TOTAL_PAGES)

        if (problems.isNotEmpty()) {
            throw QuranDataException(problems.take(6).joinToString("\n"))
        }
    }
}
