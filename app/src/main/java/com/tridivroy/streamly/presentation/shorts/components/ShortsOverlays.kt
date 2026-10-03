package com.tridivroy.streamly.presentation.shorts.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import com.tridivroy.streamly.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tridivroy.streamly.core.theme.SageMint
import com.tridivroy.streamly.core.theme.StreamlyBoltGlyph
import com.tridivroy.streamly.core.theme.StreamlyBrand
import com.tridivroy.streamly.core.theme.StreamlyIcons
import com.tridivroy.streamly.core.theme.StreamlyShape
import com.tridivroy.streamly.core.theme.StreamlyType
import com.tridivroy.streamly.domain.model.Video
import com.tridivroy.streamly.presentation.common.formatCount

/** The glowing bolt, "Shorts", and the mute toggle and page position on the right. */
@Composable
fun ShortsHeader(
    pageNumber: Int,
    pageCount: Int,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            StreamlyBoltGlyph(
                color = SageMint,
                modifier = Modifier
                    .size(16.dp)
                    // Stands in for the prototype's drop-shadow glow on the glyph.
                    .shadow(elevation = 6.dp, ambientColor = SageMint, spotColor = SageMint),
            )
            Text(
                text = stringResource(R.string.nav_shorts),
                style = MaterialTheme.typography.titleMedium,
                color = StreamlyBrand.OnMedia,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MuteButton(isMuted = isMuted, onClick = onToggleMute)
            Text(
                text = "$pageNumber / $pageCount",
                style = StreamlyType.Eyebrow,
                color = StreamlyBrand.OnMediaVariant,
            )
        }
    }
}

/** A smaller frosted tile in the rail's style. It consumes its own tap, so it never toggles playback. */
@Composable
private fun MuteButton(
    isMuted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(StreamlyShape.FrostedTile)
            .background(StreamlyBrand.FrostedSurface)
            .border(1.dp, StreamlyBrand.FrostedBorder, StreamlyShape.FrostedTile)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (isMuted) StreamlyIcons.VolumeOff else StreamlyIcons.VolumeOn,
            contentDescription = stringResource(if (isMuted) R.string.shorts_unmute else R.string.shorts_mute),
            tint = StreamlyBrand.OnMedia,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** One entry in [ShortsActionRail]. [onClick] `null` renders the tile as a read-only stat. */
data class ShortsAction(
    val icon: ImageVector,
    val contentDescription: String,
    val count: Long,
    val active: Boolean = false,
    val onClick: (() -> Unit)? = null,
)

/**
 * The right rail: 50dp frosted squares at 18dp radius with a white hairline, counts beneath.
 *
 * The handoff specifies `backdrop-filter: blur(14px)`. Compose's `Modifier.blur` does not sample what
 * is behind a composable (and is API 31+ anyway), so these use a translucent dark fill over the
 * video, which reads the same against the frames and works on every supported version.
 */
@Composable
fun ShortsActionRail(
    actions: List<ShortsAction>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        actions.forEach { action -> RailTile(action) }
    }
}

@Composable
private fun RailTile(action: ShortsAction) {
    val tint by animateColorAsState(
        targetValue = if (action.active) SageMint else StreamlyBrand.OnMedia,
        animationSpec = tween(TRANSITION_MS),
        label = "railTint",
    )
    val background by animateColorAsState(
        targetValue = if (action.active) StreamlyBrand.MintTintOnMedia else StreamlyBrand.FrostedSurface,
        animationSpec = tween(TRANSITION_MS),
        label = "railBackground",
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(StreamlyShape.FrostedTile)
                .background(background)
                .border(1.dp, StreamlyBrand.FrostedBorder, StreamlyShape.FrostedTile)
                .then(
                    if (action.onClick != null) Modifier.clickable(onClick = action.onClick) else Modifier,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = action.contentDescription,
                tint = tint,
                modifier = Modifier.size(22.dp),
            )
        }
        if (action.count > 0) {
            Text(
                text = formatCount(action.count),
                style = MaterialTheme.typography.labelMedium,
                color = StreamlyBrand.OnMedia,
            )
        }
    }
}

/** Bottom-left: avatar, @handle, Follow, caption and the sound row. */
@Composable
fun ShortsOverlay(
    video: Video,
    isFollowing: Boolean,
    onFollowClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(StreamlyBrand.FrostedSurface),
                contentAlignment = Alignment.Center,
            ) {
                if (video.channel.avatarUrl.isNotBlank()) {
                    AsyncImage(
                        model = video.channel.avatarUrl,
                        contentDescription = null,
                        placeholder = ColorPainter(Color.Transparent),
                        error = ColorPainter(Color.Transparent),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(34.dp),
                    )
                } else {
                    Text(
                        text = video.channel.initial,
                        style = MaterialTheme.typography.labelSmall,
                        color = StreamlyBrand.OnMedia,
                    )
                }
            }

            video.channel.handle.takeIf { it.isNotBlank() }?.let { handle ->
                Text(
                    text = "@$handle",
                    style = MaterialTheme.typography.titleSmall,
                    color = StreamlyBrand.OnMedia,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                FollowPill(isFollowing = isFollowing, onClick = onFollowClick)
            }
        }

        // The caption: the title, with the description beneath when there is one.
        Text(
            text = video.title,
            style = MaterialTheme.typography.bodyLarge,
            color = StreamlyBrand.OnMedia,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (video.description.isNotBlank()) {
            Text(
                text = video.description,
                style = MaterialTheme.typography.bodySmall,
                color = StreamlyBrand.OnMediaVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        video.soundLabel?.takeIf { it.isNotBlank() }?.let { sound ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = StreamlyIcons.MusicNote,
                    contentDescription = null,
                    tint = StreamlyBrand.OnMediaVariant,
                    modifier = Modifier.size(13.dp),
                )
                Text(
                    text = sound,
                    style = MaterialTheme.typography.labelMedium,
                    color = StreamlyBrand.OnMediaVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun FollowPill(
    isFollowing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = StreamlyShape.asymmetricPill(28.dp)
    Box(
        modifier = modifier
            .height(28.dp)
            .clip(shape)
            .background(if (isFollowing) Color.Transparent else StreamlyBrand.MintTintOnMedia)
            .border(1.dp, SageMint, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(if (isFollowing) R.string.shorts_following else R.string.shorts_follow),
            style = MaterialTheme.typography.labelSmall,
            color = StreamlyBrand.MintTextOnMedia,
            maxLines = 1,
        )
    }
}

/** The 2dp mint playback bar that sits just above the nav. */
@Composable
fun ShortsProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .height(2.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(StreamlyBrand.OnMedia.copy(alpha = 0.2f)),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .width(maxWidth * fraction.coerceIn(0f, 1f))
                .background(SageMint),
        )
    }
}

private const val TRANSITION_MS = 200
