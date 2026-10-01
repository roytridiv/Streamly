package com.tridivroy.streamly.presentation.shorts

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.tridivroy.streamly.R
import com.tridivroy.streamly.domain.model.Video
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private const val PROGRESS_POLL_MS = 200L

/** Stateful entry point: wires [ShortsViewModel] to the stateless [ShortsScreen]. */
@Composable
fun ShortsRoute(
    modifier: Modifier = Modifier,
    viewModel: ShortsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val shareChooserTitle = stringResource(R.string.shorts_share_chooser)

    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    is ShortsUiEffect.Share -> {
                        val send = Intent(Intent.ACTION_SEND)
                            .setType("text/plain")
                            .putExtra(Intent.EXTRA_SUBJECT, effect.video.title)
                            .putExtra(Intent.EXTRA_TEXT, "${effect.video.title}\n${effect.video.videoUrl}")
                        context.startActivity(Intent.createChooser(send, shareChooserTitle))
                    }
                }
            }
        }
    }

    // Pause when the screen is hidden (app backgrounded, share sheet over us); resume on return.
    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.onEvent(ShortsUiEvent.OnScreenStart) }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.onEvent(ShortsUiEvent.OnScreenStop) }

    ShortsScreen(
        uiState = uiState,
        playerFor = viewModel::playerFor,
        onEvent = viewModel::onEvent,
        modifier = modifier,
    )
}

@Composable
fun ShortsScreen(
    uiState: ShortsUiState,
    playerFor: (index: Int) -> Player?,
    onEvent: (ShortsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val screenModifier = modifier
        .fillMaxSize()
        .background(Color.Black)

    when (uiState) {
        ShortsUiState.Loading -> Box(screenModifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White)
        }

        ShortsUiState.Empty -> MessageContent(
            message = stringResource(R.string.shorts_empty),
            onRetry = { onEvent(ShortsUiEvent.Retry) },
            modifier = screenModifier,
        )

        is ShortsUiState.Error -> MessageContent(
            message = uiState.message ?: stringResource(R.string.shorts_error_generic),
            onRetry = { onEvent(ShortsUiEvent.Retry) },
            modifier = screenModifier,
        )

        is ShortsUiState.Success -> ShortsPager(
            state = uiState,
            playerFor = playerFor,
            onEvent = onEvent,
            modifier = screenModifier,
        )
    }
}

@Composable
private fun ShortsPager(
    state: ShortsUiState.Success,
    playerFor: (index: Int) -> Player?,
    onEvent: (ShortsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    // initialPage keeps the pager aligned with the ViewModel after rotation.
    val pagerState = rememberPagerState(initialPage = state.activeIndex) { state.videos.size }
    val currentOnEvent by rememberUpdatedState(onEvent)

    // Switch playback only once a swipe settles, so dragging never thrashes the players.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            currentOnEvent(ShortsUiEvent.OnPageSettled(page))
        }
    }

    VerticalPager(
        state = pagerState,
        modifier = modifier,
        key = { index -> state.videos[index].id },
    ) { page ->
        val video = state.videos[page]
        val isActive = page == state.activeIndex
        ShortPage(
            video = video,
            // Only the active and preloaded pages have a player; others show their thumbnail.
            player = if (isActive || page == state.preloadIndex) playerFor(page) else null,
            isActive = isActive,
            isPaused = isActive && state.isPaused,
            isLiked = video.id in state.likedIds,
            onEvent = onEvent,
        )
    }
}

@Composable
private fun ShortPage(
    video: Video,
    player: Player?,
    isActive: Boolean,
    isPaused: Boolean,
    isLiked: Boolean,
    onEvent: (ShortsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onEvent(ShortsUiEvent.OnTogglePlay) },
            ),
    ) {
        AsyncImage(
            model = video.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        if (player != null) {
            ShortVideoSurface(player = player, modifier = Modifier.fillMaxSize())
        }

        // Scrim so white overlay text stays readable on bright frames.
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.4f)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)))),
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, bottom = 16.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            VideoMetadata(video = video, modifier = Modifier.weight(1f))
            ActionColumn(
                isLiked = isLiked,
                onLike = { onEvent(ShortsUiEvent.OnLikeClick(video.id)) },
                onShare = { onEvent(ShortsUiEvent.OnShareClick(video)) },
            )
        }

        if (isActive && player != null) {
            PlaybackIndicators(player = player, modifier = Modifier.fillMaxSize())
        }

        if (isPaused) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = stringResource(R.string.shorts_play),
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(72.dp),
            )
        }
    }
}

/** Controller-less [PlayerView] cropped to fill the page, as in other vertical video feeds. */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun ShortVideoSurface(
    player: Player,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        factory = { context ->
            PlayerView(context).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                // Let the thumbnail show through until the first frame renders.
                setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                keepScreenOn = true
            }
        },
        update = { view -> view.player = player },
        onRelease = { view -> view.player = null },
        modifier = modifier,
    )
}

@Composable
private fun VideoMetadata(
    video: Video,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(end = 8.dp)) {
        Text(
            text = video.title,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (video.category.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "#${video.category}",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.9f),
            )
        }
        if (video.description.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = video.description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ActionColumn(
    isLiked: Boolean,
    onLike: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconButton(onClick = onLike) {
            Icon(
                imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = stringResource(if (isLiked) R.string.shorts_unlike else R.string.shorts_like),
                tint = if (isLiked) Color.Red else Color.White,
                modifier = Modifier.size(32.dp),
            )
        }
        IconButton(onClick = onShare) {
            Icon(
                imageVector = Icons.Filled.Share,
                contentDescription = stringResource(R.string.shorts_share),
                tint = Color.White,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

/**
 * Buffering spinner and a thin playback progress bar for the active page. Progress is polled
 * (Player has no position callback) and read inside the indicator's lambda, so each tick only
 * redraws the bar instead of recomposing the page.
 */
@Composable
private fun PlaybackIndicators(
    player: Player,
    modifier: Modifier = Modifier,
) {
    var progress by remember(player) { mutableFloatStateOf(0f) }
    var isBuffering by remember(player) { mutableStateOf(player.playbackState == Player.STATE_BUFFERING) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    LaunchedEffect(player) {
        while (isActive) {
            val duration = player.duration
            progress = if (duration > 0) (player.currentPosition.toFloat() / duration).coerceIn(0f, 1f) else 0f
            delay(PROGRESS_POLL_MS)
        }
    }

    Box(modifier) {
        if (isBuffering) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.align(Alignment.Center))
        }
        LinearProgressIndicator(
            progress = { progress },
            color = Color.White,
            trackColor = Color.White.copy(alpha = 0.25f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(2.dp),
        )
    }
}

@Composable
private fun MessageContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .statusBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = message, color = Color.White, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text(stringResource(R.string.shorts_retry))
        }
    }
}
