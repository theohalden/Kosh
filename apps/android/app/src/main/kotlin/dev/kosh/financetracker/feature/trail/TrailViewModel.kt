package dev.kosh.financetracker.feature.trail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.kosh.financetracker.core.model.Transaction
import dev.kosh.financetracker.core.model.TransactionType
import dev.kosh.financetracker.core.model.countsAsSpending
import dev.kosh.financetracker.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

enum class TrailFilter { ALL, EXPENSES, INCOME, TRANSFERS }

data class TrailUiState(
    val query: String = "",
    val filter: TrailFilter = TrailFilter.ALL,
    val transactions: List<Transaction> = emptyList(),
)

@HiltViewModel
class TrailViewModel @Inject constructor(
    repository: TransactionRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(TrailFilter.ALL)

    val uiState: StateFlow<TrailUiState> = combine(
        repository.observeTransactions(),
        query,
        filter,
    ) { transactions, query, filter ->
        val filtered = transactions
            .filter { matchesFilter(it, filter) }
            .filter { matchesQuery(it, query) }
        TrailUiState(query = query, filter = filter, transactions = filtered)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrailUiState())

    fun setQuery(value: String) {
        query.value = value
    }

    fun setFilter(value: TrailFilter) {
        filter.value = value
    }

    private fun matchesFilter(transaction: Transaction, filter: TrailFilter): Boolean = when (filter) {
        TrailFilter.ALL -> true
        TrailFilter.EXPENSES -> transaction.type.countsAsSpending()
        TrailFilter.INCOME -> transaction.type == TransactionType.INCOME
        TrailFilter.TRANSFERS -> transaction.type == TransactionType.TRANSFER
    }

    private fun matchesQuery(transaction: Transaction, query: String): Boolean {
        if (query.isBlank()) return true
        val q = query.trim().lowercase()
        return transaction.merchant?.lowercase()?.contains(q) == true ||
            transaction.accountSuffix?.contains(q) == true ||
            transaction.amount.toPlainString().contains(q)
    }
}
