package dev.kosh.financetracker.feature.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.kosh.financetracker.core.finance.CategorySlice
import dev.kosh.financetracker.ui.components.CategoryIconTile
import dev.kosh.financetracker.ui.components.EmptyState
import dev.kosh.financetracker.ui.components.KoshCard
import dev.kosh.financetracker.ui.components.KoshWordmark
import dev.kosh.financetracker.ui.components.MonthSelectorPill
import dev.kosh.financetracker.ui.components.SpendTrendChart
import dev.kosh.financetracker.ui.components.colorFor
import dev.kosh.financetracker.ui.components.labelFor
import dev.kosh.financetracker.ui.format.formatRupees
import dev.kosh.financetracker.ui.theme.KoshColors
import dev.kosh.financetracker.ui.theme.KoshExtendedTheme
import dev.kosh.financetracker.ui.theme.Spacing
import java.time.YearMonth
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun InsightsScreen(
    viewModel: InsightsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KoshColors.BaseBlack),
    ) {
        if (!state.hasAnyData) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = Spacing.md),
            ) {
                Spacer(Modifier.height(Spacing.sm))
                KoshWordmark()
                EmptyState(
                    title = "No spending yet",
                    description = "Once Kosh reads some bank SMS, your spending trend and category breakdown will show up here.",
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = Spacing.sm,
                    bottom = Spacing.xxl,
                ),
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .padding(horizontal = Spacing.md),
                    ) {
                        InsightsHeader(
                            selectedMonth = state.selectedMonth,
                            availableMonths = state.availableMonths,
                            monthsWithData = state.monthsWithData,
                            onSelectMonth = viewModel::selectMonth,
                        )
                        Spacer(Modifier.height(Spacing.lg))
                        SpendingSummary(
                            expense = state.monthSummary.expense,
                            percentChange = state.percentChangeVsPreviousMonth,
                        )
                        Spacer(Modifier.height(Spacing.lg))
                        SpendTrendChart(
                            days = state.dailySpend,
                            color = KoshExtendedTheme.colors.accentText,
                        )
                        Spacer(Modifier.height(Spacing.lg))
                        Text(
                            "Where it went",
                            style = MaterialTheme.typography.titleMedium,
                            color = KoshColors.PrimaryText,
                        )
                        Spacer(Modifier.height(Spacing.sm))
                    }
                }

                if (state.categoryBreakdown.isEmpty()) {
                    item {
                        Text(
                            "No categorized spending this month yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = KoshColors.SecondaryText,
                            modifier = Modifier.padding(horizontal = Spacing.md),
                        )
                    }
                } else {
                    item {
                        CategoryBreakdownCard(
                            slices = state.categoryBreakdown,
                            modifier = Modifier.padding(horizontal = Spacing.md),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightsHeader(
    selectedMonth: YearMonth,
    availableMonths: List<YearMonth>,
    monthsWithData: Set<YearMonth>,
    onSelectMonth: (YearMonth) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            KoshWordmark()
            Spacer(Modifier.height(Spacing.xs))
            Text("Spending", style = MaterialTheme.typography.titleLarge, color = KoshColors.PrimaryText)
        }

        MonthSelectorPill(
            selectedMonth = selectedMonth,
            availableMonths = availableMonths,
            monthsWithData = monthsWithData,
            onSelectMonth = onSelectMonth,
        )
    }
}

@Composable
private fun SpendingSummary(expense: java.math.BigDecimal, percentChange: Float?) {
    Column {
        Text("Spent this month", style = MaterialTheme.typography.bodyMedium, color = KoshColors.SecondaryText)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                formatRupees(expense),
                style = MaterialTheme.typography.displayLarge.copy(fontFeatureSettings = "tnum"),
                color = KoshColors.PrimaryText,
            )
            if (percentChange != null) {
                Spacer(Modifier.width(Spacing.sm))
                PercentChangeBadge(percentChange)
            }
        }
    }
}

@Composable
private fun PercentChangeBadge(percentChange: Float) {
    val isIncrease = percentChange > 0f
    val color = if (isIncrease) KoshExtendedTheme.colors.expense else KoshExtendedTheme.colors.income
    val arrow = if (isIncrease) "▲" else "▼"
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
    ) {
        Text(
            "$arrow ${abs((percentChange * 100).roundToInt())}%",
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun CategoryBreakdownCard(slices: List<CategorySlice>, modifier: Modifier = Modifier) {
    KoshCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            slices.forEachIndexed { index, slice ->
                CategoryRow(slice)
                if (index != slices.lastIndex) {
                    Spacer(Modifier.height(Spacing.ms))
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(slice: CategorySlice) {
    val color = colorFor(slice.category)
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryIconTile(slice.category, labelFor(slice.category), size = 32.dp)
                Spacer(Modifier.width(Spacing.sm))
                Text(labelFor(slice.category), style = MaterialTheme.typography.bodyMedium, color = KoshColors.PrimaryText)
            }
            Text(
                formatRupees(slice.amount),
                style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                color = KoshColors.PrimaryText,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(Spacing.xs))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(KoshColors.Divider),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(slice.fraction.coerceIn(0f, 1f))
                    .height(4.dp)
                    .background(color, RoundedCornerShape(2.dp)),
            )
        }
    }
}
