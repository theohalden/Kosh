package dev.kosh.financetracker.data.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import dagger.hilt.android.AndroidEntryPoint
import dev.kosh.financetracker.core.classifier.FinancialMessageDetector
import dev.kosh.financetracker.core.classifier.toTransaction
import dev.kosh.financetracker.core.model.EventSource
import dev.kosh.financetracker.core.model.RawFinancialEvent
import dev.kosh.financetracker.core.model.TransactionDirection
import dev.kosh.financetracker.core.parser.TransactionParserRegistry
import dev.kosh.financetracker.data.repository.TransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/**
 * Catches new SMS the instant they arrive so transactions appear without the user
 * having to open the app and rescan. Feeds the same detector/parser/repository path
 * as historical import (Phase 2), just triggered live instead of on demand.
 */
@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    @Inject lateinit var parserRegistry: TransactionParserRegistry

    @Inject lateinit var transactionRepository: TransactionRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val sender = messages[0].originatingAddress.orEmpty()
        val body = messages.joinToString(separator = "") { it.messageBody.orEmpty() }
        val timestampEpochMillis = messages[0].timestampMillis

        if (!FinancialMessageDetector.isFinancialMessage(body)) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val sourceMessageId = smsSourceMessageId(sender, timestampEpochMillis, body)
                val event = RawFinancialEvent(
                    id = sourceMessageId,
                    source = EventSource.SMS,
                    sender = sender,
                    rawText = body,
                    timestamp = Instant.ofEpochMilli(timestampEpochMillis),
                    sourceIdentifier = sourceMessageId,
                )

                val parsed = parserRegistry.parse(event)
                if (parsed != null && parsed.direction != TransactionDirection.UNKNOWN) {
                    val transaction = parsed.toTransaction(sourceMessageId = sourceMessageId, rawSourceText = body)
                    val inserted = transactionRepository.saveIfNew(transaction)
                    if (inserted) transactionRepository.reconcileTransfers()
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
