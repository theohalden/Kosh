package dev.kosh.financetracker.feature.importstatement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.kosh.financetracker.core.statement.StatementImportResult
import dev.kosh.financetracker.core.statement.StatementParserRegistry
import dev.kosh.financetracker.core.statement.StatementTransaction
import dev.kosh.financetracker.core.statement.StatementValidation
import dev.kosh.financetracker.data.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed interface StatementImportUiState {
    data object Idle : StatementImportUiState
    data object Parsing : StatementImportUiState
    data class Confirming(
        val fileName: String,
        val bank: String,
        val accountSuffix: String,
        val count: Int,
        val periodLabel: String,
        private val transactions: List<StatementTransaction>,
    ) : StatementImportUiState {
        internal fun rows(): List<StatementTransaction> = transactions
    }
    data object Importing : StatementImportUiState
    data class Done(val results: List<StatementImportResult.Imported>) : StatementImportUiState
    data class Error(val message: String) : StatementImportUiState
}

@HiltViewModel
class StatementImportViewModel @Inject constructor(
    private val repository: TransactionRepository,
) : ViewModel() {

    private val registry = StatementParserRegistry()

    private val _uiState = MutableStateFlow<StatementImportUiState>(StatementImportUiState.Idle)
    val uiState: StateFlow<StatementImportUiState> = _uiState.asStateFlow()

    fun onFileSelected(fileName: String, bytes: ByteArray) {
        _uiState.value = StatementImportUiState.Parsing
        viewModelScope.launch {
            val rows = try {
                withContext(Dispatchers.Default) { registry.parse(fileName, bytes) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.value = StatementImportUiState.Error("Couldn't read this statement. Check the file and try again.")
                return@launch
            }
            if (rows.isEmpty()) {
                _uiState.value = StatementImportUiState.Error(
                    "Couldn't recognize \"$fileName\" as a supported bank statement (HDFC, Kotak, or Slice).",
                )
                return@launch
            }

            val issues = StatementValidation.check(rows)
            if (issues.isNotEmpty()) {
                _uiState.value = StatementImportUiState.Error(
                    "Statement check failed at transaction ${issues.first().row}: ${issues.first().reason}. Nothing was imported.",
                )
                return@launch
            }
            // Reject ambiguous input rather than silently dropping another account.
            val accounts = rows.map { it.bank.trim().lowercase(java.util.Locale.ROOT) to it.accountSuffix.trim() }.distinct()
            if (accounts.size != 1) {
                _uiState.value = StatementImportUiState.Error("Select a statement for one bank account at a time.")
                return@launch
            }
            val accountSuffix = rows.first().accountSuffix
            val sorted = rows.sortedBy { it.timestamp }

            _uiState.value = StatementImportUiState.Confirming(
                fileName = fileName,
                bank = sorted.first().bank,
                accountSuffix = accountSuffix,
                count = sorted.size,
                periodLabel = formatPeriod(sorted),
                transactions = sorted,
            )
        }
    }

    fun confirmImport() {
        val current = _uiState.value as? StatementImportUiState.Confirming ?: return
        _uiState.value = StatementImportUiState.Importing
        viewModelScope.launch {
            try {
                val results = repository.importStatement(current.rows())
                _uiState.value = StatementImportUiState.Done(results)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.value = StatementImportUiState.Error(
                    "Import failed. Your existing transactions were kept. Please try again.",
                )
            }
        }
    }

    fun dismiss() {
        _uiState.value = StatementImportUiState.Idle
    }

    private fun formatPeriod(rows: List<StatementTransaction>): String {
        val zone = java.time.ZoneId.systemDefault()
        val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM yyyy")
        val start = rows.first().timestamp.atZone(zone).format(formatter)
        val end = rows.last().timestamp.atZone(zone).format(formatter)
        return if (start == end) start else "$start – $end"
    }
}
