package com.qurancompanion.app.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.qurancompanion.app.data.similarity.ArabicText
import com.qurancompanion.app.domain.model.Ayah
import com.qurancompanion.app.domain.model.AyahMark
import com.qurancompanion.app.domain.model.AyahMarker

data class MarkColors(
    val target: Color,
    val memorized: Color,
    val inProgress: Color,
    val needsReview: Color,
    val marker: Color,
)

/** Character range [start, end) of one ayah inside an annotated string (used to detect taps). */
data class AyahSpan(val ayahId: Int, val start: Int, val end: Int)

@Composable
fun rememberMarkColors(): MarkColors {
    val c = MaterialTheme.colorScheme
    return remember(c) {
        MarkColors(
            target = c.primary.copy(alpha = 0.32f),
            memorized = c.primary.copy(alpha = 0.12f),
            inProgress = c.secondary.copy(alpha = 0.14f),
            needsReview = c.error.copy(alpha = 0.14f),
            marker = c.secondary,
        )
    }
}

/**
 * Appends ayah texts followed by ornate end-of-ayah numbers, highlighting the target and
 * memorized ayahs. In [hideMode] every ayah is hidden until its id is in [revealed]; [partial] (ayah id,
 * words) shows the first words of the ayah being recited.
 * Hizb-quarter starts get a "۞" before them and sajdas a "۩" after them. Each ayah's character
 * range is added to [spans] when given.
 */
fun AnnotatedString.Builder.appendAyahs(
    ayahs: List<Ayah>,
    highlightAyahId: Int,
    marks: Map<Int, AyahMark>,
    colors: MarkColors,
    spans: MutableList<AyahSpan>? = null,
    hideMode: Boolean = false,
    revealed: Set<Int> = emptySet(),
    markers: Map<Int, Set<AyahMarker>> = emptyMap(),
    partial: Pair<Int, Int>? = null,
) {
    ayahs.forEach { a ->
        val start = length
        val ayahMarkers = markers[a.id].orEmpty()
        if (AyahMarker.QUARTER in ayahMarkers) {
            withStyle(SpanStyle(color = colors.marker, fontWeight = FontWeight.Bold)) { append("\u06DE ") }
        }
        val background = if (a.id == highlightAyahId) {
            colors.target
        } else {
            when (marks[a.id]) {
                AyahMark.MEMORIZED -> colors.memorized
                AyahMark.IN_PROGRESS -> colors.inProgress
                AyahMark.NEEDS_REVIEW -> colors.needsReview
                null -> null
            }
        }
        val hidden = hideMode && a.id !in revealed
        val shown = when {
            !hidden -> a.text
            // The current ayah shows only the words revealed so far; every other hidden ayah is a placeholder.
            partial != null && partial.first == a.id && partial.second > 0 ->
                ArabicText.tokenize(a.text).original.take(partial.second).joinToString(" ") + " …"
            else -> "· · ·"
        }
        if (background != null) {
            withStyle(SpanStyle(background = background)) { append(shown) }
        } else {
            append(shown)
        }
        withStyle(SpanStyle(color = colors.marker)) {
            append(" \uFD3F${a.ayahNumber.toArabicIndic()}\uFD3E ")
            if (AyahMarker.SAJDA in ayahMarkers) append("\u06E9 ")
        }
        spans?.add(AyahSpan(a.id, start, length))
    }
}

/** Plain flowing text of consecutive ayahs (used by review, quiz and similar-ayah screens). */
fun flowingAyahText(ayahs: List<Ayah>, markerColor: Color): AnnotatedString = buildAnnotatedString {
    appendAyahs(ayahs, 0, emptyMap(), MarkColors(Color.Unspecified, Color.Unspecified, Color.Unspecified, Color.Unspecified, markerColor))
}
