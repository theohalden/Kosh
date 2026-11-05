package dev.kosh.financetracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.kosh.financetracker.core.finance.CategorySlice

@Composable
fun SegmentedBar(
    slices: List<CategorySlice>,
    colors: List<Color>,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(8.dp)) {
            slices.forEachIndexed { index, slice ->
                if (slice.fraction <= 0f) return@forEachIndexed
                Box(
                    modifier = Modifier
                        .fillMaxWidth(slice.fraction)
                        .height(8.dp)
                        .background(colors[index % colors.size]),
                )
            }
        }
    }
}
