package com.qurancompanion.app.data.quran

import java.io.ByteArrayInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkerParserTest {
    @Test
    fun readsQuartersAndSajdasAndIgnoresOtherElements() {
        val xml = """<quran><quarters><quarter index="2" sura="2" aya="26"/><quarter index="1" sura="1" aya="1"/></quarters>""" +
            """<sajdas><sajda index="1" sura="7" aya="206" type="recommended"/></sajdas>""" +
            """<pages><page index="1" sura="1" aya="1"/></pages></quran>"""
        val markers = QuranSourceParser.parseMarkers(ByteArrayInputStream(xml.toByteArray()))
        assertEquals(listOf(1, 2), markers.quarters.map { it.index })
        assertEquals(1, markers.sajdas.size)
        assertEquals(7, markers.sajdas[0].sura)
        assertEquals(206, markers.sajdas[0].aya)
    }

    @Test
    fun brokenXmlGivesNoMarkersInsteadOfFailing() {
        val markers = QuranSourceParser.parseMarkers(ByteArrayInputStream("<not xml".toByteArray()))
        assertTrue(markers.quarters.isEmpty() && markers.sajdas.isEmpty())
    }
}
