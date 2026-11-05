package dev.kosh.financetracker.feature.sync

import dev.kosh.financetracker.core.classifier.FinancialMessageDetector
import dev.kosh.financetracker.core.classifier.toTransaction
import dev.kosh.financetracker.core.model.TransactionDirection
import dev.kosh.financetracker.core.parser.TransactionParserRegistry
import dev.kosh.financetracker.data.repository.TransactionRepository
import dev.kosh.financetracker.data.sms.SmsReader
import dev.kosh.financetracker.data.sms.smsSourceMessageId
import dev.kosh.financetracker.data.sms.toRawFinancialEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

data class SyncState(
    val isSyncing: Boolean = false,
    val lastScannedCount: Int = 0,
    val lastImportedCount: Int = 0,
    val hasSyncedOnce: Boolean = false,
    val lastAlreadyRepresentedCount: Int = 0,
    val lastUnparsedCount: Int = 0,
    val lastUnknownDirectionCount: Int = 0,
    val error: String? = null,
)

/**
 * Single shared engine for turning the SMS inbox into stored transactions — used by
 * every screen that needs fresh data (Overview, Trail) instead of each one re-running
 * its own scan. Live capture (SmsReceiver) handles one message at a time separately;
 * this does a full historical rescan.
 */
@Singleton
class SmsSyncManager @Inject constructor(
    private val smsReader: SmsReader,
    private val parserRegistry: TransactionParserRegistry,
    private val transactionRepository: TransactionRepository,
) {
    private val _state = MutableStateFlow(SyncState())
    val state: StateFlow<SyncState> = _state.asStateFlow()
    private val mutex = Mutex()

    suspend fun sync() = mutex.withLock {
        _state.value = _state.value.copy(isSyncing = true, error = null)

        try {
        val result = withContext(Dispatchers.IO) {
            val all = smsReader.readInboxMessages()
            val financial = all.filter { FinancialMessageDetector.isFinancialMessage(it.body) }

            var imported = 0
            var alreadyRepresented = 0
            var unparsed = 0
            var unknownDirection = 0
            for (entry in financial) {
                val parsed = parserRegistry.parse(entry.toRawFinancialEvent())
                if (parsed == null) {
                    unparsed++
                    continue
                }
                if (parsed.direction == TransactionDirection.UNKNOWN) {
                    unknownDirection++
                    continue
                }

                val sourceMessageId = smsSourceMessageId(entry.sender, entry.timestampEpochMillis, entry.body)
                val transaction = parsed.toTransaction(sourceMessageId = sourceMessageId, rawSourceText = entry.body)
                if (transactionRepository.saveIfNew(transaction)) imported++ else alreadyRepresented++
            }

            transactionRepository.reconcileTransfers()
            transactionRepository.reconcileIncomeClassification()
            transactionRepository.reconcileCategories()
            SyncState(
                lastScannedCount = all.size, lastImportedCount = imported, hasSyncedOnce = true,
                lastAlreadyRepresentedCount = alreadyRepresented, lastUnparsedCount = unparsed,
                lastUnknownDirectionCount = unknownDirection,
            )
        }
        _state.value = result
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            _state.value = _state.value.copy(error = "SMS scan did not finish. Some messages may not have been processed. Try again.")
        } finally {
            _state.value = _state.value.copy(isSyncing = false)
        }
    }
}
