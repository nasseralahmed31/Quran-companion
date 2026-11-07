package com.qurancompanion.app.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** Simple line chart with a filled area; values are plotted left to right. */
@Composable
fun LineChart(values: List<Float>, lineColor: Color, fillColor: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxWidth().height(120.dp)) {
        if (values.size < 2) return@Canvas
        val maxValue = maxOf(values.max(), 1f)
        val stepX = size.width / (values.size - 1)
        val points = values.mapIndexed { i, v ->
            Offset(i * stepX, size.height - (v / maxValue) * size.height * 0.9f - size.height * 0.05f)
        }
        val line = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        val area = Path().apply {
            addPath(line)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(area, fillColor)
        drawPath(line, lineColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** Two bars per label (e.g. memorized vs reviewed). */
@Composable
fun PairedBars(
    labels: List<String>,
    first: List<Int>,
    second: List<Int>,
    firstColor: Color,
    secondColor: Color,
    modifier: Modifier = Modifier,
) {
    val maxValue = maxOf(1, (first + second).maxOrNull() ?: 1)
    Row(modifier.fillMaxWidth().height(150.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        labels.indices.forEach { i ->
            Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Box(
                        Modifier.width(10.dp).fillMaxHeight((first[i] / maxValue.toFloat()).coerceIn(0.01f, 1f))
                            .background(firstColor, RoundedCornerShape(4.dp)),
                    )
                    Box(
                        Modifier.width(10.dp).fillMaxHeight((second[i] / maxValue.toFloat()).coerceIn(0.01f, 1f))
                            .background(secondColor, RoundedCornerShape(4.dp)),
                    )
                }
                Text(labels[i], style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Calendar-style grid: [levels] are laid out column by column (7 rows per column); -1 = no cell. */
@Composable
fun Heatmap(levels: List<Int>, palette: List<Color>, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxWidth().height(112.dp)) {
        val weeks = (levels.size + 6) / 7
        if (weeks == 0) return@Canvas
        val cell = minOf(size.width / weeks, size.height / 7)
        val gap = cell * 0.15f
        val offsetX = (size.width - cell * weeks) / 2
        levels.forEachIndexed { i, level ->
            if (level < 0) return@forEachIndexed
            val col = i / 7
            val row = i % 7
            drawRoundRect(
                color = palette[level.coerceIn(0, palette.size - 1)],
                topLeft = Offset(offsetX + col * cell, row * cell),
                size = Size(cell - gap, cell - gap),
                cornerRadius = CornerRadius(3.dp.toPx()),
            )
        }
    }
}

/** One horizontal bar split into proportional segments; zero segments are skipped. */
@Composable
fun StackedBar(segments: List<Pair<Int, Color>>, modifier: Modifier = Modifier) {
    val visible = segments.filter { it.first > 0 }
    Row(modifier.fillMaxWidth().height(18.dp).background(Color.Transparent, RoundedCornerShape(9.dp))) {
        if (visible.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxHeight().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(9.dp)))
        }
        visible.forEach { (count, color) ->
            Box(Modifier.weight(count.toFloat()).fillMaxHeight().background(color))
        }
    }
}
