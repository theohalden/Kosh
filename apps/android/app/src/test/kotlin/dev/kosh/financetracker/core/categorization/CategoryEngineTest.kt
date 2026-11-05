package dev.kosh.financetracker.core.categorization

import dev.kosh.financetracker.core.model.TransactionCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryEngineTest {

    @Test
    fun `categorizes known food merchants`() {
        assertEquals(TransactionCategory.FOOD, CategoryEngine.categorize("SWIGGY"))
        assertEquals(TransactionCategory.FOOD, CategoryEngine.categorize("Nothing But Chicken Dadar"))
    }

    @Test
    fun `categorizes groceries before food when both could match`() {
        assertEquals(TransactionCategory.GROCERIES, CategoryEngine.categorize("BLINKIT"))
    }

    @Test
    fun `categorizes rides as travel`() {
        assertEquals(TransactionCategory.TRAVEL, CategoryEngine.categorize("Rapido QR Pay"))
    }

    @Test
    fun `categorizes retail as shopping`() {
        assertEquals(TransactionCategory.SHOPPING, CategoryEngine.categorize("RELIANCE RETAIL LIMITIED"))
    }

    @Test
    fun `falls back to uncategorized for a plain person name`() {
        assertEquals(TransactionCategory.UNCATEGORIZED, CategoryEngine.categorize("AMOL KALEL"))
    }

    @Test
    fun `falls back to uncategorized for null or blank merchant`() {
        assertEquals(TransactionCategory.UNCATEGORIZED, CategoryEngine.categorize(null))
        assertEquals(TransactionCategory.UNCATEGORIZED, CategoryEngine.categorize(""))
    }

    @Test
    fun `matches the exact merchant name Vi as Bills, not a substring false-positive`() {
        // Regression: a keyword rule of "vi " (with a trailing space, to avoid matching
        // inside words like "Vijay" or "Investment") never matched the bare merchant
        // string "Vi" itself, so every Vodafone Idea recharge fell through to Uncategorized.
        assertEquals(TransactionCategory.BILLS, CategoryEngine.categorize("Vi"))
        assertEquals(TransactionCategory.BILLS, CategoryEngine.categorize("vi"))
    }

    @Test
    fun `does not false-positive match unrelated merchants containing vi as a substring`() {
        assertEquals(TransactionCategory.UNCATEGORIZED, CategoryEngine.categorize("Vijay Sharma"))
        assertEquals(TransactionCategory.OTHER, CategoryEngine.categorize("Groww Invest Tech Pvt Ltd"))
    }

    @Test
    fun `categorizes known merchants across the newly added categories`() {
        assertEquals(TransactionCategory.TECHNOLOGY, CategoryEngine.categorize("Croma Electronics"))
        assertEquals(TransactionCategory.HOME, CategoryEngine.categorize("IKEA"))
        assertEquals(TransactionCategory.OTHER, CategoryEngine.categorize("Zerodha Broking"))
    }
}
