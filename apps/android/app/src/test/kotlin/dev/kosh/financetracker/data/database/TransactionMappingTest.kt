package dev.kosh.financetracker.data.database

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

class TransactionMappingTest {

    @Test
    fun `entity round-trips through domain mapping without losing fields`() {
        val now = Instant.ofEpochMilli(System.currentTimeMillis())
        val original = Transaction(
            id = 7,
            timestamp = now,
            amount = BigDecimal("450.00"),
            direction = TransactionDirection.DEBIT,
            type = TransactionType.EXPENSE,
            category = TransactionCategory.FOOD,
            merchant = "Swiggy",
            rawMerchant = "UPI-SWIGGY-BANGALORE",
            accountId = 1L,
            accountSuffix = "1234",
            bank = "HDFC",
            paymentMethod = PaymentMethod.UPI,
            source = TransactionSource.SMS,
            sourceMessageId = "msg-123",
            rawSourceText = "Rs.450.00 debited from A/c XX1234 on 28-Sep-26 to VPA swiggy@ybl SWIGGY.",
            confidence = 0.94,
            notes = null,
            createdAt = now,
        )

        val roundTripped = original.toEntity().toDomain()

        assertEquals(original, roundTripped)
    }
}
