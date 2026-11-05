package dev.kosh.financetracker.core.model

/** Purchase-basis spending: card repayments settle debt and are not a second purchase. */
fun TransactionType.countsAsSpending(): Boolean =
    this == TransactionType.EXPENSE || this == TransactionType.CREDIT_CARD_PURCHASE

fun TransactionType.displayLabel(): String = when (this) {
    TransactionType.CREDIT_CARD_PURCHASE -> "Credit card purchase"
    TransactionType.CREDIT_CARD_PAYMENT -> "Credit card repayment"
    else -> name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
}
