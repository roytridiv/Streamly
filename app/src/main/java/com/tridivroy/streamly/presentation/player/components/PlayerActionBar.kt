package com.tridivroy.streamly.presentation.player.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tridivroy.streamly.R
import com.tridivroy.streamly.core.theme.SageMint
import com.tridivroy.streamly.core.theme.StreamlyIcons
import com.tridivroy.streamly.core.theme.StreamlyShape
import com.tridivroy.streamly.domain.model.DownloadStatus
import com.tridivroy.streamly.domain.model.VideoDownload
import com.tridivroy.streamly.presentation.common.formatCount
import kotlin.math.roundToInt

/**
 * Like / Share / Download, three 44dp buttons with the Download button 1.45× wider.
 *
 * Download carries the whole offline lifecycle: idle, a progress ring with a percentage while the
 * background fills left-to-right in MintTint, then a mint "Offline ready". Its states come straight
 * from the existing [VideoDownload] flow.
 */
@Composable
fun PlayerActionBar(
    isLiked: Boolean,
    likeCount: Long,
    download: VideoDownload?,
    onLikeClick: () -> Unit,
    onShareClick: () -> Unit,
    onDownloadClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ActionButton(
            icon = if (isLiked) StreamlyIcons.HeartFilled else StreamlyIcons.Heart,
            label = formatCount(likeCount),
            active = isLiked,
            onClick = onLikeClick,
            modifier = Modifier.weight(1f),
        )
        ActionButton(
            icon = StreamlyIcons.Share,
            label = stringResource(R.string.player_share),
            active = false,
            onClick = onShareClick,
            modifier = Modifier.weight(1f),
        )
        DownloadButton(
            download = download,
            onClick = onDownloadClick,
            modifier = Modifier.weight(DOWNLOAD_WEIGHT),
        )
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val border by animateColorAsState(
        targetValue = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = tween(TRANSITION_MS),
        label = "actionBorder",
    )
    val background by animateColorAsState(
        targetValue = if (active) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        animationSpec = tween(TRANSITION_MS),
        label = "actionBackground",
    )
    val content by animateColorAsState(
        targetValue = if (active) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(TRANSITION_MS),
        label = "actionContent",
    )

    Row(
        modifier = modifier
            .height(HEIGHT)
            .clip(StreamlyShape.ActionButton)
            .background(background)
            .border(1.dp, border, StreamlyShape.ActionButton)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = content, modifier = Modifier.size(19.dp))
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = content, maxLines = 1)
    }
}

@Composable
private fun DownloadButton(
    download: VideoDownload?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val status = download?.status
    val percent = download?.progressPercent ?: 0f
    val isBusy = status == DownloadStatus.Queued ||
        status == DownloadStatus.Downloading ||
        status == DownloadStatus.Paused
    val isDone = status == DownloadStatus.Downloaded

    val border by animateColorAsState(
        targetValue = if (isBusy || isDone) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outlineVariant
        },
        animationSpec = tween(TRANSITION_MS),
        label = "downloadBorder",
    )
    val content by animateColorAsState(
        targetValue = if (isBusy || isDone) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(TRANSITION_MS),
        label = "downloadContent",
    )
    // The fill tracks real progress, so it is eased rather than snapped between poll intervals.
    val fillFraction by animateFloatAsState(
        targetValue = if (isBusy) (percent / 100f).coerceIn(0f, 1f) else 0f,
        animationSpec = tween(FILL_MS),
        label = "downloadFill",
    )

    BoxWithConstraints(
        modifier = modifier
            .height(HEIGHT)
            .clip(StreamlyShape.ActionButton)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, border, StreamlyShape.ActionButton)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (fillFraction > 0f) {
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .width(maxWidth * fillFraction)
                    .background(MaterialTheme.colorScheme.primaryContainer),
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            when {
                isBusy -> ProgressRing(fraction = (percent / 100f).coerceIn(0f, 1f), color = SageMint)
                isDone -> Icon(StreamlyIcons.Check, null, tint = content, modifier = Modifier.size(18.dp))
                else -> Icon(StreamlyIcons.Download, null, tint = content, modifier = Modifier.size(18.dp))
            }
            Text(
                text = downloadLabel(status, percent),
                style = MaterialTheme.typography.labelLarge,
                color = content,
                maxLines = 1,
            )
            // While a download is in flight the whole button cancels it. The cross says so, instead of
            // leaving "tap again to cancel" as something the user has to guess.
            if (isBusy) {
                Icon(
                    imageVector = StreamlyIcons.Close,
                    contentDescription = stringResource(R.string.player_download_cancel),
                    tint = content,
                    modifier = Modifier.size(15.dp),
                )
            }
        }
    }
}

/** The 18dp ring that replaces the download icon while a download is in flight. */
@Composable
private fun ProgressRing(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .size(18.dp)
            .drawBehind {
                val stroke = Stroke(width = 2.2.dp.toPx())
                val inset = stroke.width / 2f
                val arcSize = Size(size.width - stroke.width, size.height - stroke.width)
                drawArc(
                    color = color.copy(alpha = 0.25f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                    size = arcSize,
                    style = stroke,
                )
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * fraction,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                    size = arcSize,
                    style = stroke,
                )
            },
    )
}

@Composable
private fun downloadLabel(status: DownloadStatus?, percent: Float): String = when (status) {
    null -> stringResource(R.string.player_download)
    DownloadStatus.Queued -> stringResource(R.string.player_download_queued)
    DownloadStatus.Downloading -> stringResource(R.string.player_download_percent, percent.roundToInt())
    DownloadStatus.Paused -> stringResource(R.string.download_status_paused)
    DownloadStatus.Downloaded -> stringResource(R.string.player_offline_ready)
    DownloadStatus.Failed -> stringResource(R.string.player_download_failed)
}

private val HEIGHT = 44.dp
private const val DOWNLOAD_WEIGHT = 1.45f
private const val TRANSITION_MS = 200
private const val FILL_MS = 250
