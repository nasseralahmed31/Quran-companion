package com.qurancompanion.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Translucent rounded card with a soft border (glass-style without costly blur). */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    borderColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    val container = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    val border = BorderStroke(1.dp, borderColor)
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier, shape = shape, color = container, border = border) {
            Column(Modifier.padding(16.dp), content = content)
        }
    } else {
        Surface(modifier = modifier, shape = shape, color = container, border = border) {
            Column(Modifier.padding(16.dp), content = content)
        }
    }
}
