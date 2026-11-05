package dev.kosh.financetracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Hand-drawn bar-chart glyph — material-icons-core has no BarChart/Insights icon. */
@Composable
fun InsightsIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(24.dp)) {
        val barWidth = size.width * 0.2f
        val gap = size.width * 0.14f
        val baseline = size.height * 0.92f
        val heights = listOf(0.45f, 0.85f, 0.62f)

        heights.forEachIndexed { index, heightFraction ->
            val barHeight = size.height * heightFraction
            val x = index * (barWidth + gap)
            drawRoundRect(
                color = tint,
                topLeft = Offset(x, baseline - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth * 0.3f, barWidth * 0.3f),
            )
        }
    }
}
