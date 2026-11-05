package dev.kosh.financetracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** Hand-drawn to avoid pulling in material-icons-extended for one glyph. */
@Composable
fun CalendarIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val strokeWidth = 1.4.dp.toPx()
        val inset = size.width * 0.14f
        val bodyTop = size.height * 0.22f

        drawRoundRect(
            color = tint,
            topLeft = Offset(inset, bodyTop),
            size = Size(size.width - inset * 2, size.height - bodyTop - inset * 0.4f),
            cornerRadius = CornerRadius(size.width * 0.12f, size.width * 0.12f),
            style = Stroke(width = strokeWidth),
        )
        // Binder rings
        drawLine(tint, Offset(size.width * 0.32f, bodyTop * 0.4f), Offset(size.width * 0.32f, bodyTop * 1.3f), strokeWidth)
        drawLine(tint, Offset(size.width * 0.68f, bodyTop * 0.4f), Offset(size.width * 0.68f, bodyTop * 1.3f), strokeWidth)
        // Header divider
        drawLine(
            tint,
            Offset(inset, bodyTop + size.height * 0.16f),
            Offset(size.width - inset, bodyTop + size.height * 0.16f),
            strokeWidth,
        )
    }
}
