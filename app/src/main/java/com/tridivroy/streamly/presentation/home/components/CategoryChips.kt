package com.tridivroy.streamly.presentation.home.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.tridivroy.streamly.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tridivroy.streamly.core.theme.StreamlyShape

/**
 * The horizontally scrolling category row. Each chip is the brand's asymmetric pill: SoftCharcoal
 * with a hairline edge when idle, MintTint with a mint edge and MintText when selected.
 */
@Composable
fun CategoryChips(
    categories: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 2.dp, bottom = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = ALL_KEY) {
            CategoryChip(
                label = stringResource(R.string.home_category_all),
                selected = selected == null,
                onClick = { onSelect(null) },
            )
        }
        items(categories, key = { it }) { category ->
            CategoryChip(
                label = category,
                selected = category == selected,
                onClick = { onSelect(category) },
            )
        }
    }
}

@Composable
private fun CategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        animationSpec = tween(TRANSITION_MS),
        label = "chipBackground",
    )
    val border by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = tween(TRANSITION_MS),
        label = "chipBorder",
    )
    val content by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(TRANSITION_MS),
        label = "chipContent",
    )

    Box(
        modifier = modifier
            .height(CHIP_HEIGHT)
            .clip(StreamlyShape.asymmetricPill(CHIP_HEIGHT))
            .background(background)
            .border(1.dp, border, StreamlyShape.asymmetricPill(CHIP_HEIGHT))
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = content, maxLines = 1)
    }
}

private const val ALL_KEY = "all"
private val CHIP_HEIGHT = 34.dp
private const val TRANSITION_MS = 200
