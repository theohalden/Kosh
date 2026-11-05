package dev.kosh.financetracker.feature.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.kosh.financetracker.core.model.Transaction
import dev.kosh.financetracker.core.model.TransactionCategory
import dev.kosh.financetracker.data.repository.TransactionRepository
import dev.kosh.financetracker.navigation.KoshDestination
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: TransactionRepository,
) : ViewModel() {

    private val transactionId: Long =
        checkNotNull(savedStateHandle.get<String>(KoshDestination.Detail.ARG_TRANSACTION_ID)).toLong()

    val transaction: StateFlow<Transaction?> = repository.observeTransaction(transactionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setCategory(category: TransactionCategory) {
        viewModelScope.launch { repository.setCategory(transactionId, category) }
    }
}
