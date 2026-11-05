package dev.kosh.financetracker.feature.review

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import dev.kosh.financetracker.core.model.Transaction
import dev.kosh.financetracker.core.model.TransactionCategory
import dev.kosh.financetracker.ui.components.CategoryIconTile
import dev.kosh.financetracker.ui.components.EmptyState
import dev.kosh.financetracker.ui.components.KoshCard
import dev.kosh.financetracker.ui.components.KoshWordmark
import dev.kosh.financetracker.ui.components.REVIEW_PICKER_CATEGORIES
import dev.kosh.financetracker.ui.components.labelFor
import dev.kosh.financetracker.ui.format.formatRupees
import dev.kosh.financetracker.ui.theme.KoshColors
import dev.kosh.financetracker.ui.theme.KoshExtendedTheme
import dev.kosh.financetracker.ui.theme.Spacing
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ReviewScreen(viewModel: ReviewViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KoshColors.BaseBlack)
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.md),
    ) {
        Spacer(Modifier.height(Spacing.sm))
        KoshWordmark()
        Spacer(Modifier.height(Spacing.md))
        Text("Review", style = MaterialTheme.typography.titleLarge, color = KoshColors.PrimaryText)
        Text(
            "Help us categorize a few transactions",
            style = MaterialTheme.typography.bodyMedium,
            color = KoshColors.SecondaryText,
        )
        Spacer(Modifier.height(Spacing.lg))

        val current = state.current
        if (current == null) {
            EmptyState(
                title = "All caught up",
                description = "Nothing needs your review right now.",
            )
            Spacer(Modifier.height(Spacing.xxl))
        } else {
            ReviewCard(
                transaction = current,
                position = state.position,
                total = state.queue.size,
                selectedCategory = state.selectedCategory,
                onPrevious = viewModel::goToPrevious,
                onNext = viewModel::goToNext,
                onSelectCategory = viewModel::selectCategory,
                onConfirm = viewModel::confirmSelection,
            )
            // Extra bottom padding so "Confirm type" clears the bottom nav bar
            // instead of sitting flush against it.
            Spacer(Modifier.height(Spacing.xxl + Spacing.xxl))
        }
    }
}

@Composable
private fun ReviewCard(
    transaction: Transaction,
    position: Int,
    total: Int,
    selectedCategory: TransactionCategory?,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSelectCategory: (TransactionCategory) -> Unit,
    onConfirm: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${position + 1} of $total",
                style = MaterialTheme.typography.labelMedium,
                color = KoshColors.SecondaryText,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                NavArrow(direction = -1, enabled = position > 0, onClick = onPrevious)
                NavArrow(direction = 1, enabled = position < total - 1, onClick = onNext)
            }
        }

        Text(
            formatRupees(transaction.amount),
            style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = "tnum"),
            color = KoshColors.PrimaryText,
            modifier = Modifier.padding(top = Spacing.sm),
        )
        Text(
            transaction.timestamp.atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("d MMM yyyy · h:mm a")),
            style = MaterialTheme.typography.labelMedium,
            color = KoshColors.SecondaryText,
        )

        Text(
            "We couldn't confidently categorize this transaction. It looks like a " +
                "${transaction.paymentMethod?.name?.lowercase() ?: "card"} payment, but the merchant is unclear.",
            style = MaterialTheme.typography.bodyMedium,
            color = KoshColors.SecondaryText,
            modifier = Modifier.padding(top = Spacing.md, bottom = Spacing.md),
        )

        if (!transaction.rawSourceText.isNullOrBlank()) {
            Text(
                "Source message (from bank SMS)",
                style = MaterialTheme.typography.labelSmall,
                color = KoshColors.SecondaryText,
            )
            KoshCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.xs, bottom = Spacing.md),
                containerColor = KoshColors.RaisedGraphite,
                cornerRadius = 16.dp,
            ) {
                Text(
                    transaction.rawSourceText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = KoshColors.PrimaryText,
                    modifier = Modifier.padding(Spacing.ms),
                )
            }
        }

        Text(
            "What type of transaction is this?",
            style = MaterialTheme.typography.titleMedium,
            color = KoshColors.PrimaryText,
            modifier = Modifier.padding(bottom = Spacing.sm),
        )

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            REVIEW_PICKER_CATEGORIES.forEach { category ->
                CategoryOptionRow(
                    category = category,
                    selected = selectedCategory == category,
                    onClick = { onSelectCategory(category) },
                )
            }
        }

        Spacer(Modifier.height(Spacing.lg))

        Button(
            onClick = onConfirm,
            enabled = selectedCategory != null,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = KoshExtendedTheme.colors.accentText,
                contentColor = KoshColors.BaseBlack,
                disabledContainerColor = KoshColors.RaisedGraphite,
                disabledContentColor = KoshColors.SecondaryText,
            ),
            shape = RoundedCornerShape(14.dp),
        ) {
            Text("Confirm type", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun NavArrow(direction: Int, enabled: Boolean, onClick: () -> Unit) {
    val tint = if (enabled) KoshColors.PrimaryText else KoshColors.SecondaryText.copy(alpha = 0.4f)
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(KoshColors.RaisedGraphite.copy(alpha = 0.6f))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (direction < 0) "‹" else "›",
            style = MaterialTheme.typography.titleMedium,
            color = tint,
        )
    }
}

@Composable
private fun CategoryOptionRow(
    category: TransactionCategory,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) KoshExtendedTheme.colors.accentText else KoshColors.Divider
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) KoshExtendedTheme.colors.accentText.copy(alpha = 0.12f) else KoshColors.Graphite)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.ms, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryIconTile(category, labelFor(category), size = 28.dp)
        Spacer(Modifier.width(Spacing.ms))
        Text(
            labelFor(category),
            style = MaterialTheme.typography.bodyLarge,
            color = KoshColors.PrimaryText,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .border(1.5.dp, if (selected) KoshExtendedTheme.colors.accentText else KoshColors.SecondaryText, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(KoshExtendedTheme.colors.accentText),
                )
            }
        }
    }
}
