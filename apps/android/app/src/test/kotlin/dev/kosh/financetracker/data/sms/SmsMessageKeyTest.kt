package dev.kosh.financetracker.data.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SmsMessageKeyTest {

    @Test
    fun `same content produces the same key regardless of millisecond jitter`() {
        val a = smsSourceMessageId("HDFCBK", 1_000_000_500L, "Rs.450 debited")
        val b = smsSourceMessageId("HDFCBK", 1_000_000_900L, "Rs.450 debited")

        assertEquals(a, b)
    }

    @Test
    fun `different senders produce different keys`() {
        val a = smsSourceMessageId("HDFCBK", 1_000_000_000L, "Rs.450 debited")
        val b = smsSourceMessageId("KOTAKB", 1_000_000_000L, "Rs.450 debited")

        assertNotEquals(a, b)
    }

    @Test
    fun `different bodies produce different keys`() {
        val a = smsSourceMessageId("HDFCBK", 1_000_000_000L, "Rs.450 debited")
        val b = smsSourceMessageId("HDFCBK", 1_000_000_000L, "Rs.451 debited")

        assertNotEquals(a, b)
    }
}
