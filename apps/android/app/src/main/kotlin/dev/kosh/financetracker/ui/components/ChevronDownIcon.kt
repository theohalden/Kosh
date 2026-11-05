package dev.kosh.financetracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

/** Small hand-drawn chevron — consistent stroke weight with CalendarIcon/VisibilityToggle,
 * used instead of a plain "⌄" text glyph which renders inconsistently across fonts. */
@Composable
fun ChevronDownIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(10.dp)) {
        val strokeWidth = 1.4.dp.toPx()
        val left = size.width * 0.1f
        val right = size.width * 0.9f
        val midY = size.height * 0.68f
        val topY = size.height * 0.28f

        drawLine(
            color = tint,
            start = Offset(left, topY),
            end = Offset(size.width / 2f, midY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint,
            start = Offset(size.width / 2f, midY),
            end = Offset(right, topY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}
