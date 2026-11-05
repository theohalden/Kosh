package dev.kosh.financetracker.feature.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.kosh.financetracker.core.model.Transaction
import dev.kosh.financetracker.core.model.TransactionCategory
import dev.kosh.financetracker.core.model.TransactionType
import dev.kosh.financetracker.core.model.countsAsSpending
import dev.kosh.financetracker.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReviewUiState(
    val queue: List<Transaction> = emptyList(),
    val position: Int = 0,
    val current: Transaction? = null,
    val selectedCategory: TransactionCategory? = null,
)

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val repository: TransactionRepository,
) : ViewModel() {

    private val queueFlow = repository.observeTransactions()
        .map { all ->
            all.filter {
                it.type.countsAsSpending() &&
                    (it.category == null || it.category == TransactionCategory.UNCATEGORIZED)
            }
        }

    private val position = MutableStateFlow(0)
    private val selectedCategory = MutableStateFlow<TransactionCategory?>(null)

    val uiState: StateFlow<ReviewUiState> = combine(queueFlow, position, selectedCategory) { queue, pos, selected ->
        val clamped = pos.coerceIn(0, (queue.size - 1).coerceAtLeast(0))
        ReviewUiState(
            queue = queue,
            position = clamped,
            current = queue.getOrNull(clamped),
            selectedCategory = selected,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReviewUiState())

    fun selectCategory(category: TransactionCategory) {
        selectedCategory.value = category
    }

    fun goToPrevious() {
        position.value = (position.value - 1).coerceAtLeast(0)
        selectedCategory.value = null
    }

    fun goToNext() {
        position.value += 1
        selectedCategory.value = null
    }

    fun confirmSelection() {
        val current = uiState.value.current ?: return
        val category = uiState.value.selectedCategory ?: return
        viewModelScope.launch {
            repository.setCategory(current.id, category)
            selectedCategory.value = null
        }
    }
}
