package dev.kosh.financetracker.ui.components

import androidx.compose.ui.graphics.Color
import dev.kosh.financetracker.core.model.TransactionCategory
import dev.kosh.financetracker.ui.theme.KoshColors

/** One stable identity color per category, used everywhere a category shows up
 * (Home, Activity, Review, Spending) so it reads like a recognizable icon color
 * rather than a random per-list-position color. */
fun colorFor(category: TransactionCategory?): Color = when (category) {
    TransactionCategory.FOOD -> KoshColors.WarmHighlight
    TransactionCategory.GROCERIES -> KoshColors.CategoryTeal
    TransactionCategory.TRAVEL -> KoshColors.CategoryBlue
    TransactionCategory.FUEL -> KoshColors.BurntAmber
    TransactionCategory.SHOPPING -> KoshColors.CategoryPink
    TransactionCategory.BILLS -> KoshColors.Gold
    TransactionCategory.SUBSCRIPTIONS -> KoshColors.CategoryRed
    TransactionCategory.RENT -> KoshColors.CategoryIndigo
    TransactionCategory.FAMILY -> KoshColors.Comparison
    TransactionCategory.HEALTH -> KoshColors.Positive
    TransactionCategory.ENTERTAINMENT -> KoshColors.CategoryIndigo
    TransactionCategory.EDUCATION -> KoshColors.CategoryBlue
    TransactionCategory.PERSONAL_CARE -> KoshColors.CategoryPink
    TransactionCategory.HOME -> KoshColors.BurntAmber
    TransactionCategory.TECHNOLOGY -> KoshColors.CategoryBlue
    TransactionCategory.BUSINESS -> KoshColors.SecondaryText
    TransactionCategory.FEES -> KoshColors.Expense
    TransactionCategory.OTHER -> KoshColors.CategoryTeal
    TransactionCategory.UNCATEGORIZED, null -> KoshColors.SecondaryText
}

fun labelFor(category: TransactionCategory): String = when (category) {
    TransactionCategory.FOOD -> "Food & Dining"
    TransactionCategory.GROCERIES -> "Groceries"
    TransactionCategory.TRAVEL -> "Transport"
    TransactionCategory.FUEL -> "Fuel"
    TransactionCategory.SHOPPING -> "Shopping"
    TransactionCategory.BILLS -> "Bills & Utilities"
    TransactionCategory.SUBSCRIPTIONS -> "Subscriptions"
    TransactionCategory.RENT -> "Rent"
    TransactionCategory.FAMILY -> "Family"
    TransactionCategory.HEALTH -> "Health"
    TransactionCategory.ENTERTAINMENT -> "Entertainment"
    TransactionCategory.EDUCATION -> "Education"
    TransactionCategory.PERSONAL_CARE -> "Personal Care"
    TransactionCategory.HOME -> "Home"
    TransactionCategory.TECHNOLOGY -> "Technology"
    TransactionCategory.BUSINESS -> "Business"
    TransactionCategory.FEES -> "Fees"
    TransactionCategory.OTHER -> "Other"
    TransactionCategory.UNCATEGORIZED -> "Uncategorized"
}

/** The short list offered in the Review picker — the full enum has 19 values, too
 * many to review one at a time; these cover the overwhelming majority of spending. */
val REVIEW_PICKER_CATEGORIES = listOf(
    TransactionCategory.FOOD,
    TransactionCategory.BILLS,
    TransactionCategory.SHOPPING,
    TransactionCategory.TRAVEL,
    TransactionCategory.OTHER,
)
