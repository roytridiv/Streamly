package com.tridivroy.streamly.presentation.shorts

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
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
import com.tridivroy.streamly.core.theme.StreamlyBrand
import com.tridivroy.streamly.core.theme.StreamlyIcons
import com.tridivroy.streamly.domain.model.Video
import com.tridivroy.streamly.presentation.common.rememberPlaybackProgress
import com.tridivroy.streamly.presentation.shorts.components.ShortsAction
import com.tridivroy.streamly.presentation.shorts.components.ShortsActionRail
import com.tridivroy.streamly.presentation.shorts.components.ShortsHeader
import com.tridivroy.streamly.presentation.shorts.components.ShortsOverlay
import com.tridivroy.streamly.presentation.shorts.components.ShortsProgressBar

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
        .background(StreamlyBrand.Letterbox)

    when (uiState) {
        ShortsUiState.Loading -> Box(screenModifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = StreamlyBrand.OnMedia)
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

    Box(modifier) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
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
                isSaved = video.id in state.savedIds,
                isFollowing = video.channel.handle.takeIf { it.isNotBlank() } in state.followedChannels,
                onEvent = onEvent,
            )
        }

        // Header and position sit above the pager so they do not travel with the pages.
        ShortsHeader(
            pageNumber = state.activeIndex + 1,
            pageCount = state.videos.size,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(start = 18.dp, end = 18.dp, top = 10.dp),
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
    isSaved: Boolean,
    isFollowing: Boolean,
    onEvent: (ShortsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress by rememberPlaybackProgress(if (isActive) player else null)

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

        // Scrim so overlay text stays readable on bright frames.
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.45f)
                .background(Brush.verticalGradient(listOf(Color.Transparent, StreamlyBrand.Scrim))),
        )

        ShortsActionRail(
            actions = listOf(
                ShortsAction(
                    icon = if (isLiked) StreamlyIcons.HeartFilled else StreamlyIcons.Heart,
                    contentDescription = stringResource(if (isLiked) R.string.shorts_unlike else R.string.shorts_like),
                    count = video.stats.likeCount + if (isLiked) 1 else 0,
                    active = isLiked,
                    onClick = { onEvent(ShortsUiEvent.OnLikeClick(video.id)) },
                ),
                // Streamly has no comments screen, so this shows the count without claiming to open
                // anything — a tile, not a button. Give it an onClick once there is somewhere to go.
                ShortsAction(
                    icon = StreamlyIcons.Comment,
                    contentDescription = stringResource(R.string.shorts_comments),
                    count = video.stats.commentCount,
                ),
                ShortsAction(
                    icon = StreamlyIcons.Share,
                    contentDescription = stringResource(R.string.shorts_share),
                    count = 0,
                    onClick = { onEvent(ShortsUiEvent.OnShareClick(video)) },
                ),
                ShortsAction(
                    icon = if (isSaved) StreamlyIcons.SaveFilled else StreamlyIcons.Save,
                    contentDescription = stringResource(if (isSaved) R.string.shorts_unsave else R.string.shorts_save),
                    count = 0,
                    active = isSaved,
                    onClick = { onEvent(ShortsUiEvent.OnSaveClick(video.id)) },
                ),
            ),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 12.dp, bottom = RAIL_BOTTOM_PADDING),
        )

        ShortsOverlay(
            video = video,
            isFollowing = isFollowing,
            onFollowClick = { onEvent(ShortsUiEvent.OnFollowClick(video.channel.handle)) },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 80.dp, bottom = OVERLAY_BOTTOM_PADDING),
        )

        ShortsProgressBar(
            fraction = progress.fraction,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = PROGRESS_BOTTOM_PADDING),
        )

        if (isActive && progress.isBuffering) {
            CircularProgressIndicator(
                color = StreamlyBrand.OnMedia,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        if (isPaused) {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(StreamlyBrand.FrostedSurface),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = StreamlyIcons.Play,
                    contentDescription = stringResource(R.string.shorts_play),
                    tint = StreamlyBrand.OnMedia,
                    modifier = Modifier.size(32.dp),
                )
            }
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
                // In-stream captions would collide with the metadata overlay.
                subtitleView?.visibility = android.view.View.GONE
            }
        },
        update = { view -> view.player = player },
        onRelease = { view -> view.player = null },
        modifier = modifier,
    )
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
        Text(
            text = message,
            color = StreamlyBrand.OnMedia,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text(stringResource(R.string.shorts_retry))
        }
    }
}

/** Keeps the rail, overlay and progress bar clear of the bottom nav, as the prototype does. */
private val RAIL_BOTTOM_PADDING = 44.dp
private val OVERLAY_BOTTOM_PADDING = 34.dp
private val PROGRESS_BOTTOM_PADDING = 18.dp
