package dev.kosh.financetracker.core.classifier

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IncomeClassifierTest {

    @Test
    fun `recognizes salary credited SMS as likely salary`() {
        assertTrue(
            IncomeClassifier.isLikelySalary(
                merchant = null,
                rawSourceText = "Rs.49,800.00 credited to A/c XX1234 on 1-Sep-26 by NEFT. Info: SALARY. Avl Bal Rs.55,230.00",
            ),
        )
    }

    @Test
    fun `recognizes a payroll merchant name`() {
        assertTrue(IncomeClassifier.isLikelySalary(merchant = "ACME PAYROLL SERVICES", rawSourceText = null))
    }

    @Test
    fun `does not treat a random person's UPI credit as salary`() {
        assertFalse(
            IncomeClassifier.isLikelySalary(
                merchant = "vivekpatel4049-1",
                rawSourceText = "Rs.313.00 credited to your account from vivekpatel4049-1 via UPI",
            ),
        )
    }

    @Test
    fun `does not treat a merchant refund as salary`() {
        assertFalse(IncomeClassifier.isLikelySalary(merchant = "AMAZON REFUND", rawSourceText = "Rs.500 credited"))
    }
}
