package com.tridivroy.streamly.presentation.navigation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tridivroy.streamly.R
import com.tridivroy.streamly.core.media.NowPlaying
import com.tridivroy.streamly.core.theme.StreamlyBrand
import com.tridivroy.streamly.core.theme.StreamlyIcons
import com.tridivroy.streamly.core.theme.StreamlyShape
import com.tridivroy.streamly.core.theme.TextMuted

/**
 * The docked mini-player that sits above the bottom bar on Home and Downloads while something is
 * loaded in the shared player: 58dp tall, a mint edge, and a 2dp mint progress line along the bottom.
 *
 * Tapping the body opens the Player; the button toggles playback without leaving the tab.
 */
@Composable
fun MiniPlayer(
    nowPlaying: NowPlaying,
    onClick: () -> Unit,
    onTogglePlay: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(HEIGHT)
            .clip(StreamlyShape.Card)
            // The handoff says "frosted SoftCharcoal at 88%", which assumes a backdrop blur behind it.
            // Compose has none, so 88% just let the feed show through: on device the card underneath and
            // its duration badge were legible straight through this bar. Opaque is the honest read of a
            // frosted surface without a blur to frost.
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, StreamlyBrand.GlowEdge, StreamlyShape.Card)
            .clickable(onClick = onClick)
            .padding(start = 7.dp, end = 2.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AsyncImage(
                model = nowPlaying.video.thumbnailUrl,
                contentDescription = null,
                placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                error = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 76.dp, height = 44.dp)
                    .clip(RoundedCornerShape(10.dp)),
            )

            Column(Modifier.weight(1f)) {
                Text(
                    text = nowPlaying.video.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                nowPlaying.video.channel.name.takeIf { it.isNotBlank() }?.let { channel ->
                    Text(
                        text = channel,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            IconButton(onClick = onTogglePlay, modifier = Modifier.size(44.dp)) {
                Icon(
                    imageVector = if (nowPlaying.isPlaying) StreamlyIcons.Pause else StreamlyIcons.Play,
                    contentDescription = stringResource(
                        if (nowPlaying.isPlaying) R.string.mini_player_pause else R.string.mini_player_play,
                    ),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp),
                )
            }

            // Ends playback rather than pausing it: without this the bar had no way off the screen
            // short of opening the Player and backing out of the whole tab.
            IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = StreamlyIcons.Close,
                    contentDescription = stringResource(R.string.mini_player_dismiss),
                    tint = TextMuted,
                    modifier = Modifier.size(17.dp),
                )
            }
        }

        // 2dp mint progress line along the bottom edge.
        BoxWithConstraints(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(2.dp),
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .width(maxWidth * nowPlaying.progress)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

private val HEIGHT = 58.dp
