package com.tridivroy.streamly.presentation.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import com.tridivroy.streamly.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tridivroy.streamly.core.theme.StreamlyBrand
import com.tridivroy.streamly.core.theme.StreamlyIcons
import com.tridivroy.streamly.core.theme.StreamlyShape
import com.tridivroy.streamly.core.theme.StreamlyType
import com.tridivroy.streamly.core.theme.TextMuted
import com.tridivroy.streamly.domain.model.Video
import com.tridivroy.streamly.presentation.common.formatCount
import com.tridivroy.streamly.presentation.common.formatDuration
import com.tridivroy.streamly.presentation.common.formatRelativeAge

/**
 * A Home feed card: 16:9 thumbnail with a mint duration badge and, when the video is saved offline,
 * a frosted "Offline" tag; below it the channel avatar, title and meta line.
 */
@Composable
fun VideoCard(
    video: Video,
    isDownloaded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(StreamlyShape.Card)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(StreamlyShape.Card),
        ) {
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = null,
                placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                error = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
            )

            if (video.duration > 0) {
                DurationBadge(
                    duration = video.duration,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp),
                )
            }

            if (isDownloaded) {
                OfflineTag(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp),
                )
            }
        }

        Row(
            modifier = Modifier.padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            ChannelAvatar(video = video, size = 36.dp)

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                videoMetaLine(video)?.let { meta ->
                    Text(text = meta, style = MaterialTheme.typography.bodySmall, color = TextMuted, maxLines = 1)
                }
            }
        }
    }
}

/** The mint duration stamp used on every thumbnail in the app. */
@Composable
fun DurationBadge(
    duration: Long,
    modifier: Modifier = Modifier,
    horizontalPadding: Int = 7,
    verticalPadding: Int = 4,
) {
    Box(
        modifier = modifier
            .clip(StreamlyShape.DurationBadge)
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = horizontalPadding.dp, vertical = verticalPadding.dp),
    ) {
        Text(
            text = formatDuration(duration),
            style = StreamlyType.Duration,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

/** The frosted "✓ Offline" tag on a downloaded video's thumbnail. */
@Composable
fun OfflineTag(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(StreamlyShape.DurationBadge)
            .background(StreamlyBrand.Scrim)
            .padding(horizontal = 7.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = StreamlyIcons.Check,
            contentDescription = null,
            tint = StreamlyBrand.MintTextOnMedia,
            modifier = Modifier.size(11.dp),
        )
        Text(
            text = stringResource(R.string.home_offline_tag),
            style = MaterialTheme.typography.labelMedium,
            color = StreamlyBrand.MintTextOnMedia,
        )
    }
}

/**
 * The channel's avatar as a rounded tile, falling back to its initial on a tinted square when there
 * is no image — which is also what shows while the image loads.
 */
@Composable
fun ChannelAvatar(
    video: Video,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    val cornerScale = size / 3f
    val initialFallback = ColorPainter(MaterialTheme.colorScheme.surfaceVariant)
    Box(
        modifier = modifier
            .size(size)
            .clip(StreamlyShape.asymmetricPill(cornerScale * 2))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (video.channel.avatarUrl.isNotBlank()) {
            AsyncImage(
                model = video.channel.avatarUrl,
                contentDescription = null,
                placeholder = initialFallback,
                error = initialFallback,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size),
            )
        } else {
            Text(
                text = video.channel.initial,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/**
 * "Nordlys Films · 1.2M views · 3 days ago", dropping whichever parts the source did not provide so
 * the line never reads "0 views" or " ·  · ".
 */
@Composable
fun videoMetaLine(video: Video, includeChannel: Boolean = true): String? {
    val parts = buildList {
        if (includeChannel) video.channel.name.takeIf { it.isNotBlank() }?.let(::add)
        video.stats.viewCount.takeIf { it > 0 }?.let { add(stringResource(R.string.home_views, formatCount(it))) }
        formatRelativeAge(video.stats.publishedAtEpochSeconds)?.let(::add)
        // With no channel or stats at all, the category still says something useful about the video.
        if (isEmpty()) video.category.takeIf { it.isNotBlank() }?.let(::add)
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}
