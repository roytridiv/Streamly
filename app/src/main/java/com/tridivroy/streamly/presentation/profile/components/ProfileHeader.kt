package com.tridivroy.streamly.presentation.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tridivroy.streamly.R
import com.tridivroy.streamly.core.theme.SageMint
import com.tridivroy.streamly.core.theme.StreamlyIcons
import com.tridivroy.streamly.core.theme.StreamlyShape
import com.tridivroy.streamly.core.theme.StreamlyType
import com.tridivroy.streamly.core.theme.TextMuted
import com.tridivroy.streamly.domain.model.UserSession
import com.tridivroy.streamly.presentation.profile.WatchStats

/** Avatar, name, handle and the Premium badge, over a soft mint wash. */
@Composable
fun ProfileHeader(
    session: UserSession,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(StreamlyShape.Card)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .background(
                Brush.horizontalGradient(listOf(SageMint.copy(alpha = 0.10f), Color.Transparent)),
            )
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, StreamlyShape.Card)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Avatar(session = session, size = 64.dp)

        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                text = session.displayName,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "@${session.handle}",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                maxLines = 1,
            )
            if (session.isPremium) {
                PremiumBadge()
            }
        }
    }
}

/** The avatar tile, falling back to the account's initial while the image loads or if it fails. */
@Composable
fun Avatar(
    session: UserSession,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    val shape = StreamlyShape.asymmetricPill(size * 0.7f)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(1.dp, SageMint.copy(alpha = 0.35f), shape),
        contentAlignment = Alignment.Center,
    ) {
        if (session.avatarUrl.isNotBlank()) {
            AsyncImage(
                model = session.avatarUrl,
                contentDescription = null,
                placeholder = ColorPainter(Color.Transparent),
                error = ColorPainter(Color.Transparent),
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size),
            )
        } else {
            Text(
                text = session.initial,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun PremiumBadge(modifier: Modifier = Modifier) {
    val shape = StreamlyShape.asymmetricPill(22.dp)
    Row(
        modifier = modifier
            .height(22.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = StreamlyIcons.Spark,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(11.dp),
        )
        Text(
            text = stringResource(R.string.profile_premium),
            style = StreamlyType.Duration,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

/** Three figures across one card: watched, downloaded, favourited. */
@Composable
fun WatchStatsRow(
    stats: WatchStats,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(StreamlyShape.Card)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        StatTile(value = stats.videosWatched, label = stringResource(R.string.profile_stat_watched))
        StatDivider()
        StatTile(value = stats.downloads, label = stringResource(R.string.profile_stat_downloads))
        StatDivider()
        StatTile(value = stats.favourites, label = stringResource(R.string.profile_stat_favourites))
    }
}

@Composable
private fun StatTile(
    value: Int,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = TextMuted)
    }
}

@Composable
private fun StatDivider() {
    Box(
        Modifier
            .size(width = 1.dp, height = 32.dp)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}
