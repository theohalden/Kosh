package dev.kosh.financetracker.feature.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.kosh.financetracker.core.model.Transaction
import dev.kosh.financetracker.core.model.TransactionCategory
import dev.kosh.financetracker.core.model.TransactionDirection
import dev.kosh.financetracker.core.model.TransactionType
import dev.kosh.financetracker.core.model.displayLabel
import dev.kosh.financetracker.core.parser.ParserFields
import dev.kosh.financetracker.ui.components.CategoryIconTile
import dev.kosh.financetracker.ui.components.REVIEW_PICKER_CATEGORIES
import dev.kosh.financetracker.ui.components.labelFor
import dev.kosh.financetracker.ui.theme.KoshExtendedTheme
import dev.kosh.financetracker.ui.theme.Spacing
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    onBack: () -> Unit,
    viewModel: TransactionDetailViewModel = hiltViewModel(),
) {
    val transaction by viewModel.transaction.collectAsState()
    var tabIndex by remember { mutableIntStateOf(0) }
    var editingCategory by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Transaction story") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        val current = transaction
        if (current == null) {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {}
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.md),
        ) {
            val isExpense = current.direction == TransactionDirection.DEBIT
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "₹${current.amount}",
                    style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = "tnum"),
                    color = when (current.type) {
                        TransactionType.CREDIT_CARD_PAYMENT, TransactionType.TRANSFER -> MaterialTheme.colorScheme.onSurfaceVariant
                        else -> if (isExpense) KoshExtendedTheme.colors.expense else KoshExtendedTheme.colors.income
                    },
                )
            }
            Text(
                current.timestamp.atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("d MMM yyyy · h:mm a")),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                current.merchant ?: "Unknown",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            current.accountSuffix?.let {
                Text(
                    "${current.bank ?: "Account"} •••• $it",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(Spacing.md))

            TabRow(
                selectedTabIndex = tabIndex,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = KoshExtendedTheme.colors.accentText,
            ) {
                Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }, text = { Text("How Kosh understood this") })
                Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }, text = { Text("Source & details") })
            }

            Spacer(Modifier.height(Spacing.md))

            if (tabIndex == 0) {
                UnderstandingTab(current)
            } else {
                DetailsTab(
                    transaction = current,
                    editingCategory = editingCategory,
                    onEditCategoryClick = { editingCategory = !editingCategory },
                    onCategorySelected = {
                        viewModel.setCategory(it)
                        editingCategory = false
                    },
                )
            }
        }
    }
}

@Composable
private fun UnderstandingTab(transaction: Transaction) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        item {
            TimelineStep(
                title = "SMS captured",
                subtitle = transaction.timestamp.atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("d MMM, h:mm a")),
                body = transaction.rawSourceText ?: "No source message stored for this entry.",
            )
        }
        item {
            TimelineStep(
                title = "Identified as ${transaction.type.displayLabel().lowercase()}",
                subtitle = null,
                body = if (dev.kosh.financetracker.core.finance.ConfirmedClassification.type(transaction) != null) {
                    transaction.notes.orEmpty()
                } else when (transaction.type) {
                    TransactionType.CREDIT_CARD_PURCHASE -> "The issuer confirms a completed card purchase. It counts as spending on the purchase date."
                    TransactionType.CREDIT_CARD_PAYMENT -> "This settles a credit card balance. It is neither new spending nor income; the purchase is counted separately."
                    TransactionType.TRANSFER -> "Classified as a transfer and excluded from spending and income."
                    TransactionType.UNKNOWN -> "The purpose of this movement is unresolved. A credit alone does not establish income."
                    else -> "Classified from the stored transaction and available source evidence."
                },
            )
        }
        item {
            TimelineStep(
                title = "Categorized as ${labelFor(transaction.category ?: TransactionCategory.UNCATEGORIZED)}",
                subtitle = null,
                body = if (transaction.merchant != null) {
                    "Matched from the merchant name (\"${transaction.merchant}\"). You can change this anytime."
                } else {
                    "No merchant name was found in the message, so this couldn't be auto-categorized."
                },
            )
        }
    }
}

@Composable
private fun TimelineStep(title: String, subtitle: String?, body: String) {
    Column {
        Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.xs),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = MaterialTheme.shapes.medium,
        ) {
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(Spacing.ms),
            )
        }
    }
}

@Composable
private fun DetailsTab(
    transaction: Transaction,
    editingCategory: Boolean,
    onEditCategoryClick: () -> Unit,
    onCategorySelected: (TransactionCategory) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.ms)) {
        DetailFieldRow("Transaction type", transaction.type.displayLabel())
        DetailFieldRow("Category", labelFor(transaction.category ?: TransactionCategory.UNCATEGORIZED))
        DetailFieldRow("Account", transaction.accountSuffix?.let { "${transaction.bank ?: "Account"} •••• $it" } ?: "Unknown")
        ParserFields.cardSuffix(transaction.rawSourceText.orEmpty())?.let { DetailFieldRow("Card", "•••• $it") }
        DetailFieldRow("Source", transaction.source.name)
        transaction.reconciliationNote?.let { DetailFieldRow("Reconciliation", it) }
        DetailFieldRow(
            "Reference time",
            transaction.timestamp.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a")),
        )

        Spacer(Modifier.height(Spacing.sm))

        OutlinedButton(onClick = onEditCategoryClick, modifier = Modifier.fillMaxWidth()) {
            Text("Edit category")
        }

        if (editingCategory) {
            Spacer(Modifier.height(Spacing.sm))
            REVIEW_PICKER_CATEGORIES.forEach { category ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCategorySelected(category) }
                        .padding(vertical = Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CategoryIconTile(category, labelFor(category), size = 28.dp)
                    Spacer(Modifier.width(Spacing.ms))
                    Text(labelFor(category), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
private fun DetailFieldRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
    }
}
