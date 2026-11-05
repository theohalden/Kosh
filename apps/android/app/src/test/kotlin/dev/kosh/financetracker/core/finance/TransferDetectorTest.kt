package dev.kosh.financetracker.core.finance

import dev.kosh.financetracker.core.model.PaymentMethod
import dev.kosh.financetracker.core.model.Transaction
import dev.kosh.financetracker.core.model.TransactionCategory
import dev.kosh.financetracker.core.model.TransactionDirection
import dev.kosh.financetracker.core.model.TransactionSource
import dev.kosh.financetracker.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

class TransferDetectorTest {

    private var nextId = 1L

    private fun transaction(
        amount: String,
        direction: TransactionDirection,
        type: TransactionType,
        timestamp: Instant,
        accountSuffix: String?,
    ) = Transaction(
        id = nextId++,
        timestamp = timestamp,
        amount = BigDecimal(amount),
        direction = direction,
        type = type,
        category = TransactionCategory.UNCATEGORIZED,
        merchant = null,
        rawMerchant = null,
        accountId = null,
        accountSuffix = accountSuffix,
        bank = "HDFC",
        paymentMethod = PaymentMethod.UPI,
        source = TransactionSource.SMS,
        sourceMessageId = null,
        rawSourceText = "UPI Ref No 123456789012",
        confidence = 1.0,
        notes = null,
        createdAt = timestamp,
    )

    @Test
    fun `equal amounts and nearby times do not prove a transfer without a shared reference`() {
        val now = Instant.parse("2026-09-28T08:00:00Z")
        val debit = transaction("100", TransactionDirection.DEBIT, TransactionType.EXPENSE, now, "1234")
            .copy(rawSourceText = "UPI Ref No 123456789012")
        val credit = transaction("100", TransactionDirection.CREDIT, TransactionType.UNKNOWN, now, "5678")
            .copy(rawSourceText = "UPI Ref No 123456789013")
        assertTrue(TransferDetector.findTransferPairs(listOf(debit, credit)).isEmpty())
    }

    @Test
    fun `ambiguous shared references require review instead of choosing the first pair`() {
        val now = Instant.parse("2026-09-28T08:00:00Z")
        val debit = transaction("100", TransactionDirection.DEBIT, TransactionType.EXPENSE, now, "1234")
        val first = transaction("100", TransactionDirection.CREDIT, TransactionType.UNKNOWN, now, "5678")
        val second = transaction("100", TransactionDirection.CREDIT, TransactionType.UNKNOWN, now, "9012")
        assertTrue(TransferDetector.findTransferPairs(listOf(debit, first, second)).isEmpty())
    }

    @Test
    fun `does not turn a card purchase and unattributed receipt into a bank transfer`() {
        val now = Instant.parse("2026-09-28T08:00:00Z")
        val debit = transaction("1593", TransactionDirection.DEBIT, TransactionType.EXPENSE, now, null)
        val credit = transaction("1593", TransactionDirection.CREDIT, TransactionType.UNKNOWN, now, null)
        assertTrue(TransferDetector.findTransferPairs(listOf(debit, credit)).isEmpty())
        assertTrue(TransferDetector.findTransferPairs(listOf(
            debit.copy(accountSuffix = "1234", bank = null), credit.copy(accountSuffix = "5678", bank = null),
        )).isEmpty())
    }

    @Test
    fun `matches a same-amount opposite-direction pair on different accounts within the time window`() {
        val now = Instant.parse("2026-09-28T08:00:00Z")
        val hdfcDebit = transaction("100.00", TransactionDirection.DEBIT, TransactionType.EXPENSE, now, "5590")
        val kotakCredit = transaction("100.00", TransactionDirection.CREDIT, TransactionType.INCOME, now.plusSeconds(90), "9996")

        val matched = TransferDetector.findTransferPairs(listOf(hdfcDebit, kotakCredit))

        assertEquals(setOf(hdfcDebit.id, kotakCredit.id), matched)
    }

    @Test
    fun `does not match transactions outside the time window`() {
        val now = Instant.parse("2026-09-28T08:00:00Z")
        val hdfcDebit = transaction("100.00", TransactionDirection.DEBIT, TransactionType.EXPENSE, now, "5590")
        val kotakCredit = transaction("100.00", TransactionDirection.CREDIT, TransactionType.INCOME, now.plusSeconds(3600), "9996")

        val matched = TransferDetector.findTransferPairs(listOf(hdfcDebit, kotakCredit))

        assertTrue(matched.isEmpty())
    }

    @Test
    fun `does not match transactions on the same known account`() {
        val now = Instant.parse("2026-09-28T08:00:00Z")
        val debit = transaction("100.00", TransactionDirection.DEBIT, TransactionType.EXPENSE, now, "5590")
        val credit = transaction("100.00", TransactionDirection.CREDIT, TransactionType.INCOME, now.plusSeconds(5), "5590")

        val matched = TransferDetector.findTransferPairs(listOf(debit, credit))

        assertTrue(matched.isEmpty())
    }

    @Test
    fun `does not match a real unrelated expense and income with different amounts`() {
        val now = Instant.parse("2026-09-28T08:00:00Z")
        val expense = transaction("450.00", TransactionDirection.DEBIT, TransactionType.EXPENSE, now, "5590")
        val income = transaction("49800.00", TransactionDirection.CREDIT, TransactionType.INCOME, now.plusSeconds(30), "9996")

        val matched = TransferDetector.findTransferPairs(listOf(expense, income))

        assertTrue(matched.isEmpty())
    }

    @Test
    fun `does not touch transactions that are already a more specific type`() {
        val now = Instant.parse("2026-09-28T08:00:00Z")
        val emi = transaction("100.00", TransactionDirection.DEBIT, TransactionType.LOAN_EMI, now, "5590")
        val credit = transaction("100.00", TransactionDirection.CREDIT, TransactionType.INCOME, now.plusSeconds(5), "9996")

        val matched = TransferDetector.findTransferPairs(listOf(emi, credit))

        assertTrue(matched.isEmpty())
    }

    @Test
    fun `matches a self-transfer whose credit leg is UNKNOWN, not INCOME`() {
        // Regression: since IncomeClassifier narrowed CREDIT->INCOME to salary-only, a
        // self-transfer's incoming leg is TransactionType.UNKNOWN, not INCOME, until this
        // detector relabels it TRANSFER. The candidate filter must not exclude UNKNOWN.
        val now = Instant.parse("2026-09-28T08:00:00Z")
        val hdfcDebit = transaction("100.00", TransactionDirection.DEBIT, TransactionType.EXPENSE, now, "5590")
        val kotakCredit = transaction("100.00", TransactionDirection.CREDIT, TransactionType.UNKNOWN, now.plusSeconds(5), "9996")

        val matched = TransferDetector.findTransferPairs(listOf(hdfcDebit, kotakCredit))

        assertEquals(setOf(hdfcDebit.id, kotakCredit.id), matched)
    }

    @Test
    fun `matches amounts that differ only in decimal scale, e_g 5210 vs 5210_00`() {
        // Regression: BigDecimal("5210") != BigDecimal("5210.00") under equals(), even
        // though they're the same number — SMS bodies aren't consistent about decimals.
        val now = Instant.parse("2026-09-28T08:00:00Z")
        val debit = transaction("5210", TransactionDirection.DEBIT, TransactionType.EXPENSE, now, "8384")
        val credit = transaction("5210.00", TransactionDirection.CREDIT, TransactionType.INCOME, now.plusMillis(601), "5590")

        val matched = TransferDetector.findTransferPairs(listOf(debit, credit))

        assertEquals(setOf(debit.id, credit.id), matched)
    }
}
