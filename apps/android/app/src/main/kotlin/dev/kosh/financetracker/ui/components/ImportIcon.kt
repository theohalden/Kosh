package dev.kosh.financetracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

/** Hand-drawn "import" glyph — an arrow rising into a tray, consistent stroke
 * weight with the other hand-drawn icons (CalendarIcon, ChevronDownIcon). */
@Composable
fun ImportIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val strokeWidth = 1.4.dp.toPx()
        val cx = size.width / 2f
        val arrowTop = size.height * 0.1f
        val arrowBottom = size.height * 0.55f
        val armSpan = size.width * 0.2f

        drawLine(tint, Offset(cx, arrowTop), Offset(cx, arrowBottom), strokeWidth, cap = StrokeCap.Round)
        drawLine(tint, Offset(cx - armSpan, arrowTop + armSpan), Offset(cx, arrowTop), strokeWidth, cap = StrokeCap.Round)
        drawLine(tint, Offset(cx + armSpan, arrowTop + armSpan), Offset(cx, arrowTop), strokeWidth, cap = StrokeCap.Round)

        val trayTop = size.height * 0.72f
        val trayLeft = size.width * 0.14f
        val trayRight = size.width * 0.86f
        val trayBottom = size.height * 0.92f
        drawLine(tint, Offset(trayLeft, trayTop), Offset(trayLeft, trayBottom), strokeWidth, cap = StrokeCap.Round)
        drawLine(tint, Offset(trayRight, trayTop), Offset(trayRight, trayBottom), strokeWidth, cap = StrokeCap.Round)
        drawLine(tint, Offset(trayLeft, trayBottom), Offset(trayRight, trayBottom), strokeWidth, cap = StrokeCap.Round)
    }
}
