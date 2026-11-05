package dev.kosh.financetracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.kosh.financetracker.core.finance.AccountSummary
import dev.kosh.financetracker.ui.format.formatRupees
import dev.kosh.financetracker.ui.theme.KoshColors
import dev.kosh.financetracker.ui.theme.Spacing

/** A small fixed set of on-brand 3-stop gradients — enough to tell accounts apart
 * at a glance without inventing a new color per bank name. Picked deterministically
 * from the bank name so the same account always renders the same gradient. */
private val ACCOUNT_GRADIENTS = listOf(
    listOf(KoshColors.WarmHighlight, KoshColors.Gold, KoshColors.BurntAmber),
    listOf(KoshColors.CategoryBlue, KoshColors.CategoryIndigo, KoshColors.BaseBlack),
    listOf(KoshColors.CategoryTeal, KoshColors.CategoryBlue, KoshColors.BaseBlack),
    listOf(KoshColors.CategoryIndigo, KoshColors.CategoryPink, KoshColors.BaseBlack),
)

private fun gradientFor(bank: String?): List<Color> {
    val index = (bank?.hashCode() ?: 0).let { if (it < 0) -it else it } % ACCOUNT_GRADIENTS.size
    return ACCOUNT_GRADIENTS[index]
}

/**
 * Real per-account spend, no fabricated card number or expiry — only what the SMS
 * parser actually recognized (bank name + masked last-digits suffix).
 */
@Composable
fun AccountCard(summary: AccountSummary, amountsHidden: Boolean) {
    val gradientColors = gradientFor(summary.bank)
    val glowColor = gradientColors.first()
    val shape = RoundedCornerShape(20.dp)

    Box(
        modifier = Modifier
            .width(260.dp)
            .height(156.dp)
            .shadow(elevation = 12.dp, shape = shape, ambientColor = glowColor.copy(alpha = 0.55f), spotColor = glowColor.copy(alpha = 0.55f))
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = gradientColors,
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                ),
            ),
    ) {
        // Glossy corner highlight — a diagonal light fade from the top-left, the
        // detail that reads as a "premium glass card" rather than a flat gradient.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color.White.copy(alpha = 0.18f), Color.Transparent),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                    ),
                ),
        )
        // Bottom vignette — subtle darkening for depth, grounding the text.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(0f to Color.Transparent, 0.45f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.2f)),
                    ),
                ),
        )

        Column(modifier = Modifier.fillMaxSize().padding(Spacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    summary.bank ?: "Account",
                    style = MaterialTheme.typography.bodyMedium,
                    color = KoshColors.PrimaryText,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "•• ${summary.accountSuffix}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = KoshColors.PrimaryText.copy(alpha = 0.85f),
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(
                        "↓ Debit",
                        style = MaterialTheme.typography.labelSmall,
                        color = KoshColors.PrimaryText.copy(alpha = 0.75f),
                    )
                    Text(
                        formatRupees(summary.monthDebit, amountsHidden),
                        style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                        color = KoshColors.PrimaryText,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                    Text(
                        "↑ Credit",
                        style = MaterialTheme.typography.labelSmall,
                        color = KoshColors.PrimaryText.copy(alpha = 0.75f),
                    )
                    Text(
                        formatRupees(summary.monthCredit, amountsHidden),
                        style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                        color = KoshColors.PrimaryText,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                "${summary.debitCount} debit · ${summary.creditCount} credit",
                style = MaterialTheme.typography.labelSmall,
                color = KoshColors.PrimaryText.copy(alpha = 0.7f),
            )
        }
    }
}
