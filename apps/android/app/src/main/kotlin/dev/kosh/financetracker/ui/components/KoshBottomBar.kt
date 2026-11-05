package dev.kosh.financetracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.kosh.financetracker.navigation.KoshDestination
import dev.kosh.financetracker.ui.theme.KoshExtendedTheme
import dev.kosh.financetracker.ui.theme.Spacing

private data class BottomNavItem(
    val destination: KoshDestination,
    val label: String,
    val icon: @Composable (tint: Color) -> Unit,
)

private val ITEMS = listOf(
    BottomNavItem(KoshDestination.Overview, "Home") { tint -> Icon(Icons.Filled.Home, contentDescription = "Home", tint = tint) },
    BottomNavItem(KoshDestination.Trail, "Activity") { tint -> Icon(Icons.Filled.List, contentDescription = "Activity", tint = tint) },
    BottomNavItem(KoshDestination.Insights, "Spending") { tint -> InsightsIcon(tint = tint) },
    BottomNavItem(KoshDestination.Review, "Review") { tint -> Icon(Icons.Filled.CheckCircle, contentDescription = "Review", tint = tint) },
)

@Composable
fun KoshBottomBar(
    currentRoute: String?,
    reviewBadgeCount: Int,
    onNavigate: (KoshDestination) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(top = Spacing.sm, bottom = Spacing.xs),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            ITEMS.forEach { item ->
                val selected = currentRoute == item.destination.route
                BottomNavEntry(
                    item = item,
                    selected = selected,
                    badgeCount = if (item.destination == KoshDestination.Review) reviewBadgeCount else 0,
                    onClick = { onNavigate(item.destination) },
                )
            }
        }
    }
}

@Composable
private fun BottomNavEntry(
    item: BottomNavItem,
    selected: Boolean,
    badgeCount: Int,
    onClick: () -> Unit,
) {
    val contentColor = if (selected) {
        KoshExtendedTheme.colors.accentText
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = Modifier
            .selectable(selected = selected, onClick = onClick)
            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            item.icon(contentColor)
            if (badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .align(Alignment.TopEnd)
                        .clip(CircleShape)
                        .background(KoshExtendedTheme.colors.expense),
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(item.label, style = MaterialTheme.typography.labelSmall, color = contentColor)
    }
}
