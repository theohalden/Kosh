package dev.kosh.financetracker.core.classifier

import dev.kosh.financetracker.core.finance.FinanceCalculator
import dev.kosh.financetracker.core.model.*
import dev.kosh.financetracker.core.parser.TransactionParserRegistry
import dev.kosh.financetracker.core.statement.StatementTransaction
import dev.kosh.financetracker.core.statement.toTransaction
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneOffset

class CardTransactionClassifierTest {
    private val time = Instant.parse("2026-11-02T14:00:00Z")
    private fun transaction(body: String, direction: TransactionDirection, type: TransactionType = TransactionType.UNKNOWN) =
        StatementTransaction(time, BigDecimal("1593.00"), direction, body, null, null, "HDFC", "1234")
            .toTransaction().copy(source = TransactionSource.SMS, type = type, accountSuffix = null)

    private val purchase = transaction(
        "Spent Rs.1593 On HDFC Bank Card 5678 At GOOGLEWORKSP On 2026-11-02. Not You? SMS BLOCK CC 5678",
        TransactionDirection.DEBIT, TransactionType.TRANSFER,
    )
    private val receipt = transaction(
        "PAYMENT OF Rs.1593 RECEIVED TOWARDS YOUR CREDIT CARD ENDING WITH 5678 ON 2-11-2026",
        TransactionDirection.CREDIT, TransactionType.TRANSFER,
    )

    @Test fun `purchase counts once and card repayments do not become spending or income`() {
        val bankPayment = StatementTransaction(
            time, BigDecimal("1593"), TransactionDirection.DEBIT,
            "IB BILLPAY DR-HDFC92-434155XXXXXX5678", null, null, "HDFC", "1234",
        ).toTransaction()
        val projected = CardTransactionClassifier.projectAll(listOf(purchase, receipt, bankPayment))
        assertEquals(listOf(TransactionType.CREDIT_CARD_PURCHASE, TransactionType.CREDIT_CARD_PAYMENT, TransactionType.CREDIT_CARD_PAYMENT), projected.map { it.type })
        val summary = FinanceCalculator.summaryForMonth(projected, YearMonth.of(2026, 11), ZoneOffset.UTC)
        assertEquals(BigDecimal("1593.00"), summary.expense)
        assertEquals(BigDecimal.ZERO, summary.income)
        assertEquals(BigDecimal("1593.00"), FinanceCalculator.categoryBreakdownForMonth(projected, YearMonth.of(2026, 11), ZoneOffset.UTC).single().amount)
        assertEquals(BigDecimal("1593.00"), FinanceCalculator.dailySpendForMonth(projected, YearMonth.of(2026, 11), ZoneOffset.UTC).single().amount)
        assertEquals(TransactionType.TRANSFER, purchase.type) // original record not edited
    }

    @Test fun `repayment confirmations are not income even when an older parser marked them as transfers`() {
        val second = receipt.copy(rawSourceText = "HDFC Bank Cardmember, Payment of Rs 1593 was credited to your card ending 5678 on 02/NOV/2026.")
        assertTrue(CardTransactionClassifier.projectAll(listOf(receipt, second)).all { it.type == TransactionType.CREDIT_CARD_PAYMENT })
    }

    @Test fun `ordinary debit card purchase and unrelated billpay are not inferred to be credit card activity`() {
        val debitCard = purchase.copy(type = TransactionType.EXPENSE, rawSourceText = "Spent Rs.1593 On HDFC Bank Debit Card 1234 At EXAMPLE On 2026-11-02")
        val unrelated = debitCard.copy(source = TransactionSource.STATEMENT, rawSourceText = "IB BILLPAY DR-ELECTRICITY")
        assertEquals(TransactionType.EXPENSE, CardTransactionClassifier.project(debitCard).type)
        assertEquals(TransactionType.EXPENSE, CardTransactionClassifier.project(unrelated).type)
        assertEquals(TransactionType.TRANSFER, CardTransactionClassifier.project(purchase.copy(source = TransactionSource.MANUAL)).type)
    }

    @Test fun `actual HDFC spent SMS is parsed without UPI and classified as a completed purchase`() {
        val event = RawFinancialEvent("example", EventSource.SMS, "HDFCBK", requireNotNull(purchase.rawSourceText), time, null)
        val parsed = requireNotNull(TransactionParserRegistry().parse(event))
        assertEquals(TransactionDirection.DEBIT, parsed.direction)
        val result = parsed.toTransaction("sms:example", event.rawText)
        assertEquals(TransactionType.CREDIT_CARD_PURCHASE, result.type)
        assertEquals("Google Workspace", result.merchant)
        assertEquals(TransactionCategory.SUBSCRIPTIONS, result.category)
    }

    @Test fun `a specific existing category is preserved`() {
        assertEquals(TransactionCategory.BUSINESS, CardTransactionClassifier.project(purchase.copy(category = TransactionCategory.BUSINESS)).category)
    }
}
