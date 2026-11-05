package dev.kosh.financetracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.kosh.financetracker.ui.theme.KoshColors

/**
 * Standard elevated card shell — a soft shadow, a subtle border, and a faint
 * top-to-bottom lightening so surfaces read as distinct, raised layers against the
 * pure-black background rather than a flat color-contrast block.
 */
@Composable
fun KoshCard(
    modifier: Modifier = Modifier,
    containerColor: Color = KoshColors.Graphite,
    cornerRadius: Dp = 20.dp,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    val topTone = lerp(containerColor, Color.White, 0.05f)

    Box(
        modifier = modifier
            .shadow(elevation = 4.dp, shape = shape, ambientColor = Color.Black.copy(alpha = 0.4f), spotColor = Color.Black.copy(alpha = 0.4f))
            .clip(shape)
            .background(Brush.verticalGradient(colors = listOf(topTone, containerColor)))
            .border(1.dp, KoshColors.Divider.copy(alpha = 0.5f), shape),
    ) {
        Column(modifier = Modifier.fillMaxWidth(), content = content)
    }
}
