package dev.kosh.financetracker.core.classifier

import dev.kosh.financetracker.core.categorization.CategoryEngine
import dev.kosh.financetracker.core.model.*
import dev.kosh.financetracker.core.parser.ParserFields
import dev.kosh.financetracker.core.parser.TransactionMessageStatus
import java.util.Locale

/** Deterministic, non-destructive interpretation of issuer evidence, not amount/timing guesses. */
object CardTransactionClassifier {
    data class CardKey(val bank: String, val suffix: String)
    private val receipt = Regex(
        """\bpayment\b[\s\S]*\breceived\s+towards\s+(?:your\s+)?credit\s+card\b|\bpayment\b[\s\S]*\bcredited\s+to\s+your\s+card\b""",
        RegexOption.IGNORE_CASE,
    )
    private val creditEvidence = Regex("""\bcredit\s+card\b|\bcardmember\b|\bBLOCK\s+CC\b""", RegexOption.IGNORE_CASE)
    private val purchase = Regex("""\b(?:spent|paid|purchased)\s+(?:Rs\.?|INR\.?|₹)""", RegexOption.IGNORE_CASE)
    private val merchantAt = Regex("""\bAt\s+(.+?)\s+On\s+\d""", RegexOption.IGNORE_CASE)
    private val billPayment = Regex("""\bIB\s+BILLPAY\s+DR-HDFC\d+-\d+[X*]+(\d{4})\b""", RegexOption.IGNORE_CASE)

    private fun cardKey(transaction: Transaction): CardKey? {
        val bank = transaction.bank?.trim()?.lowercase(Locale.ROOT)?.takeIf { it.isNotBlank() } ?: return null
        val suffix = ParserFields.cardSuffix(transaction.rawSourceText.orEmpty()) ?: return null
        return CardKey(bank, suffix)
    }

    fun knownCards(transactions: List<Transaction>): Set<CardKey> = transactions.mapNotNull { transaction ->
        if (transaction.source == TransactionSource.MANUAL || TransactionMessageStatus.isNonCompleted(transaction.rawSourceText.orEmpty())) return@mapNotNull null
        if (!creditEvidence.containsMatchIn(transaction.rawSourceText.orEmpty())) return@mapNotNull null
        cardKey(transaction)
    }.toSet()

    fun projectAll(transactions: List<Transaction>): List<Transaction> {
        val cards = knownCards(transactions)
        return transactions.map { project(it, cards) }
    }

    fun project(transaction: Transaction, cards: Set<CardKey> = emptySet()): Transaction {
        if (transaction.source == TransactionSource.MANUAL) return transaction
        val body = transaction.rawSourceText.orEmpty()
        if (TransactionMessageStatus.isNonCompleted(body)) return transaction
        val key = cardKey(transaction)
        val isCreditCard = creditEvidence.containsMatchIn(body) || (key != null && key in cards)
        if (transaction.direction == TransactionDirection.CREDIT && receipt.containsMatchIn(body) && isCreditCard) {
            return transaction.copy(type = TransactionType.CREDIT_CARD_PAYMENT, merchant = transaction.merchant ?: "Credit card repayment", paymentMethod = PaymentMethod.CARD)
        }
        if (transaction.direction == TransactionDirection.DEBIT && purchase.containsMatchIn(body) && key != null && isCreditCard) {
            val rawMerchant = transaction.merchant ?: merchantAt.find(body)?.groupValues?.get(1)?.trim()
            val merchant = if (rawMerchant?.uppercase(Locale.ROOT) in setOf("GOOGLEWORKSP", "GOOGLE WORKSPACE")) "Google Workspace" else rawMerchant
            val category = if (transaction.category == null || transaction.category == TransactionCategory.UNCATEGORIZED) {
                CategoryEngine.categorize(merchant)
            } else transaction.category
            return transaction.copy(type = TransactionType.CREDIT_CARD_PURCHASE, merchant = merchant, category = category, paymentMethod = PaymentMethod.CARD)
        }
        val statementCard = billPayment.find(body)?.groupValues?.get(1)
        if (transaction.source == TransactionSource.STATEMENT && transaction.direction == TransactionDirection.DEBIT &&
            statementCard != null && CardKey(transaction.bank.orEmpty().trim().lowercase(Locale.ROOT), statementCard) in cards) {
            return transaction.copy(type = TransactionType.CREDIT_CARD_PAYMENT)
        }
        return transaction
    }
}
