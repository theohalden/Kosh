package dev.kosh.financetracker.core.model

import java.time.Instant

enum class EventSource {
    SMS,
    NOTIFICATION,
}

/**
 * SMS and notifications both become this before anything finance-specific touches
 * them, so ingestion stays decoupled from parsing/classification (notifications
 * feed the same pipeline later without the parser layer knowing the difference).
 */
data class RawFinancialEvent(
    val id: String,
    val source: EventSource,
    val sender: String?,
    val rawText: String,
    val timestamp: Instant,
    val sourceIdentifier: String?,
)
