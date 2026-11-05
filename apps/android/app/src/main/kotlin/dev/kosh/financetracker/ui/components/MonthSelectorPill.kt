package dev.kosh.financetracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.kosh.financetracker.ui.theme.KoshColors
import dev.kosh.financetracker.ui.theme.Spacing
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/** The tappable month pill (calendar icon + label + chevron) plus the
 * [MonthPickerSheet] it opens — shared by Home and Spending so both screens'
 * month selection looks and behaves identically. */
@Composable
fun MonthSelectorPill(
    selectedMonth: YearMonth,
    availableMonths: List<YearMonth>,
    monthsWithData: Set<YearMonth>,
    onSelectMonth: (YearMonth) -> Unit,
) {
    var sheetOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(KoshColors.RaisedGraphite.copy(alpha = 0.7f))
            .clickable { sheetOpen = true }
            .padding(horizontal = Spacing.ms, vertical = Spacing.sm)
            .semantics { contentDescription = "Selected month: ${monthLabel(selectedMonth)}. Tap to change." },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CalendarIcon(tint = KoshColors.SecondaryText, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(Spacing.xs))
        Text(monthLabel(selectedMonth), style = MaterialTheme.typography.bodyMedium, color = KoshColors.PrimaryText)
        Spacer(Modifier.width(Spacing.xs))
        ChevronDownIcon(tint = KoshColors.SecondaryText)
    }

    if (sheetOpen) {
        MonthPickerSheet(
            selectedMonth = selectedMonth,
            availableMonths = availableMonths,
            monthsWithData = monthsWithData,
            onSelectMonth = onSelectMonth,
            onDismiss = { sheetOpen = false },
        )
    }
}

private fun monthLabel(month: YearMonth): String =
    month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
