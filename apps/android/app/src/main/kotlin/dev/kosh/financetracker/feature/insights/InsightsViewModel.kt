package dev.kosh.financetracker.feature.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.kosh.financetracker.core.finance.CategorySlice
import dev.kosh.financetracker.core.finance.DaySpend
import dev.kosh.financetracker.core.finance.FinanceCalculator
import dev.kosh.financetracker.core.finance.MonthSummary
import dev.kosh.financetracker.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.math.BigDecimal
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

data class InsightsUiState(
    val selectedMonth: YearMonth = YearMonth.now(),
    val availableMonths: List<YearMonth> = emptyList(),
    val monthsWithData: Set<YearMonth> = emptySet(),
    val monthSummary: MonthSummary = MonthSummary(BigDecimal.ZERO, BigDecimal.ZERO),
    val percentChangeVsPreviousMonth: Float? = null,
    val dailySpend: List<DaySpend> = emptyList(),
    val categoryBreakdown: List<CategorySlice> = emptyList(),
    val hasAnyData: Boolean = false,
)

@HiltViewModel
class InsightsViewModel @Inject constructor(
    repository: TransactionRepository,
) : ViewModel() {

    private val zone = ZoneId.systemDefault()
    private val selectedMonth = MutableStateFlow<YearMonth?>(null)

    val uiState: StateFlow<InsightsUiState> = combine(
        repository.observeTransactions(),
        selectedMonth,
    ) { transactions, explicitMonth ->
        val month = explicitMonth ?: YearMonth.now(zone)
        val monthsWithData = FinanceCalculator.monthsWithData(transactions, zone)
        val months = (FinanceCalculator.recentMonths(YearMonth.now(zone)) + monthsWithData)
            .distinct()
            .sortedDescending()

        InsightsUiState(
            selectedMonth = month,
            availableMonths = months,
            monthsWithData = monthsWithData.toSet(),
            monthSummary = FinanceCalculator.summaryForMonth(transactions, month, zone),
            percentChangeVsPreviousMonth = FinanceCalculator.percentChangeVsPreviousMonth(transactions, month, zone),
            dailySpend = FinanceCalculator.dailySpendForMonth(transactions, month, zone),
            categoryBreakdown = FinanceCalculator.categoryBreakdownForMonth(transactions, month, zone),
            hasAnyData = transactions.isNotEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InsightsUiState())

    fun selectMonth(month: YearMonth) {
        selectedMonth.value = month
    }
}
