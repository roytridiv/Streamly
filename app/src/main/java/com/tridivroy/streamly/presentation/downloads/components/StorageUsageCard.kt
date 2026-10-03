package com.tridivroy.streamly.presentation.downloads.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tridivroy.streamly.R
import com.tridivroy.streamly.core.storage.StorageUsage
import com.tridivroy.streamly.core.theme.SageMint
import com.tridivroy.streamly.core.theme.StorageOther
import com.tridivroy.streamly.core.theme.StreamlyShape
import com.tridivroy.streamly.core.theme.StreamlyType
import com.tridivroy.streamly.core.theme.TextMuted
import com.tridivroy.streamly.presentation.common.formatBytes

/**
 * The device-storage card: a 10dp segmented bar with Streamly's share in mint (plus a glow), the rest
 * of the volume's used space in grey, and free space left as the track, with a legend below.
 */
@Composable
fun StorageUsageCard(
    storage: StorageUsage,
    modifier: Modifier = Modifier,
) {
    val streamlyFraction by animateFloatAsState(
        targetValue = storage.streamlyFraction,
        animationSpec = tween(300),
        label = "streamlyShare",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(StreamlyShape.Card)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.downloads_device_storage),
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
            )
            Text(
                text = stringResource(
                    R.string.downloads_storage_used,
                    formatBytes(storage.usedBytes),
                    formatBytes(storage.totalBytes),
                ),
                style = StreamlyType.MonoValue,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.background),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (streamlyFraction > 0f) {
                Box(
                    Modifier
                        .weight(streamlyFraction)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp))
                        .mintGlow()
                        .background(SageMint),
                )
            }
            if (storage.otherFraction > 0f) {
                Box(
                    Modifier
                        .weight(storage.otherFraction)
                        .fillMaxHeight()
                        .background(StorageOther),
                )
            }
            // The remaining weight is free space, left as the track colour.
            val free = (1f - streamlyFraction - storage.otherFraction).coerceAtLeast(0f)
            if (free > 0f) {
                Box(Modifier.weight(free).fillMaxHeight())
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            LegendEntry(
                color = SageMint,
                label = stringResource(R.string.downloads_legend_streamly, formatBytes(storage.streamlyBytes)),
            )
            LegendEntry(
                color = StorageOther,
                label = stringResource(R.string.downloads_legend_other, formatBytes(storage.otherBytes)),
            )
            LegendEntry(
                color = MaterialTheme.colorScheme.background,
                outlineColor = StorageOther,
                label = stringResource(R.string.downloads_legend_free, formatBytes(storage.freeBytes)),
            )
        }
    }
}

@Composable
private fun LegendEntry(
    color: Color,
    label: String,
    outlineColor: Color? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
                .then(
                    if (outlineColor != null) {
                        Modifier.drawBehind {
                            drawRoundRect(
                                color = outlineColor,
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()),
                            )
                        }
                    } else {
                        Modifier
                    },
                ),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** The handoff's `box-shadow: 0 0 10px mint@60%` under the Streamly segment. */
private fun Modifier.mintGlow(): Modifier = shadow(
    elevation = 6.dp,
    ambientColor = SageMint,
    spotColor = SageMint,
)

/**
 * The faded divider: a 1dp rule that fades to nothing over its outer 48dp, so it reads as a soft
 * separation rather than a hard line across the screen.
 */
@Composable
fun FadedDivider(modifier: Modifier = Modifier) {
    val line = MaterialTheme.colorScheme.outlineVariant
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .drawBehind {
                val fade = 48.dp.toPx().coerceAtMost(size.width / 2f)
                val stops = if (size.width <= 0f) {
                    arrayOf(0f to Color.Transparent, 1f to Color.Transparent)
                } else {
                    arrayOf(
                        0f to Color.Transparent,
                        (fade / size.width) to line,
                        (1f - fade / size.width) to line,
                        1f to Color.Transparent,
                    )
                }
                drawRect(
                    brush = Brush.horizontalGradient(colorStops = stops),
                    topLeft = Offset.Zero,
                    size = Size(size.width, size.height),
                )
            },
    )
}
