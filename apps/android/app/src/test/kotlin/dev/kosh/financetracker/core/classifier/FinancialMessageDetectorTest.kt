package dev.kosh.financetracker.core.classifier

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialMessageDetectorTest {

    @Test
    fun `detects debited SMS as financial`() {
        val body = "Rs.450.00 debited from A/c XX1234 on 28-Sep-26 via UPI to SWIGGY. Avl bal Rs.12,340.50"
        assertTrue(FinancialMessageDetector.isFinancialMessage(body))
    }

    @Test
    fun `detects credited SMS as financial`() {
        val body = "INR 49,800.00 credited to your account XX8892 towards SALARY"
        assertTrue(FinancialMessageDetector.isFinancialMessage(body))
    }

    @Test
    fun `rejects unrelated promotional SMS`() {
        val body = "Flat 50% off on your next order! Use code SAVE50. T&C apply."
        assertFalse(FinancialMessageDetector.isFinancialMessage(body))
    }

    @Test
    fun `rejects OTP SMS with no financial keywords`() {
        val body = "123456 is your OTP for login. Do not share it with anyone."
        assertFalse(FinancialMessageDetector.isFinancialMessage(body))
    }

    @Test
    fun `rejects blank body`() {
        assertFalse(FinancialMessageDetector.isFinancialMessage(""))
    }
}
