package dev.kosh.financetracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.kosh.financetracker.core.finance.DaySpend
import java.math.BigDecimal

/**
 * Small decorative trend indicator for Home — real daily totals, last N days, no
 * interactivity (the full interactive chart belongs to the Spending/Insights screen,
 * a later pass). Renders nothing meaningful — and is announced as such — when there
 * isn't enough real data to imply a trend.
 */
@Composable
fun SpendSparkline(
    days: List<DaySpend>,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val recent = days.takeLast(7)
    val maxAmount = recent.maxOfOrNull { it.amount } ?: BigDecimal.ZERO

    Canvas(
        modifier = modifier
            .size(width = 40.dp, height = 56.dp)
            .semantics {
                contentDescription = if (recent.size >= 2) {
                    "Spending trend over the last ${recent.size} days"
                } else {
                    "Not enough data yet for a spending trend"
                }
            },
    ) {
        if (recent.isEmpty() || maxAmount <= BigDecimal.ZERO) return@Canvas

        val barCount = recent.size
        val gap = 4.dp.toPx()
        val barWidth = (size.width - gap * (barCount - 1)) / barCount
        val maxBarHeight = size.height

        recent.forEachIndexed { index, day ->
            val fraction = (day.amount.toFloat() / maxAmount.toFloat()).coerceIn(0.08f, 1f)
            val barHeight = maxBarHeight * fraction
            val x = index * (barWidth + gap)
            drawRoundRect(
                color = color,
                topLeft = androidx.compose.ui.geometry.Offset(x, maxBarHeight - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2, barWidth / 2),
            )
        }
    }
}
