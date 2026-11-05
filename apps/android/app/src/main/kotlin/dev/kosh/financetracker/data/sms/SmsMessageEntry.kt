package dev.kosh.financetracker.data.sms

import dev.kosh.financetracker.core.model.EventSource
import dev.kosh.financetracker.core.model.RawFinancialEvent
import java.time.Instant

data class SmsMessageEntry(
    val id: String,
    val sender: String,
    val body: String,
    val timestampEpochMillis: Long,
)

fun SmsMessageEntry.toRawFinancialEvent(): RawFinancialEvent = RawFinancialEvent(
    id = id,
    source = EventSource.SMS,
    sender = sender,
    rawText = body,
    timestamp = Instant.ofEpochMilli(timestampEpochMillis),
    sourceIdentifier = id,
)
