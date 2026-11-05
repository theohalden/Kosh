package dev.kosh.financetracker.core.model

import java.math.BigDecimal
import java.time.Instant

enum class TransactionDirection {
    CREDIT,
    DEBIT,
    UNKNOWN,
}

enum class TransactionType {
    INCOME,
    EXPENSE,
    TRANSFER,
    CREDIT_CARD_PURCHASE,
    CREDIT_CARD_PAYMENT,
    LOAN_EMI,
    LOAN_DISBURSEMENT,
    INVESTMENT,
    SAVINGS,
    REFUND,
    CASH_WITHDRAWAL,
    CASH_DEPOSIT,
    FEE,
    INTEREST_INCOME,
    UNKNOWN,
}

enum class TransactionCategory {
    FOOD,
    GROCERIES,
    TRAVEL,
    FUEL,
    SHOPPING,
    BILLS,
    SUBSCRIPTIONS,
    RENT,
    FAMILY,
    HEALTH,
    ENTERTAINMENT,
    EDUCATION,
    PERSONAL_CARE,
    HOME,
    TECHNOLOGY,
    BUSINESS,
    FEES,
    OTHER,
    UNCATEGORIZED,
}

enum class PaymentMethod {
    UPI,
    CARD,
    NEFT,
    IMPS,
    RTGS,
    CASH,
    NETBANKING,
    UNKNOWN,
}

enum class TransactionSource {
    SMS,
    NOTIFICATION,
    MANUAL,
    /** Imported from a bank-issued statement file (CSV/Excel) rather than parsed
     * from a message — treated as ground truth for the account+month it covers,
     * superseding whatever SMS parsing produced for that same window. */
    STATEMENT,
}

data class Transaction(
    val id: Long = 0,
    val timestamp: Instant,
    val amount: BigDecimal,
    val direction: TransactionDirection,
    val type: TransactionType,
    val category: TransactionCategory?,
    val merchant: String?,
    val rawMerchant: String?,
    val accountId: Long?,
    /** Raw account suffix (e.g. "5590") extracted by the parser. Used for transfer
     * detection before a full Account entity exists (Phase 19). */
    val accountSuffix: String?,
    /** Bank/issuer name (e.g. "HDFC") as recognized by the parser — powers the
     * masked account display ("HDFC ••1234") in Activity. Null for a generic/
     * unrecognized-bank parse. */
    val bank: String?,
    val paymentMethod: PaymentMethod?,
    val source: TransactionSource,
    val sourceMessageId: String?,
    /** Full original SMS/notification text this was parsed from — powers the
     * "how Kosh understood this" transaction detail view and the review queue. */
    val rawSourceText: String?,
    val confidence: Double,
    val notes: String?,
    val createdAt: Instant,
    /** Running account balance immediately after this transaction, when the source
     * provides one (bank statements always do; SMS sometimes does). Powers real
     * balance tracking over time instead of just transaction totals. */
    val balanceAfter: BigDecimal? = null,
    val transactionRef: String? = null,
    /** Display-only reconciliation state; original evidence is never rewritten. */
    val reconciliationNote: String? = null,
    val isUnmatchedOverlap: Boolean = false,
)
