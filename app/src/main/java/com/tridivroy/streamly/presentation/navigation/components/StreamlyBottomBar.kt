package com.tridivroy.streamly.presentation.navigation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tridivroy.streamly.core.theme.StreamlyShape
import com.tridivroy.streamly.core.theme.StreamlyType
import com.tridivroy.streamly.core.theme.TextMuted
import com.tridivroy.streamly.core.theme.TextPrimary

/** One tab in [StreamlyBottomBar] / [StreamlyNavRail]. */
data class NavBarItem(
    val label: String,
    val icon: ImageVector,
    val selected: Boolean,
    /** Mint count badge; `0` hides it. */
    val badgeCount: Int = 0,
    val onClick: () -> Unit,
)

/**
 * The bottom nav from the handoff: 66dp tall, a hairline top edge, and an asymmetric mint pill that
 * fills behind the selected item's icon.
 */
@Composable
fun StreamlyBottomBar(
    items: List<NavBarItem>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .drawTopHairline()
            // Horizontal as well as bottom: in landscape the gesture bar or a cutout can sit on a side
            // edge, and the bar's end items would otherwise fall underneath it.
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
            .heightIn(min = BAR_HEIGHT)
            .padding(horizontal = 18.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.Top,
    ) {
        items.forEach { item ->
            NavBarCell(item = item, modifier = Modifier.weight(1f, fill = false))
        }
    }
}

@Composable
private fun NavBarCell(
    item: NavBarItem,
    modifier: Modifier = Modifier,
) {
    val contentColor by animateColorAsState(
        targetValue = if (item.selected) MaterialTheme.colorScheme.onPrimaryContainer else TextMuted,
        animationSpec = tween(TRANSITION_MS),
        label = "navItemColor",
    )
    val pillColor by animateColorAsState(
        targetValue = if (item.selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        animationSpec = tween(TRANSITION_MS),
        label = "navItemPill",
    )

    Box(modifier.widthIn(max = 96.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .selectable(
                    selected = item.selected,
                    role = Role.Tab,
                    onClick = item.onClick,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(width = 52.dp, height = 28.dp)
                    .clip(StreamlyShape.asymmetricPill(28.dp))
                    .background(pillColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(21.dp),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }

        if (item.badgeCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 2.dp, end = 18.dp)
                    .widthIn(min = 16.dp)
                    .height(16.dp)
                    .clip(StreamlyShape.asymmetricPill(16.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = item.badgeCount.toString(),
                    style = StreamlyType.Duration,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

/**
 * The same tabs as a side rail, for medium and expanded widths (tablets, unfolded foldables). The
 * handoff is phone-only; this keeps the adaptive layout the app already supported, restyled with
 * the same mint pill.
 */
@Composable
fun StreamlyNavRail(
    items: List<NavBarItem>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            // Start + vertical, not all four sides: the rail hugs the leading edge, so it needs to clear a
            // side cutout or side navigation bar there, plus the status bar and gesture strip. Padding
            // its trailing edge too was what made it look oversized in landscape.
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Start + WindowInsetsSides.Vertical))
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        items.forEach { item -> NavBarCell(item = item, modifier = Modifier.widthIn(min = 80.dp)) }
    }
}

/** A 1dp top edge at the handoff's TextPrimary-at-6% — lighter than the general Outline token. */
private fun Modifier.drawTopHairline(): Modifier = drawBehind {
    drawRect(color = TextPrimary.copy(alpha = 0.06f), size = Size(size.width, 1.dp.toPx()))
}

private val BAR_HEIGHT = 66.dp
private const val TRANSITION_MS = 200
