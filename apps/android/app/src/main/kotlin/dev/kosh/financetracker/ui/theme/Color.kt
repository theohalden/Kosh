package dev.kosh.financetracker.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Kosh warm approved palette (29 Sep 2026 direction). Single dark theme — the
 * reference boards give no light-mode equivalent of this look, so this replaces
 * the earlier neutral light/dark system rather than adding a variant of it.
 */
object KoshColors {
    val BaseBlack = Color(0xFF0B0D0F)
    val Graphite = Color(0xFF191B1D)
    val RaisedGraphite = Color(0xFF252526)
    val PrimaryText = Color(0xFFF7F5F0)
    val SecondaryText = Color(0xFFB9B6B1)
    val Gold = Color(0xFFD5A64F)
    val WarmHighlight = Color(0xFFFFBF5B)
    val BurntAmber = Color(0xFF7C330D)
    val Divider = Color(0xFF3B3936)
    val Positive = Color(0xFFB7D5AE)
    val Expense = Color(0xFFF1A292)
    val Comparison = Color(0xFFC8A7DB)

    /** Qualitative palette for the category breakdown bar/legend only. */
    val ChartPalette = listOf(Gold, WarmHighlight, Expense, Comparison, SecondaryText)

    /** Extended identity colors for per-category icon tiles — one stable color per
     * category everywhere it appears (Home, Activity, Review, Spending), so a
     * category is recognizable at a glance the way a brand icon would be. */
    val CategoryBlue = Color(0xFF7FAEDB)
    val CategoryTeal = Color(0xFF6FBFA3)
    val CategoryRed = Color(0xFFE08A7D)
    val CategoryPink = Color(0xFFD98BB0)
    val CategoryIndigo = Color(0xFF9A93D9)
}
