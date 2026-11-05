package dev.kosh.financetracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.kosh.financetracker.core.finance.DaySpend
import java.math.BigDecimal

/**
 * Daily spend bars for a full month — real per-day totals only, no interpolation.
 * A day with no spending draws as a flat baseline, which is the honest reading
 * (not "no data", it's "₹0 that day").
 */
@Composable
fun SpendTrendChart(
    days: List<DaySpend>,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val maxAmount = days.maxOfOrNull { it.amount } ?: BigDecimal.ZERO

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .semantics {
                contentDescription = if (days.size >= 2) {
                    "Daily spending for the month, ${days.size} days of data"
                } else {
                    "Not enough daily data yet to show a trend"
                }
            },
    ) {
        if (days.isEmpty() || maxAmount <= BigDecimal.ZERO) return@Canvas

        val barCount = days.size
        val gap = if (barCount > 20) 1.dp.toPx() else 3.dp.toPx()
        val barWidth = ((size.width - gap * (barCount - 1)) / barCount).coerceAtLeast(1f)
        val maxBarHeight = size.height

        days.forEachIndexed { index, day ->
            val fraction = (day.amount.toFloat() / maxAmount.toFloat()).coerceIn(0.02f, 1f)
            val barHeight = maxBarHeight * fraction
            val x = index * (barWidth + gap)
            drawRoundRect(
                color = color,
                topLeft = Offset(x, maxBarHeight - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2),
            )
        }
    }
}
