package dev.kosh.financetracker.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.kosh.financetracker.core.finance.FinanceCalculator
import dev.kosh.financetracker.data.repository.TransactionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AppShellViewModel @Inject constructor(
    repository: TransactionRepository,
) : ViewModel() {

    val needsReviewCount: StateFlow<Int> = repository.observeTransactions()
        .map { FinanceCalculator.currentMonthUncategorizedCount(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}
