package dev.kosh.financetracker.core.parser

import dev.kosh.financetracker.core.model.EventSource
import dev.kosh.financetracker.core.model.RawFinancialEvent
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class TransactionMessageStatusTest {
    private fun event(body: String) = RawFinancialEvent("test", EventSource.SMS, "HDFCBK", body, Instant.EPOCH, null)

    @Test fun `scheduled debit alert is not an expense even with an amount and debit keyword`() {
        for (body in listOf(
            "Alert: INR.1593.00 will be debited on 03/11/2026 from HDFC Bank Card 1234 for Example Workspace",
            "Rs.1593.00 will be debited from HDFC Bank Card 1234 for Example Workspace",
            "UPI payment pending for Rs.1593.00 from A/c XX1234",
            "UPI payment failed for Rs.1593.00. Any amount debited will be reversed.",
            "Pre-debit notification: Rs.1593.00 scheduled payment from A/c XX1234",
            "Auto debit of INR 555 is set for 21/10/2026 and the debit request initiated is already in process with your bank.",
            "We've received your request for a connection. Please make a payment of Rs.807 via UPI to process this request.",
        )) {
            assertTrue(body, TransactionMessageStatus.isNonCompleted(body))
            assertNull(body, HdfcParser().parse(event(body)))
            assertNull(body, TransactionParserRegistry().parse(event(body)))
        }
    }

    @Test fun `actual completed payment remains eligible including an informational future clause`() {
        for (body in listOf(
            "Spent Rs.1593 On HDFC Bank Card 1234 At EXAMPLE On 2026-11-02",
            "Rs.1593.00 debited from A/c XX1234. Your next scheduled payment will be debited next month.",
            "Successfully paid Rs.1593 from slice a/c XX1234 via UPI AutoPay - slice",
        )) {
            assertFalse(body, TransactionMessageStatus.isNonCompleted(body))
            assertNotNull(body, TransactionParserRegistry().parse(event(body)))
        }
    }
}
