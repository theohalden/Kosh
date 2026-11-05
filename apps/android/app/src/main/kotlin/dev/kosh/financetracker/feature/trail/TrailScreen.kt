package dev.kosh.financetracker.feature.trail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.kosh.financetracker.core.model.Transaction
import dev.kosh.financetracker.core.model.TransactionDirection
import dev.kosh.financetracker.core.model.TransactionType
import dev.kosh.financetracker.ui.components.CategoryIconTile
import dev.kosh.financetracker.ui.components.EmptyState
import dev.kosh.financetracker.ui.components.KoshWordmark
import dev.kosh.financetracker.ui.format.formatRupees
import dev.kosh.financetracker.ui.theme.KoshColors
import dev.kosh.financetracker.ui.theme.KoshExtendedTheme
import dev.kosh.financetracker.ui.theme.Spacing
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun TrailScreen(
    onOpenTransaction: (Long) -> Unit,
    viewModel: TrailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val zone = ZoneId.systemDefault()
    val grouped = remember(state.transactions) {
        state.transactions.groupBy { it.timestamp.atZone(zone).toLocalDate() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KoshColors.BaseBlack)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = Spacing.md),
    ) {
        Spacer(Modifier.height(Spacing.sm))
        KoshWordmark()
        Spacer(Modifier.height(Spacing.md))
        Text("Activity", style = MaterialTheme.typography.titleLarge, color = KoshColors.PrimaryText)
        Text(
            "All transactions from your bank SMS",
            style = MaterialTheme.typography.bodyMedium,
            color = KoshColors.SecondaryText,
        )
        Spacer(Modifier.height(Spacing.md))

        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::setQuery,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search descriptions, amounts or accounts", color = KoshColors.SecondaryText) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = KoshColors.SecondaryText) },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = KoshColors.RaisedGraphite.copy(alpha = 0.7f),
                focusedContainerColor = KoshColors.RaisedGraphite.copy(alpha = 0.7f),
                unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                focusedBorderColor = KoshExtendedTheme.colors.accentText,
                focusedTextColor = KoshColors.PrimaryText,
                unfocusedTextColor = KoshColors.PrimaryText,
                cursorColor = KoshExtendedTheme.colors.accentText,
            ),
        )
        Spacer(Modifier.height(Spacing.sm))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            FilterChipRow(state.filter, viewModel::setFilter)
        }
        Spacer(Modifier.height(Spacing.sm))

        if (state.transactions.isEmpty()) {
            EmptyState(
                title = "No transactions found",
                description = "Try a different search or filter.",
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = Spacing.xxl),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                grouped.entries.sortedByDescending { it.key }.forEach { (date, dayTransactions) ->
                    item { DateHeader(date) }
                    items(dayTransactions, key = { it.id }) { transaction ->
                        TrailRow(transaction, onClick = { onOpenTransaction(transaction.id) })
                        HorizontalDivider(color = KoshColors.Divider, thickness = 1.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChipRow(selected: TrailFilter, onSelect: (TrailFilter) -> Unit) {
    val options = listOf(
        TrailFilter.ALL to "All",
        TrailFilter.EXPENSES to "Spending",
        TrailFilter.INCOME to "Income",
        TrailFilter.TRANSFERS to "Transfers",
    )
    options.forEach { (filter, label) ->
        val isSelected = selected == filter
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(if (isSelected) KoshExtendedTheme.colors.accentText.copy(alpha = 0.16f) else KoshColors.RaisedGraphite.copy(alpha = 0.6f))
                .clickable { onSelect(filter) }
                .padding(horizontal = Spacing.ms, vertical = Spacing.sm),
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = if (isSelected) KoshExtendedTheme.colors.accentText else KoshColors.SecondaryText,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }
}

@Composable
private fun DateHeader(date: LocalDate) {
    val dayLabel = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase()
    val formatted = date.format(DateTimeFormatter.ofPattern("d MMM").withLocale(Locale.getDefault())).uppercase()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.sm),
    ) {
        Text(
            "$formatted $dayLabel",
            style = MaterialTheme.typography.labelMedium,
            color = KoshColors.SecondaryText,
        )
    }
}

@Composable
private fun TrailRow(transaction: Transaction, onClick: () -> Unit) {
    val isTransfer = transaction.type == TransactionType.TRANSFER || transaction.type == TransactionType.CREDIT_CARD_PAYMENT
    val isExpense = transaction.direction == TransactionDirection.DEBIT

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryIconTile(transaction.category, transaction.merchant ?: "?", size = 36.dp)
        Spacer(Modifier.width(Spacing.ms))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                transaction.merchant ?: "Unknown",
                style = MaterialTheme.typography.bodyLarge,
                color = KoshColors.PrimaryText,
            )
            Text(
                if (transaction.reconciliationNote != null) {
                    if (transaction.isUnmatchedOverlap) "Unmatched record · Excluded from totals"
                    else "Purpose unconfirmed · Excluded from spending"
                } else if (transaction.type == TransactionType.CREDIT_CARD_PAYMENT) {
                    "Credit card repayment · Not spending or income"
                } else if (transaction.type == TransactionType.CREDIT_CARD_PURCHASE) {
                    "Credit card purchase · ${accountSubtitle(transaction)}"
                } else if (isTransfer) {
                    "Transfer · Excluded from spending"
                } else {
                    accountSubtitle(transaction)
                },
                style = MaterialTheme.typography.labelMedium,
                color = if (isTransfer) KoshExtendedTheme.colors.comparison else KoshColors.SecondaryText,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            val sign = if (isTransfer) "" else if (isExpense) "-" else "+"
            val amountColor = when {
                isTransfer -> KoshColors.SecondaryText
                isExpense -> KoshExtendedTheme.colors.expense
                else -> KoshExtendedTheme.colors.income
            }
            Text(
                "$sign${formatRupees(transaction.amount)}",
                style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.SemiBold,
                color = amountColor,
            )
            Text(
                transaction.timestamp.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("h:mm a")),
                style = MaterialTheme.typography.labelSmall,
                color = KoshColors.SecondaryText,
            )
        }
    }
}

private fun accountSubtitle(transaction: Transaction): String {
    val maskedAccount = transaction.accountSuffix?.let { suffix ->
        val bank = transaction.bank
        if (bank != null) "$bank ••$suffix" else "••$suffix"
    }
    val parts = listOfNotNull(
        maskedAccount ?: transaction.paymentMethod?.name,
        transaction.source.name,
    )
    return parts.joinToString(" · ")
}
