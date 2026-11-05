package dev.kosh.financetracker.feature.overview

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import dev.kosh.financetracker.core.finance.CategorySlice
import dev.kosh.financetracker.core.finance.DaySpend
import dev.kosh.financetracker.core.finance.MonthSummary
import dev.kosh.financetracker.core.model.Transaction
import dev.kosh.financetracker.core.model.TransactionDirection
import dev.kosh.financetracker.core.model.TransactionType
import dev.kosh.financetracker.feature.importstatement.StatementImportDialog
import dev.kosh.financetracker.feature.importstatement.StatementImportViewModel
import dev.kosh.financetracker.ui.components.AccountCard
import dev.kosh.financetracker.ui.components.CategoryIconTile
import dev.kosh.financetracker.ui.components.ImportIcon
import dev.kosh.financetracker.ui.components.KoshCard
import dev.kosh.financetracker.ui.components.KoshWordmark
import dev.kosh.financetracker.ui.components.MonthSelectorPill
import dev.kosh.financetracker.ui.components.SpendSparkline
import dev.kosh.financetracker.ui.components.VisibilityToggle
import dev.kosh.financetracker.ui.format.formatRupees
import dev.kosh.financetracker.ui.theme.KoshColors
import dev.kosh.financetracker.ui.theme.KoshExtendedTheme
import dev.kosh.financetracker.ui.theme.Spacing
import java.math.BigDecimal
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun OverviewScreen(
    onOpenTransaction: (Long) -> Unit,
    onOpenReview: () -> Unit,
    onSeeAllActivity: () -> Unit,
    onOpenInsights: () -> Unit,
    viewModel: OverviewViewModel = hiltViewModel(),
    importViewModel: StatementImportViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val importState by importViewModel.uiState.collectAsState()
    var amountsHidden by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val fileName = queryDisplayName(context, uri) ?: uri.lastPathSegment ?: "statement"
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        if (bytes != null) importViewModel.onFileSelected(fileName, bytes)
    }

    fun hasAllSmsPermissions(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
            PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) ==
            PackageManager.PERMISSION_GRANTED

    var hasPermission by remember { mutableStateOf(hasAllSmsPermissions()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        hasPermission = hasAllSmsPermissions()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KoshColors.BaseBlack),
    ) {
        when {
            !hasPermission && !state.hasAnyData -> PermissionGate(
                onGrant = {
                    permissionLauncher.launch(arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS))
                },
            )

            syncState.isSyncing && !state.hasAnyData -> LoadingState()

            !state.hasAnyData -> EmptyHomeState(onScan = viewModel::sync)

            else -> HomeContent(
                state = state,
                onSmsScan = {
                    if (hasPermission) viewModel.sync()
                    else permissionLauncher.launch(arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS))
                },
                isSyncing = syncState.isSyncing,
                syncSummary = syncState.error ?: if (syncState.hasSyncedOnce) {
                    "SMS scan: ${syncState.lastScannedCount} checked, ${syncState.lastImportedCount} added, " +
                        "${syncState.lastAlreadyRepresentedCount} already represented. " +
                        "${syncState.lastUnparsedCount} financial-looking messages could not be parsed; " +
                        "${syncState.lastUnknownDirectionCount} had no clear debit or credit."
                } else null,
                amountsHidden = amountsHidden,
                onToggleAmountsHidden = { amountsHidden = !amountsHidden },
                onSelectMonth = viewModel::selectMonth,
                onOpenTransaction = onOpenTransaction,
                onOpenReview = onOpenReview,
                onSeeAllActivity = onSeeAllActivity,
                onOpenInsights = onOpenInsights,
                onImportClick = {
                    filePickerLauncher.launch(
                        arrayOf(
                            "text/csv",
                            "text/comma-separated-values",
                            "application/vnd.ms-excel",
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            "*/*",
                        ),
                    )
                },
            )
        }

        StatementImportDialog(state = importState, onConfirm = importViewModel::confirmImport, onDismiss = importViewModel::dismiss)
    }
}

private fun queryDisplayName(context: android.content.Context, uri: android.net.Uri): String? =
    context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    }

@Composable
private fun PermissionGate(onGrant: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Spacer(Modifier.height(Spacing.lg))
        KoshWordmark()
        Spacer(Modifier.height(Spacing.lg))
        Text("Kosh needs SMS access", style = MaterialTheme.typography.titleLarge, color = KoshColors.PrimaryText)
        Text(
            "Kosh reads your bank SMS on-device to build your transaction history. Nothing leaves your phone.",
            style = MaterialTheme.typography.bodyMedium,
            color = KoshColors.SecondaryText,
        )
        Button(
            onClick = onGrant,
            colors = ButtonDefaults.buttonColors(containerColor = KoshExtendedTheme.colors.accentText, contentColor = KoshColors.BaseBlack),
        ) {
            Text("Grant SMS access")
        }
    }
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        KoshWordmark()
        Spacer(Modifier.height(Spacing.xxl))
        CircularProgressIndicator(color = KoshExtendedTheme.colors.accentText)
        Text("Reading your bank SMS…", style = MaterialTheme.typography.bodyMedium, color = KoshColors.SecondaryText)
    }
}

@Composable
private fun EmptyHomeState(onScan: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        KoshWordmark()
        Spacer(Modifier.height(Spacing.xxl))
        Text("No transactions yet", style = MaterialTheme.typography.titleLarge, color = KoshColors.PrimaryText)
        Text(
            "Scan your SMS history to find bank transactions. Opening Kosh does not rescan or change your existing history.",
            style = MaterialTheme.typography.bodyMedium,
            color = KoshColors.SecondaryText,
        )
        TextButton(onClick = onScan) { Text("Scan SMS history") }
    }
}

@Composable
private fun HomeContent(
    state: OverviewUiState,
    onSmsScan: () -> Unit,
    isSyncing: Boolean,
    syncSummary: String?,
    amountsHidden: Boolean,
    onToggleAmountsHidden: () -> Unit,
    onSelectMonth: (YearMonth) -> Unit,
    onOpenTransaction: (Long) -> Unit,
    onOpenReview: () -> Unit,
    onSeeAllActivity: () -> Unit,
    onOpenInsights: () -> Unit,
    onImportClick: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = Spacing.xxl),
    ) {
        item {
            WarmHero {
                Column(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = Spacing.md),
                ) {
                    Spacer(Modifier.height(Spacing.sm))
                    HomeHeader(
                        selectedMonth = state.selectedMonth,
                        availableMonths = state.availableMonths,
                        monthsWithData = state.monthsWithData,
                        onSelectMonth = onSelectMonth,
                        onImportClick = onImportClick,
                    )
                    Spacer(Modifier.height(Spacing.lg))
                    SpendingHero(
                        summary = state.monthSummary,
                        month = state.selectedMonth,
                        dailySpend = state.dailySpend,
                        amountsHidden = amountsHidden,
                        onToggleAmountsHidden = onToggleAmountsHidden,
                        onOpenInsights = onOpenInsights,
                    )
                }
            }
        }

        item {
            Column(modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.md)) {
                IncomeSpendingPanel(state.monthSummary, amountsHidden)
                Text("Recorded totals are provisional. Card history may be incomplete; unmatched overlapping records are excluded until reconciled.",
                    style = MaterialTheme.typography.bodySmall, color = KoshColors.SecondaryText)
                TextButton(onClick = onSmsScan, enabled = !isSyncing) {
                    Text(if (isSyncing) "Scanning SMS…" else "Scan SMS history")
                }
                if (syncSummary != null) {
                    Spacer(Modifier.height(Spacing.sm))
                    Text(syncSummary, style = MaterialTheme.typography.bodySmall, color = KoshColors.SecondaryText)
                }

                if (state.needsReviewCount > 0) {
                    Spacer(Modifier.height(Spacing.sm))
                    NeedsReviewChip(state.needsReviewCount, onClick = onOpenReview)
                }
            }
        }

        if (state.accountSummaries.isNotEmpty()) {
            item {
                Text(
                    "Accounts",
                    style = MaterialTheme.typography.titleMedium,
                    color = KoshColors.PrimaryText,
                    modifier = Modifier.padding(horizontal = Spacing.md),
                )
                Spacer(Modifier.height(Spacing.sm))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = Spacing.md),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    items(state.accountSummaries, key = { "${it.bank}-${it.accountSuffix}" }) { account ->
                        AccountCard(account, amountsHidden)
                    }
                }
                Spacer(Modifier.height(Spacing.md))
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Recent activity", style = MaterialTheme.typography.titleMedium, color = KoshColors.PrimaryText)
                Text(
                    "See all",
                    style = MaterialTheme.typography.bodyMedium,
                    color = KoshExtendedTheme.colors.accentText,
                    modifier = Modifier.clickable(onClick = onSeeAllActivity),
                )
            }
            Spacer(Modifier.height(Spacing.sm))
        }

        item {
            RecentActivityCard(
                transactions = state.recentActivity,
                amountsHidden = amountsHidden,
                onOpenTransaction = onOpenTransaction,
                modifier = Modifier.padding(horizontal = Spacing.md),
            )
        }
    }
}

@Composable
private fun WarmHero(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val glow = Brush.radialGradient(
                colors = listOf(
                    KoshColors.WarmHighlight.copy(alpha = 0.9f),
                    KoshColors.BurntAmber.copy(alpha = 0.55f),
                    KoshColors.BaseBlack,
                ),
                center = Offset(size.width * 0.5f, 0f),
                radius = size.width * 1.15f,
            )
            drawRect(brush = glow)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, KoshColors.BaseBlack),
                    startY = size.height * 0.35f,
                    endY = size.height,
                ),
            )
        }
        content()
    }
}

@Composable
private fun HomeHeader(
    selectedMonth: YearMonth,
    availableMonths: List<YearMonth>,
    monthsWithData: Set<YearMonth>,
    onSelectMonth: (YearMonth) -> Unit,
    onImportClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        KoshWordmark()

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(KoshColors.RaisedGraphite.copy(alpha = 0.7f))
                    .clickable(onClick = onImportClick)
                    .semantics { contentDescription = "Import bank statement" },
                contentAlignment = Alignment.Center,
            ) {
                ImportIcon(tint = KoshColors.SecondaryText)
            }

            MonthSelectorPill(
                selectedMonth = selectedMonth,
                availableMonths = availableMonths,
                monthsWithData = monthsWithData,
                onSelectMonth = onSelectMonth,
            )
        }
    }
}

private fun monthLabel(month: YearMonth): String =
    month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))

@Composable
private fun SpendingHero(
    summary: MonthSummary,
    month: YearMonth,
    dailySpend: List<DaySpend>,
    amountsHidden: Boolean,
    onToggleAmountsHidden: () -> Unit,
    onOpenInsights: () -> Unit,
) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.clickable(onClick = onOpenInsights)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Recorded spending", style = MaterialTheme.typography.bodyMedium, color = KoshColors.SecondaryText)
                Spacer(Modifier.width(Spacing.sm))
                VisibilityToggle(
                    visible = !amountsHidden,
                    onClick = onToggleAmountsHidden,
                    tint = KoshColors.SecondaryText,
                )
            }
            Text(
                formatRupees(summary.expense, amountsHidden),
                style = MaterialTheme.typography.displayLarge.copy(fontFeatureSettings = "tnum"),
                color = KoshColors.PrimaryText,
            )
            Text(monthLabel(month), style = MaterialTheme.typography.bodyMedium, color = KoshColors.SecondaryText)
        }

        if (dailySpend.size >= 2) {
            SpendSparkline(
                days = dailySpend,
                color = KoshExtendedTheme.colors.accentText,
                modifier = Modifier
                    .padding(top = Spacing.md)
                    .clickable(onClick = onOpenInsights),
            )
        }
    }
}

@Composable
private fun IncomeSpendingPanel(summary: MonthSummary, amountsHidden: Boolean) {
    val maxValue = maxOf(summary.income, summary.expense, BigDecimal.ONE)

    KoshCard(containerColor = KoshColors.RaisedGraphite) {
        Row(modifier = Modifier.padding(Spacing.md)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Income", style = MaterialTheme.typography.labelMedium, color = KoshColors.SecondaryText)
                Text(
                    formatRupees(summary.income, amountsHidden),
                    style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
                    color = KoshColors.PrimaryText,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(Spacing.xs))
                ProgressBar(
                    fraction = (summary.income.toFloat() / maxValue.toFloat()).coerceIn(0f, 1f),
                    color = KoshExtendedTheme.colors.comparison,
                )
                if (summary.income.compareTo(BigDecimal.ZERO) == 0) {
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        "No salary credit found this month",
                        style = MaterialTheme.typography.labelSmall,
                        color = KoshColors.SecondaryText,
                    )
                }
            }
            Spacer(Modifier.width(Spacing.lg))
            Column(modifier = Modifier.weight(1f)) {
                Text("Spending", style = MaterialTheme.typography.labelMedium, color = KoshColors.SecondaryText)
                Text(
                    formatRupees(summary.expense, amountsHidden),
                    style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
                    color = KoshColors.PrimaryText,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(Spacing.xs))
                ProgressBar(
                    fraction = (summary.expense.toFloat() / maxValue.toFloat()).coerceIn(0f, 1f),
                    color = KoshExtendedTheme.colors.warmHighlight,
                )
            }
        }
    }
}

@Composable
private fun ProgressBar(fraction: Float, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(KoshColors.Divider),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(4.dp)
                .background(color, RoundedCornerShape(2.dp)),
        )
    }
}

@Composable
private fun NeedsReviewChip(count: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(KoshColors.RaisedGraphite.copy(alpha = 0.6f))
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.ms, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (count == 1) {
                "1 transaction needs a decision — totals may change"
            } else {
                "$count transactions need a decision — totals may change"
            },
            style = MaterialTheme.typography.labelMedium,
            color = KoshExtendedTheme.colors.accentText,
        )
        Text("›", style = MaterialTheme.typography.titleMedium, color = KoshExtendedTheme.colors.accentText)
    }
}

@Composable
private fun RecentActivityCard(
    transactions: List<Transaction>,
    amountsHidden: Boolean,
    onOpenTransaction: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (transactions.isEmpty()) {
        Text(
            "No activity in this month yet.",
            style = MaterialTheme.typography.bodyMedium,
            color = KoshColors.SecondaryText,
            modifier = modifier,
        )
        return
    }

    KoshCard(modifier = modifier.fillMaxWidth()) {
        Column {
            transactions.forEachIndexed { index, transaction ->
                RecentActivityRow(transaction, amountsHidden, onClick = { onOpenTransaction(transaction.id) })
                if (index != transactions.lastIndex) {
                    HorizontalDivider(color = KoshColors.Divider, thickness = 1.dp)
                }
            }
        }
    }
}

@Composable
private fun RecentActivityRow(transaction: Transaction, amountsHidden: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(Spacing.ms),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryIconTile(transaction.category, transaction.merchant ?: "?")
        Spacer(Modifier.width(Spacing.ms))
        Column(modifier = Modifier.weight(1f)) {
            Text(transaction.merchant ?: "Unknown", style = MaterialTheme.typography.bodyLarge, color = KoshColors.PrimaryText)
            Text(
                if (transaction.type == TransactionType.CREDIT_CARD_PAYMENT) {
                    "Credit card repayment · Not spending or income"
                } else if (transaction.type == TransactionType.CREDIT_CARD_PURCHASE) {
                    "Credit card purchase"
                } else if (transaction.type == TransactionType.TRANSFER) {
                    "Transfer · Excluded from spending"
                } else {
                    transaction.timestamp.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
                },
                style = MaterialTheme.typography.labelMedium,
                color = if (transaction.type == TransactionType.TRANSFER) KoshExtendedTheme.colors.comparison else KoshColors.SecondaryText,
            )
        }
        val isTransfer = transaction.type == TransactionType.TRANSFER || transaction.type == TransactionType.CREDIT_CARD_PAYMENT
        val isExpense = transaction.direction == TransactionDirection.DEBIT
        val sign = if (amountsHidden || isTransfer) "" else if (isExpense) "-" else "+"
        val amountColor = when {
            isTransfer -> KoshColors.SecondaryText
            isExpense -> KoshExtendedTheme.colors.expense
            else -> KoshExtendedTheme.colors.income
        }
        Text(
            "$sign${formatRupees(transaction.amount, amountsHidden)}",
            style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
            fontWeight = FontWeight.SemiBold,
            color = amountColor,
        )
    }
}
