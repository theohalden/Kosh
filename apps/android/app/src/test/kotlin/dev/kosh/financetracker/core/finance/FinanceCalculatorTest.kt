package dev.kosh.financetracker.core.finance

import dev.kosh.financetracker.core.model.PaymentMethod
import dev.kosh.financetracker.core.model.Transaction
import dev.kosh.financetracker.core.model.TransactionCategory
import dev.kosh.financetracker.core.model.TransactionDirection
import dev.kosh.financetracker.core.model.TransactionSource
import dev.kosh.financetracker.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.ZonedDateTime

class FinanceCalculatorTest {

    private val zone = ZoneOffset.UTC
    private val now = ZonedDateTime.of(2026, 9, 28, 12, 0, 0, 0, zone).toInstant()

    private fun transaction(
        type: TransactionType,
        amount: String,
        timestamp: Instant,
        category: TransactionCategory = TransactionCategory.UNCATEGORIZED,
        bank: String? = null,
        accountSuffix: String? = null,
    ) = Transaction(
        timestamp = timestamp,
        amount = BigDecimal(amount),
        direction = if (type == TransactionType.INCOME) TransactionDirection.CREDIT else TransactionDirection.DEBIT,
        type = type,
        category = category,
        merchant = null,
        rawMerchant = null,
        accountId = null,
        accountSuffix = accountSuffix,
        bank = bank,
        paymentMethod = PaymentMethod.UPI,
        source = TransactionSource.SMS,
        sourceMessageId = null,
        rawSourceText = null,
        confidence = 1.0,
        notes = null,
        createdAt = timestamp,
    )

    @Test
    fun `one sided bank payment waits for evidence instead of becoming spending`() {
        val outgoing = transaction(TransactionType.EXPENSE, "10000", now, bank = "HDFC", accountSuffix = "5590")
            .copy(id = 1, rawSourceText = "Sent Rs.10000.00\nFrom HDFC Bank A/C *5590\nTo Kotak Mahindra Bank Ltd\nRef 123456789012")
        val unrelated = outgoing.copy(id = 2, rawSourceText = "Sent Rs.10000.00\nFrom HDFC Bank A/C *5590\nTo A PERSON\nRef 123456789013")
        val pending = LedgerProjection.project(listOf(outgoing, unrelated))
        assertEquals(BigDecimal("20000"), FinanceCalculator.accountSummariesForMonth(pending, YearMonth.from(now.atZone(zone)), zone).single().monthDebit)
        assertEquals(TransactionType.UNKNOWN, pending[0].type)
        assertEquals(true, pending[0].reconciliationNote != null)
        assertEquals(TransactionType.EXPENSE, pending[1].type)
        assertEquals(BigDecimal("10000"), FinanceCalculator.currentMonthSummary(pending, zone, now).expense)
        val incoming = outgoing.copy(id = 3, bank = "Kotak", accountSuffix = "9996", direction = TransactionDirection.CREDIT, type = TransactionType.UNKNOWN,
            rawSourceText = "Rs.10000 credited to Kotak A/c9996 UPI Ref 123456789012")
        val paired = LedgerProjection.project(listOf(outgoing, incoming))
        assertEquals(listOf(TransactionType.TRANSFER, TransactionType.TRANSFER), paired.map { it.type })
        assertEquals(true, paired.all { it.reconciliationNote == null })
        assertEquals(BigDecimal.ZERO, FinanceCalculator.currentMonthSummary(paired, zone, now).expense)
    }

    @Test
    fun `ambiguous Slice payment does not infer either new spending or an EMI`() {
        val row = transaction(TransactionType.EXPENSE, "1500", now)
            .copy(rawSourceText = "Sent Rs.1500.00\nFrom HDFC Bank A/C *5590\nTo slice\nRef 123456789012")
        val projected = LedgerProjection.project(listOf(row)).single()
        assertEquals(TransactionType.UNKNOWN, projected.type)
        assertEquals(true, projected.reconciliationNote != null)
    }

    @Test
    fun `confirmed transfers and loan repayments survive projection without becoming spending`() {
        val original = transaction(TransactionType.EXPENSE, "10000", now, bank = "HDFC", accountSuffix = "5590")
        val confirmed = original.copy(notes = "Kosh user-confirmed type: TRANSFER\nUser confirmed this is a transfer to their own Kotak account.")
        val emi = original.copy(id = 2, amount = BigDecimal("1234.56"), notes = "Kosh user-confirmed type: LOAN_EMI\nUser confirmed borrowing repayment.")
        val result = LedgerProjection.project(listOf(confirmed, emi))
        assertEquals(TransactionType.TRANSFER, result[0].type)
        assertEquals(TransactionType.LOAN_EMI, result[1].type)
        assertEquals(BigDecimal.ZERO, FinanceCalculator.currentMonthSummary(result, zone, now).expense)
        assertEquals(BigDecimal("11234.56"), FinanceCalculator.accountSummariesForMonth(result, YearMonth.from(now.atZone(zone)), zone).single().monthDebit)
        assertEquals(TransactionType.EXPENSE, original.type)
    }

    @Test
    fun `overlapping evidence stays visible without inflating any totals`() {
        val statement = transaction(TransactionType.EXPENSE, "59", now, bank = "Slice", accountSuffix = "8384")
            .copy(id = 1, source = TransactionSource.STATEMENT)
        val sms = statement.copy(id = 2, source = TransactionSource.SMS)
        val later = sms.copy(id = 3, timestamp = now.plusSeconds(86400))
        val projected = LedgerProjection.project(listOf(statement, sms, later))
        assertEquals(3, projected.size)
        assertEquals(true, projected.first { it.id == 2L }.reconciliationNote != null)
        assertEquals(null, projected.first { it.id == 3L }.reconciliationNote)
        val month = YearMonth.from(now.atZone(zone))
        assertEquals(BigDecimal("118"), FinanceCalculator.summaryForMonth(projected, month, zone).expense)
        assertEquals(BigDecimal("118"), FinanceCalculator.categoryBreakdownForMonth(projected, month, zone).single().amount)
        assertEquals(BigDecimal("118"), FinanceCalculator.accountSummariesForMonth(projected, month, zone).single().monthDebit)
        assertEquals(BigDecimal("118"), FinanceCalculator.dailySpendForMonth(projected, month, zone).sumOf { it.amount })
        assertEquals(null, sms.reconciliationNote)
    }

    @Test
    fun `sums expenses and income within the current month only`() {
        val withinMonth = ZonedDateTime.of(2026, 9, 15, 10, 0, 0, 0, zone).toInstant()
        val previousMonth = ZonedDateTime.of(2026, 8, 15, 10, 0, 0, 0, zone).toInstant()

        val transactions = listOf(
            transaction(TransactionType.EXPENSE, "450.00", withinMonth),
            transaction(TransactionType.EXPENSE, "2000.00", withinMonth),
            transaction(TransactionType.INCOME, "49800.00", withinMonth),
            transaction(TransactionType.EXPENSE, "999.00", previousMonth),
        )

        val summary = FinanceCalculator.currentMonthSummary(transactions, zone = zone, now = now)

        assertEquals(BigDecimal("2450.00"), summary.expense)
        assertEquals(BigDecimal("49800.00"), summary.income)
        assertEquals(BigDecimal("47350.00"), summary.netCashFlow)
    }

    @Test
    fun `ignores transfer and unknown types`() {
        val withinMonth = ZonedDateTime.of(2026, 9, 15, 10, 0, 0, 0, zone).toInstant()
        val transactions = listOf(
            transaction(TransactionType.TRANSFER, "10000.00", withinMonth),
            transaction(TransactionType.UNKNOWN, "500.00", withinMonth),
        )

        val summary = FinanceCalculator.currentMonthSummary(transactions, zone = zone, now = now)

        assertEquals(BigDecimal.ZERO, summary.income)
        assertEquals(BigDecimal.ZERO, summary.expense)
    }

    @Test
    fun `returns zero summary when no transactions this month`() {
        val summary = FinanceCalculator.currentMonthSummary(emptyList(), zone = zone, now = now)

        assertEquals(BigDecimal.ZERO, summary.income)
        assertEquals(BigDecimal.ZERO, summary.expense)
        assertEquals(BigDecimal.ZERO, summary.netCashFlow)
    }

    @Test
    fun `builds a category breakdown sorted largest first with correct fractions`() {
        val withinMonth = ZonedDateTime.of(2026, 9, 15, 10, 0, 0, 0, zone).toInstant()
        val transactions = listOf(
            transaction(TransactionType.EXPENSE, "60.00", withinMonth, TransactionCategory.FOOD),
            transaction(TransactionType.EXPENSE, "20.00", withinMonth, TransactionCategory.TRAVEL),
            transaction(TransactionType.EXPENSE, "20.00", withinMonth, TransactionCategory.SHOPPING),
            transaction(TransactionType.INCOME, "1000.00", withinMonth, TransactionCategory.UNCATEGORIZED),
        )

        val breakdown = FinanceCalculator.currentMonthCategoryBreakdown(transactions, zone = zone, now = now)

        assertEquals(3, breakdown.size)
        assertEquals(TransactionCategory.FOOD, breakdown[0].category)
        assertEquals(BigDecimal("60.00"), breakdown[0].amount)
        assertEquals(0.6f, breakdown[0].fraction, 0.001f)
    }

    @Test
    fun `counts only this month's uncategorized expenses`() {
        val withinMonth = ZonedDateTime.of(2026, 9, 15, 10, 0, 0, 0, zone).toInstant()
        val previousMonth = ZonedDateTime.of(2026, 8, 15, 10, 0, 0, 0, zone).toInstant()
        val transactions = listOf(
            transaction(TransactionType.EXPENSE, "60.00", withinMonth, TransactionCategory.UNCATEGORIZED),
            transaction(TransactionType.EXPENSE, "60.00", withinMonth, TransactionCategory.FOOD),
            transaction(TransactionType.EXPENSE, "60.00", previousMonth, TransactionCategory.UNCATEGORIZED),
            transaction(TransactionType.INCOME, "60.00", withinMonth, TransactionCategory.UNCATEGORIZED),
        )

        assertEquals(1, FinanceCalculator.currentMonthUncategorizedCount(transactions, zone = zone, now = now))
    }

    @Test
    fun `groups account summaries by bank and account suffix, sorted by spend descending`() {
        val withinMonth = ZonedDateTime.of(2026, 9, 15, 10, 0, 0, 0, zone).toInstant()
        val previousMonth = ZonedDateTime.of(2026, 8, 15, 10, 0, 0, 0, zone).toInstant()
        val transactions = listOf(
            transaction(TransactionType.EXPENSE, "100.00", withinMonth, bank = "HDFC", accountSuffix = "5590"),
            transaction(TransactionType.EXPENSE, "50.00", withinMonth, bank = "HDFC", accountSuffix = "5590"),
            transaction(TransactionType.EXPENSE, "500.00", withinMonth, bank = "Kotak", accountSuffix = "9996"),
            // Excluded: no account suffix parsed, previous month, and a TRANSFER type.
            transaction(TransactionType.EXPENSE, "999.00", withinMonth, bank = "HDFC", accountSuffix = null),
            transaction(TransactionType.EXPENSE, "999.00", previousMonth, bank = "HDFC", accountSuffix = "5590"),
        )

        val summaries = FinanceCalculator.accountSummariesForMonth(transactions, YearMonth.of(2026, 9), zone)

        assertEquals(2, summaries.size)
        assertEquals("Kotak", summaries[0].bank)
        assertEquals(BigDecimal("500.00"), summaries[0].monthSpend)
        assertEquals(1, summaries[0].transactionCount)
        assertEquals("HDFC", summaries[1].bank)
        assertEquals(BigDecimal("150.00"), summaries[1].monthSpend)
        assertEquals(2, summaries[1].transactionCount)
    }

    @Test
    fun `account summary separates debit and credit, including transfers as real account activity`() {
        val withinMonth = ZonedDateTime.of(2026, 9, 15, 10, 0, 0, 0, zone).toInstant()
        val transactions = listOf(
            transaction(TransactionType.EXPENSE, "100.00", withinMonth, bank = "HDFC", accountSuffix = "5590"),
            transaction(TransactionType.INCOME, "5000.00", withinMonth, bank = "HDFC", accountSuffix = "5590"),
            // A transfer out is still a real debit on this specific account, even
            // though it's excluded from whole-portfolio "spending".
            transaction(TransactionType.TRANSFER, "200.00", withinMonth, bank = "HDFC", accountSuffix = "5590"),
        )

        val summaries = FinanceCalculator.accountSummariesForMonth(transactions, YearMonth.of(2026, 9), zone)

        assertEquals(1, summaries.size)
        assertEquals(BigDecimal("300.00"), summaries[0].monthDebit)
        assertEquals(BigDecimal("5000.00"), summaries[0].monthCredit)
        assertEquals(2, summaries[0].debitCount)
        assertEquals(1, summaries[0].creditCount)
    }
}
