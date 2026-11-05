package dev.kosh.financetracker.feature.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.kosh.financetracker.core.finance.AccountSummary
import dev.kosh.financetracker.core.finance.CategorySlice
import dev.kosh.financetracker.core.finance.DaySpend
import dev.kosh.financetracker.core.finance.FinanceCalculator
import dev.kosh.financetracker.core.finance.MonthSummary
import dev.kosh.financetracker.core.model.Transaction
import dev.kosh.financetracker.data.repository.TransactionRepository
import dev.kosh.financetracker.feature.sync.SmsSyncManager
import dev.kosh.financetracker.feature.sync.SyncState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

data class OverviewUiState(
    val selectedMonth: YearMonth = YearMonth.now(),
    val availableMonths: List<YearMonth> = emptyList(),
    val monthsWithData: Set<YearMonth> = emptySet(),
    val monthSummary: MonthSummary = MonthSummary(BigDecimal.ZERO, BigDecimal.ZERO),
    val categoryBreakdown: List<CategorySlice> = emptyList(),
    val dailySpend: List<DaySpend> = emptyList(),
    val accountSummaries: List<AccountSummary> = emptyList(),
    val recentActivity: List<Transaction> = emptyList(),
    val totalCount: Int = 0,
    val needsReviewCount: Int = 0,
    val hasAnyData: Boolean = false,
)

@HiltViewModel
class OverviewViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val syncManager: SmsSyncManager,
) : ViewModel() {

    private val zone = ZoneId.systemDefault()
    private val selectedMonth = MutableStateFlow<YearMonth?>(null)

    val uiState: StateFlow<OverviewUiState> = combine(
        repository.observeTransactions(),
        selectedMonth,
    ) { transactions, explicitMonth ->
        val month = explicitMonth ?: YearMonth.now(zone)
        val monthsWithData = FinanceCalculator.monthsWithData(transactions, zone)
        // Always at least a real rolling window so the selector is genuinely
        // browsable, widened to include any older month that actually has data.
        val months = (FinanceCalculator.recentMonths(YearMonth.now(zone)) + monthsWithData)
            .distinct()
            .sortedDescending()

        OverviewUiState(
            selectedMonth = month,
            availableMonths = months,
            monthsWithData = monthsWithData.toSet(),
            monthSummary = FinanceCalculator.summaryForMonth(transactions, month, zone),
            categoryBreakdown = FinanceCalculator.categoryBreakdownForMonth(transactions, month, zone),
            dailySpend = FinanceCalculator.dailySpendForMonth(transactions, month, zone),
            accountSummaries = FinanceCalculator.accountSummariesForMonth(transactions, month, zone),
            recentActivity = transactions
                .filter { YearMonth.from(it.timestamp.atZone(zone)) == month }
                .take(6),
            totalCount = transactions.size,
            needsReviewCount = FinanceCalculator.uncategorizedCountForMonth(transactions, month, zone),
            hasAnyData = transactions.isNotEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OverviewUiState())

    val syncState: StateFlow<SyncState> = syncManager.state

    fun selectMonth(month: YearMonth) {
        selectedMonth.value = month
    }

    /** Caller must confirm SMS permission is granted first — the ViewModel has no
     * Context and no way to request it. */
    fun sync() {
        viewModelScope.launch { syncManager.sync() }
    }
}
