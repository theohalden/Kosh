package dev.kosh.financetracker.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.kosh.financetracker.R

/**
 * Official Material Symbols "visibility"/"visibility_off" outline (Apache License
 * 2.0), bundled as two small vector drawables rather than pulling in the whole
 * material-icons-extended library for one icon pair.
 */
@Composable
fun VisibilityToggle(
    visible: Boolean,
    onClick: () -> Unit,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Image(
        painter = painterResource(if (visible) R.drawable.ic_visibility else R.drawable.ic_visibility_off),
        contentDescription = if (visible) "Hide amounts" else "Show amounts",
        colorFilter = ColorFilter.tint(tint),
        modifier = modifier
            .size(20.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    )
}
