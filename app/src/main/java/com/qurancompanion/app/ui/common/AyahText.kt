package com.qurancompanion.app.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.qurancompanion.app.domain.model.Ayah
import com.qurancompanion.app.domain.model.AyahMark

data class MarkColors(
    val target: Color,
    val memorized: Color,
    val inProgress: Color,
    val needsReview: Color,
    val marker: Color,
)

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

/** Appends ayah texts followed by ornate end-of-ayah numbers, highlighting target / memorized ayahs. */
fun AnnotatedString.Builder.appendAyahs(
    ayahs: List<Ayah>,
    highlightAyahId: Int,
    marks: Map<Int, AyahMark>,
    colors: MarkColors,
) {
    ayahs.forEach { a ->
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
        if (background != null) {
            withStyle(SpanStyle(background = background)) { append(a.text) }
        } else {
            append(a.text)
        }
        withStyle(SpanStyle(color = colors.marker)) {
            append(" \uFD3F${a.ayahNumber.toArabicIndic()}\uFD3E ")
        }
    }
}

/** Plain flowing text of consecutive ayahs (used by review, quiz and similar-ayah screens). */
fun flowingAyahText(ayahs: List<Ayah>, markerColor: Color): AnnotatedString = buildAnnotatedString {
    appendAyahs(ayahs, 0, emptyMap(), MarkColors(Color.Unspecified, Color.Unspecified, Color.Unspecified, Color.Unspecified, markerColor))
}
