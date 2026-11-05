package dev.kosh.financetracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Semantic colors Material3's ColorScheme has no slot for. */
data class KoshExtendedColors(
    val income: Color,
    val expense: Color,
    val accentText: Color,
    val comparison: Color,
    val warmHighlight: Color,
    val burntAmber: Color,
    val raisedSurface: Color,
)

private val ExtendedColors = KoshExtendedColors(
    income = KoshColors.Positive,
    expense = KoshColors.Expense,
    accentText = KoshColors.Gold,
    comparison = KoshColors.Comparison,
    warmHighlight = KoshColors.WarmHighlight,
    burntAmber = KoshColors.BurntAmber,
    raisedSurface = KoshColors.RaisedGraphite,
)

private val LocalKoshExtendedColors = staticCompositionLocalOf { ExtendedColors }

object KoshExtendedTheme {
    val colors: KoshExtendedColors
        @Composable get() = LocalKoshExtendedColors.current
}

private val KoshDarkColorScheme = darkColorScheme(
    background = KoshColors.BaseBlack,
    surface = KoshColors.Graphite,
    surfaceVariant = KoshColors.RaisedGraphite,
    onBackground = KoshColors.PrimaryText,
    onSurface = KoshColors.PrimaryText,
    onSurfaceVariant = KoshColors.SecondaryText,
    primary = KoshColors.Gold,
    onPrimary = KoshColors.BaseBlack,
    outline = KoshColors.Divider,
)

@Composable
fun KoshTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalKoshExtendedColors provides ExtendedColors) {
        MaterialTheme(
            colorScheme = KoshDarkColorScheme,
            typography = KoshTypography,
            content = content,
        )
    }
}
