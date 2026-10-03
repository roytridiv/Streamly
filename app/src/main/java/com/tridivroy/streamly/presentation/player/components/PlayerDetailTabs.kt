package com.tridivroy.streamly.presentation.player.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
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
import com.tridivroy.streamly.domain.model.Chapter
import com.tridivroy.streamly.domain.model.Video
import com.tridivroy.streamly.presentation.common.formatCount
import com.tridivroy.streamly.presentation.common.formatDuration
import com.tridivroy.streamly.presentation.home.components.ChannelAvatar
import com.tridivroy.streamly.presentation.home.components.DurationBadge
import com.tridivroy.streamly.presentation.home.components.videoMetaLine
import com.tridivroy.streamly.presentation.player.PlayerTab
import com.tridivroy.streamly.presentation.player.UpNextLayout

/**
 * The tab strip: three labels in a SoftCharcoal container with a Slate indicator pill that slides
 * between them. The pill carries the mint edge, so the selected tab reads as a cut-out rather than a
 * highlight.
 */
@Composable
fun PlayerTabRow(
    selected: PlayerTab,
    momentCount: Int,
    onSelect: (PlayerTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(StreamlyShape.Card)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(4.dp),
    ) {
        val tabWidth = maxWidth / PlayerTab.entries.size
        val indicatorOffset by animateDpAsState(
            targetValue = tabWidth * selected.ordinal,
            animationSpec = tween(durationMillis = INDICATOR_MS, easing = IndicatorEasing),
            label = "tabIndicator",
        )

        Box(
            Modifier
                .offset(x = indicatorOffset)
                .width(tabWidth)
                .height(TAB_HEIGHT)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.background)
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
        )

        Row(Modifier.fillMaxWidth()) {
            PlayerTab.entries.forEach { tab ->
                TabLabel(
                    tab = tab,
                    count = if (tab == PlayerTab.KeyMoments) momentCount else null,
                    selected = tab == selected,
                    onClick = { onSelect(tab) },
                    modifier = Modifier.width(tabWidth),
                )
            }
        }
    }
}

@Composable
private fun TabLabel(
    tab: PlayerTab,
    count: Int?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val content by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(200),
        label = "tabLabel",
    )
    Row(
        modifier = modifier
            .height(TAB_HEIGHT)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    ) {
        Text(
            text = stringResource(tab.labelRes()),
            style = MaterialTheme.typography.labelLarge,
            color = content,
            maxLines = 1,
        )
        if (count != null && count > 0) {
            Text(text = count.toString(), style = StreamlyType.Timestamp, color = TextMuted)
        }
    }
}

private fun PlayerTab.labelRes(): Int = when (this) {
    PlayerTab.Overview -> R.string.player_tab_overview
    PlayerTab.KeyMoments -> R.string.player_tab_key_moments
    PlayerTab.UpNext -> R.string.player_tab_up_next
}

/** Title, meta, the channel row with Subscribe, and the expandable description card. */
@Composable
fun OverviewTab(
    video: Video,
    isSubscribed: Boolean,
    descriptionExpanded: Boolean,
    onToggleDescription: () -> Unit,
    onSubscribeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = video.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            videoMetaLine(video, includeChannel = false)?.let { meta ->
                Text(text = meta, style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
        }

        if (video.channel.name.isNotBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                ChannelAvatar(video = video, size = 40.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = video.channel.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (video.channel.subscriberCount > 0) {
                        Text(
                            text = stringResource(
                                R.string.player_subscribers,
                                formatCount(video.channel.subscriberCount),
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMuted,
                        )
                    }
                }
                SubscribeBadge(isSubscribed = isSubscribed, onClick = onSubscribeClick)
            }
        }

        if (video.description.isNotBlank() || video.tags.isNotEmpty()) {
            DescriptionCard(
                description = video.description,
                tags = video.tags,
                expanded = descriptionExpanded,
                onToggle = onToggleDescription,
            )
        }
    }
}

/** Solid mint when not subscribed, an outlined "✓ Subscribed" once you are. */
@Composable
private fun SubscribeBadge(
    isSubscribed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = StreamlyShape.asymmetricPill(BADGE_HEIGHT)
    val background by animateColorAsState(
        targetValue = if (isSubscribed) androidx.compose.ui.graphics.Color.Transparent else MaterialTheme.colorScheme.primary,
        animationSpec = tween(200),
        label = "subscribeBackground",
    )
    val content by animateColorAsState(
        targetValue = if (isSubscribed) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onPrimary,
        animationSpec = tween(200),
        label = "subscribeContent",
    )

    Row(
        modifier = modifier
            .height(BADGE_HEIGHT)
            .clip(shape)
            .background(background)
            .border(1.dp, MaterialTheme.colorScheme.primary, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (isSubscribed) {
            Icon(StreamlyIcons.Check, null, tint = content, modifier = Modifier.size(13.dp))
        }
        Text(
            text = stringResource(
                if (isSubscribed) R.string.player_subscribed else R.string.player_subscribe,
            ),
            style = MaterialTheme.typography.labelSmall,
            color = content,
            maxLines = 1,
        )
    }
}

@Composable
private fun DescriptionCard(
    description: String,
    tags: List<String>,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(250),
        label = "descChevron",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(StreamlyShape.Card)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onToggle)
            .padding(horizontal = 15.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (tags.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                tags.forEach { tag ->
                    Text(
                        text = "#$tag",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }

        if (description.isNotBlank()) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (expanded) Int.MAX_VALUE else COLLAPSED_LINES,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(
                    if (expanded) R.string.player_show_less else R.string.player_show_more,
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Icon(
                imageVector = StreamlyIcons.ChevronDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .size(14.dp)
                    .rotate(chevronRotation),
            )
        }
    }
}

/**
 * Wrapping chips, one per chapter, each with a mono timestamp stamp. The chip containing the current
 * position wears a solid mint stamp so the list tracks playback.
 */
@Composable
fun KeyMomentsTab(
    chapters: List<Chapter>,
    currentPositionSeconds: Long,
    onSeekTo: (positionSeconds: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (chapters.isEmpty()) {
        Box(modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.player_no_key_moments),
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
            )
        }
        return
    }

    val activeIndex = chapters.indexOfLast { it.positionSeconds <= currentPositionSeconds }

    Column(
        modifier = modifier.padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.player_key_moments_hint),
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            chapters.forEachIndexed { index, chapter ->
                MomentChip(
                    chapter = chapter,
                    isActive = index == activeIndex,
                    onClick = { onSeekTo(chapter.positionSeconds) },
                )
            }
        }
    }
}

@Composable
private fun MomentChip(
    chapter: Chapter,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = StreamlyShape.asymmetricPill(CHIP_HEIGHT)
    Row(
        modifier = modifier
            .height(CHIP_HEIGHT)
            .clip(shape)
            .background(
                if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
            )
            .border(
                1.dp,
                if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape,
            )
            .clickable(onClick = onClick)
            .padding(start = 6.dp, end = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        val stampShape = StreamlyShape.asymmetricPill(28.dp)
        Box(
            Modifier
                .clip(stampShape)
                .background(
                    if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background,
                )
                .padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            Text(
                text = formatDuration(chapter.positionSeconds),
                style = StreamlyType.Duration,
                color = if (isActive) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                },
            )
        }
        Text(
            text = chapter.label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
        )
    }
}

/** "Autoplay on · N clips" plus a List ↔ Carousel toggle. */
@Composable
fun UpNextTab(
    videos: List<Video>,
    layout: UpNextLayout,
    onToggleLayout: () -> Unit,
    onVideoClick: (videoId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (videos.isEmpty()) {
        Box(modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.player_up_next_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
            )
        }
        return
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.player_autoplay_count, videos.size),
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
            )
            Row(
                modifier = Modifier
                    .height(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                    .clickable(onClick = onToggleLayout)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = if (layout == UpNextLayout.List) {
                        StreamlyIcons.LayoutColumns
                    } else {
                        StreamlyIcons.LayoutRows
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(15.dp),
                )
                Text(
                    text = stringResource(
                        if (layout == UpNextLayout.List) {
                            R.string.player_layout_carousel
                        } else {
                            R.string.player_layout_list
                        },
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        when (layout) {
            UpNextLayout.Carousel -> LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(videos, key = { it.id }) { video ->
                    UpNextCard(video = video, onClick = { onVideoClick(video.id) })
                }
            }

            UpNextLayout.List -> Column(
                modifier = Modifier.padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                videos.forEach { video ->
                    UpNextRow(video = video, onClick = { onVideoClick(video.id) })
                }
            }
        }
    }
}

/** 200dp carousel card. */
@Composable
private fun UpNextCard(
    video: Video,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(200.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Thumbnail(video = video, modifier = Modifier.fillMaxWidth(), cornerRadius = 14)
        Text(
            text = video.title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        video.channel.name.takeIf { it.isNotBlank() }?.let { channel ->
            Text(text = channel, style = MaterialTheme.typography.labelMedium, color = TextMuted, maxLines = 1)
        }
    }
}

/** 136dp list row. */
@Composable
private fun UpNextRow(
    video: Video,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Thumbnail(video = video, modifier = Modifier.width(136.dp), cornerRadius = 12)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 2.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = video.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            videoMetaLine(video)?.let { meta ->
                Text(text = meta, style = MaterialTheme.typography.labelMedium, color = TextMuted, maxLines = 1)
            }
        }
    }
}

@Composable
private fun Thumbnail(
    video: Video,
    modifier: Modifier = Modifier,
    cornerRadius: Int,
) {
    Box(
        modifier
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(cornerRadius.dp)),
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
                horizontalPadding = 5,
                verticalPadding = 3,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp),
            )
        }
    }
}

private val TAB_HEIGHT = 38.dp
private val BADGE_HEIGHT = 34.dp
private val CHIP_HEIGHT = 40.dp
private const val COLLAPSED_LINES = 2
private const val INDICATOR_MS = 300

/** `cubic-bezier(.3,.7,.2,1)` — the indicator's slide. */
private val IndicatorEasing = CubicBezierEasing(0.3f, 0.7f, 0.2f, 1f)
