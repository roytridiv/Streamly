package com.tridivroy.streamly.presentation.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.tridivroy.streamly.core.theme.LogoSizeTopBar
import com.tridivroy.streamly.core.theme.StreamlyLogoTile
import com.tridivroy.streamly.core.theme.StreamlyShape
import com.tridivroy.streamly.core.theme.StreamlyType
import com.tridivroy.streamly.core.theme.logoCornerRadius

/** One of the trailing icon buttons in [HomeTopBar]. */
data class TopBarAction(
    val icon: ImageVector,
    val contentDescription: String,
    /** Shows the handoff's 7dp mint dot on the icon's top-right. */
    val showDot: Boolean = false,
    val onClick: () -> Unit,
)

/**
 * Home's header: the 30dp logo tile beside the "streamly" wordmark, with 40dp icon buttons trailing.
 *
 * The handoff shows Search and Notifications in those slots. Streamly has neither feature, so they
 * carry the actions Home actually has — Refresh and Settings — at the handoff's size, radius and
 * spacing. Swapping in Search and a notification bell is a one-line change here once those exist.
 */
@Composable
fun HomeTopBar(
    actions: List<TopBarAction>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 10.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            StreamlyLogoTile(
                modifier = Modifier
                    .size(LogoSizeTopBar)
                    .clip(StreamlyShape.asymmetricPill(logoCornerRadius(LogoSizeTopBar) * 2)),
            )
            Text(
                text = "streamly",
                style = StreamlyType.Wordmark,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            actions.forEach { action -> TopBarIconButton(action) }
        }
    }
}

@Composable
private fun TopBarIconButton(action: TopBarAction) {
    Box {
        // material3 1.3 IconButton is circle-only, so this is a clipped, clickable box at the
        // handoff's 40dp / 12dp radius instead.
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(StreamlyShape.IconButton)
                .clickable(onClick = action.onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = action.contentDescription,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(22.dp),
            )
        }
        if (action.showDot) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 9.dp, end = 10.dp)
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}
