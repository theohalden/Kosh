package dev.kosh.financetracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.kosh.financetracker.ui.theme.KoshColors
import dev.kosh.financetracker.ui.theme.KoshExtendedTheme
import dev.kosh.financetracker.ui.theme.Spacing
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * Modern month picker — a grid grouped by year, replacing the plain flat
 * [androidx.compose.material3.DropdownMenu] list (unstyled, no way to tell which
 * months actually have data, selected month not visually distinguished, and it
 * rendered as a tiny popup overlapping the content behind it).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthPickerSheet(
    selectedMonth: YearMonth,
    availableMonths: List<YearMonth>,
    monthsWithData: Set<YearMonth>,
    onSelectMonth: (YearMonth) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val byYear = availableMonths.groupBy { it.year }.toSortedMap(compareByDescending { it })

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = KoshColors.Graphite,
    ) {
        Column(modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)) {
            Text(
                "Select month",
                style = MaterialTheme.typography.titleMedium,
                color = KoshColors.PrimaryText,
            )
            Spacer(Modifier.height(Spacing.md))

            byYear.forEach { (year, monthsInYear) ->
                Text(
                    year.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = KoshColors.SecondaryText,
                )
                Spacer(Modifier.height(Spacing.sm))

                monthsInYear.sortedBy { it.monthValue }.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        row.forEach { month ->
                            MonthCell(
                                month = month,
                                selected = month == selectedMonth,
                                hasData = month in monthsWithData,
                                onClick = {
                                    onSelectMonth(month)
                                    onDismiss()
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(Spacing.sm))
                }
                Spacer(Modifier.height(Spacing.sm))
            }
            Spacer(Modifier.height(Spacing.md))
        }
    }
}

@Composable
private fun MonthCell(
    month: YearMonth,
    selected: Boolean,
    hasData: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = month.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    val background = if (selected) KoshExtendedTheme.colors.accentText.copy(alpha = 0.18f) else KoshColors.RaisedGraphite.copy(alpha = 0.6f)
    val textColor = when {
        selected -> KoshExtendedTheme.colors.accentText
        hasData -> KoshColors.PrimaryText
        else -> KoshColors.SecondaryText.copy(alpha = 0.6f)
    }

    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
            if (hasData) {
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (selected) KoshExtendedTheme.colors.accentText else KoshColors.SecondaryText),
                )
            }
        }
    }
}
