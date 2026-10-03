package com.tridivroy.streamly.presentation.downloads.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tridivroy.streamly.R
import com.tridivroy.streamly.core.theme.StreamlyIcons
import com.tridivroy.streamly.core.theme.StreamlyShape
import com.tridivroy.streamly.core.theme.StreamlyType
import com.tridivroy.streamly.core.theme.TextMuted
import com.tridivroy.streamly.domain.model.DownloadStatus
import com.tridivroy.streamly.domain.model.VideoDownload
import com.tridivroy.streamly.presentation.common.formatBytes
import com.tridivroy.streamly.presentation.home.components.DurationBadge
import kotlin.math.roundToInt

/**
 * One row in the Downloads list: a 118dp thumbnail, the title, and either the mint "Offline Ready"
 * tag or — while saving — a pulsing percentage over a 3dp progress bar.
 */
@Composable
fun DownloadRow(
    download: VideoDownload,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onPauseToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isPaused = download.status == DownloadStatus.Paused
    val isBusy = download.status == DownloadStatus.Queued ||
        download.status == DownloadStatus.Downloading ||
        isPaused

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .width(118.dp)
                .aspectRatio(16f / 9f)
                .clip(StreamlyShape.SmallThumbnail),
        ) {
            AsyncImage(
                model = download.video.thumbnailUrl,
                contentDescription = null,
                placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                error = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(118.dp)
                    .aspectRatio(16f / 9f),
            )
            if (download.video.duration > 0) {
                DurationBadge(
                    duration = download.video.duration,
                    horizontalPadding = 5,
                    verticalPadding = 3,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp),
                )
            }
        }

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = download.video.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                when (download.status) {
                    DownloadStatus.Downloaded -> OfflineReadyTag()
                    DownloadStatus.Paused -> Text(
                        text = stringResource(R.string.download_status_paused),
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted,
                    )
                    DownloadStatus.Failed -> Text(
                        text = stringResource(R.string.download_status_failed),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                    )

                    DownloadStatus.Queued, DownloadStatus.Downloading -> PulsingPercent(
                        percent = download.progressPercent,
                    )
                }
                Text(
                    text = sizeAndQuality(download),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted,
                    maxLines = 1,
                )
            }

            if (isBusy) {
                val fraction = download.progressPercent?.let { (it / 100f).coerceIn(0f, 1f) }
                if (fraction != null) {
                    DownloadProgressBar(fraction)
                } else {
                    // Size still unknown, so there is no fraction to draw: Material's indeterminate bar
                    // is the right thing here and has no stop indicator to worry about.
                    LinearProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                    )
                }
            }
        }

        if (isBusy) {
            RowIconButton(
                icon = if (isPaused) StreamlyIcons.Play else StreamlyIcons.Pause,
                contentDescription = stringResource(
                    if (isPaused) R.string.downloads_resume else R.string.downloads_pause,
                ),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                onClick = onPauseToggle,
            )
        }

        RowIconButton(
            icon = StreamlyIcons.Trash,
            contentDescription = stringResource(R.string.downloads_remove),
            tint = TextMuted,
            onClick = onRemove,
        )
    }
}

/**
 * The handoff's plain 3dp bar, drawn rather than taken from Material.
 *
 * `LinearProgressIndicator` in material3 1.3 paints a "stop indicator" dot at the end of the track,
 * which showed up on device as a stray mint dot floating to the right of the row -- and its track was
 * invisible because `trackColor` was the surface behind it.
 */
@Composable
private fun DownloadProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(3.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(MaterialTheme.colorScheme.outlineVariant),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .width(maxWidth * fraction)
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}

@Composable
private fun RowIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(11.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(17.dp))
    }
}

/** The mint "✓ Offline Ready" tag. */
@Composable
fun OfflineReadyTag(modifier: Modifier = Modifier) {
    val shape = StreamlyShape.asymmetricPill(18.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = StreamlyIcons.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(10.dp),
        )
        Text(
            text = stringResource(R.string.downloads_offline_ready),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 1,
        )
    }
}

/** The percentage, pulsing between 55% and full opacity, as the prototype's `pulse` keyframes do. */
@Composable
private fun PulsingPercent(
    percent: Float?,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "percentPulse")
    val alpha by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "percentAlpha",
    )

    Text(
        text = percent?.let { "${it.roundToInt()}%" } ?: stringResource(R.string.download_status_queued),
        style = StreamlyType.Duration,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = modifier.alpha(alpha),
        maxLines = 1,
    )
}

/** "612 MB · 1080p", dropping the resolution when the source never declared one. */
@Composable
private fun sizeAndQuality(download: VideoDownload): String {
    val size = formatBytes(download.bytesDownloaded)
    val quality = download.videoHeight?.let { "${it}p" }
    return listOfNotNull(size, quality).joinToString(" · ")
}
