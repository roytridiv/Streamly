package com.tridivroy.streamly.presentation.player

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.Player
import com.tridivroy.streamly.R
import com.tridivroy.streamly.core.theme.StreamlyBrand
import com.tridivroy.streamly.core.theme.StreamlyIcons
import com.tridivroy.streamly.core.theme.StreamlyShape
import com.tridivroy.streamly.core.theme.StreamlyTheme
import com.tridivroy.streamly.core.theme.StreamlyType
import com.tridivroy.streamly.core.theme.TextMuted
import com.tridivroy.streamly.domain.model.Channel
import com.tridivroy.streamly.domain.model.Chapter
import com.tridivroy.streamly.domain.model.Video
import com.tridivroy.streamly.domain.model.VideoStats
import com.tridivroy.streamly.presentation.common.rememberPlaybackProgress
import com.tridivroy.streamly.presentation.common.safeBottomPadding
import com.tridivroy.streamly.presentation.common.safeTopPadding
import com.tridivroy.streamly.presentation.components.StreamlyToastHost
import com.tridivroy.streamly.presentation.components.rememberToastState
import com.tridivroy.streamly.presentation.player.components.FloatingPlayerViewport
import com.tridivroy.streamly.presentation.player.components.KeyMomentsTab
import com.tridivroy.streamly.presentation.player.components.OverviewTab
import com.tridivroy.streamly.presentation.player.components.PlayerActionBar
import com.tridivroy.streamly.presentation.player.components.PlayerTabRow
import com.tridivroy.streamly.presentation.player.components.rememberPlayerControlsState
import com.tridivroy.streamly.presentation.player.components.UpNextTab

/** Stateful entry point: wires [PlayerViewModel] to the stateless [PlayerScreen]. */
@Composable
fun PlayerRoute(
    videoId: String,
    windowSizeClass: WindowSizeClass,
    onBack: () -> Unit,
    onNavigateToVideo: (videoId: String) -> Unit,
    /** Lets the nav graph drop the bottom bar for fullscreen playback. */
    onFullscreenChange: (Boolean) -> Unit = {},
    viewModel: PlayerViewModel = hiltViewModel<PlayerViewModel, PlayerViewModel.Factory>(
        key = videoId,
        creationCallback = { factory -> factory.create(videoId) },
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val toastState = rememberToastState()
    val currentOnNavigateToVideo by rememberUpdatedState(onNavigateToVideo)

    val linkCopied = stringResource(R.string.player_link_copied)
    val downloadFailedPrefix = stringResource(R.string.player_download_failed)

    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    is PlayerUiEffect.DownloadFailed ->
                        toastState.show(effect.message?.takeIf { it.isNotBlank() } ?: downloadFailedPrefix)

                    is PlayerUiEffect.NavigateToVideo -> currentOnNavigateToVideo(effect.videoId)

                    is PlayerUiEffect.CopyLink -> {
                        context.copyToClipboard(effect.url)
                        toastState.show(linkCopied)
                    }
                }
            }
        }
    }

    // Android 13+: the download progress notification needs permission. Downloads run either way.
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val onEvent: (PlayerUiEvent) -> Unit = remember(viewModel) {
        { event ->
            if (event == PlayerUiEvent.OnDownloadClick &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            viewModel.onEvent(event)
        }
    }

    Box(Modifier.fillMaxSize()) {
        PlayerScreen(
            uiState = uiState,
            player = viewModel.player,
            windowSizeClass = windowSizeClass,
            onEvent = onEvent,
            onBack = onBack,
            onFullscreenChange = onFullscreenChange,
        )
        StreamlyToastHost(state = toastState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
fun PlayerScreen(
    uiState: PlayerUiState,
    player: Player,
    windowSizeClass: WindowSizeClass,
    onEvent: (PlayerUiEvent) -> Unit,
    onBack: () -> Unit,
    onFullscreenChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val isCompactHeight = windowSizeClass.heightSizeClass == WindowHeightSizeClass.Compact
    val isCompactWidth = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact

    var isFullscreen by rememberSaveable { mutableStateOf(false) }

    /*
     * Whether *we* asked for landscape, saved rather than re-derived.
     *
     * This used to be read from the window size class at the moment of the toggle, which silently
     * broke the exit path: a phone in landscape is ~800x360dp, so its widthSizeClass is Medium, not
     * Compact. The lock was therefore applied on the way in (from compact-width portrait) and the
     * unlock skipped on the way out, leaving the whole app stuck in landscape. Remembering the fact
     * means the unlock never depends on the size class we happen to be reading now.
     */
    var lockedLandscape by rememberSaveable { mutableStateOf(false) }

    // Rotating a phone into landscape still enters fullscreen on its own, and rotating back out of it
    // leaves — the button and the device's orientation drive the same single piece of state, so the
    // two can never disagree about which mode the player is in.
    LaunchedEffect(isCompactHeight) { isFullscreen = isCompactHeight }

    val toggleFullscreen: () -> Unit = {
        val entering = !isFullscreen
        isFullscreen = entering
        // A 16:9 video "filling" a portrait phone is mostly black bars, so entering from a compact
        // width also asks for landscape. Tablets and unfolded foldables have the room already and
        // keep whatever orientation the user chose.
        lockedLandscape = entering && isCompactWidth
    }

    // Orientation and the system bars are set and cleared together, in one place, for every exit
    // path there is: the button, a rotation, system back, or navigating off the Player entirely.
    FullscreenWindowEffect(isFullscreen = isFullscreen, lockLandscape = lockedLandscape)

    // Reported up so the nav graph can hide the bottom bar, and reset on the way out so leaving the
    // Player mid-fullscreen never leaves the bar hidden on the tab underneath.
    val currentOnFullscreenChange by rememberUpdatedState(onFullscreenChange)
    DisposableEffect(isFullscreen) {
        currentOnFullscreenChange(isFullscreen)
        onDispose { currentOnFullscreenChange(false) }
    }

    val success = uiState as? PlayerUiState.Success
    if (success != null && isFullscreen) {
        // Back leaves fullscreen before it leaves the Player — otherwise the only way out of an
        // immersive screen would be the button the overlay has just auto-hidden.
        BackHandler(onBack = toggleFullscreen)
        FullscreenPlayer(
            state = success,
            player = player,
            onToggleFullscreen = toggleFullscreen,
            onEvent = onEvent,
            modifier = modifier,
        )
    } else {
        PlayerContent(
            uiState = uiState,
            player = player,
            isExpandedWidth = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded,
            onEvent = onEvent,
            onBack = onBack,
            onToggleFullscreen = toggleFullscreen,
            modifier = modifier,
        )
    }
}

/**
 * Fullscreen is the same [FloatingPlayerViewport] filling the window, not a separate surface, so the
 * overlay — scrubber, key-moment ticks, play/pause and the exit button — behaves identically in both
 * modes and auto-hides the same way.
 */
@Composable
private fun FullscreenPlayer(
    state: PlayerUiState.Success,
    player: Player,
    onToggleFullscreen: () -> Unit,
    onEvent: (PlayerUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress by rememberPlaybackProgress(player)
    val controls = rememberPlayerControlsState()

    FloatingPlayerViewport(
        player = player,
        progress = progress,
        chapters = state.video.chapters,
        onTogglePlay = { if (player.isPlaying) player.pause() else player.play() },
        onSeekTo = { positionMs ->
            player.seekTo(positionMs)
            if (!player.isPlaying) player.play()
            controls.keepVisible()
        },
        onToggleFullscreen = onToggleFullscreen,
        isMuted = state.isMuted,
        onToggleMute = { onEvent(PlayerUiEvent.OnToggleMute) },
        onSeekBackward = { onEvent(PlayerUiEvent.OnSeekBackward) },
        onSeekForward = { onEvent(PlayerUiEvent.OnSeekForward) },
        isFullscreen = true,
        controls = controls,
        modifier = modifier
            .fillMaxSize()
            .background(StreamlyBrand.Letterbox),
    )
}

@Composable
private fun PlayerContent(
    uiState: PlayerUiState,
    player: Player,
    isExpandedWidth: Boolean,
    onEvent: (PlayerUiEvent) -> Unit,
    onBack: () -> Unit,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeTopPadding(),
    ) {
        PlayerHeader(onBack = onBack)
        Spacer(Modifier.height(4.dp))

        when (uiState) {
            PlayerUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }

            PlayerUiState.Empty -> MessageContent(
                message = stringResource(R.string.player_empty),
                onRetry = null,
                modifier = Modifier.fillMaxSize(),
            )

            is PlayerUiState.Error -> MessageContent(
                message = uiState.message ?: stringResource(R.string.player_error_generic),
                onRetry = { onEvent(PlayerUiEvent.Retry) },
                modifier = Modifier.fillMaxSize(),
            )

            is PlayerUiState.Success -> SuccessContent(
                state = uiState,
                player = player,
                isExpandedWidth = isExpandedWidth,
                onEvent = onEvent,
                onToggleFullscreen = onToggleFullscreen,
            )
        }
    }
}

@Composable
private fun SuccessContent(
    state: PlayerUiState.Success,
    player: Player,
    isExpandedWidth: Boolean,
    onEvent: (PlayerUiEvent) -> Unit,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress by rememberPlaybackProgress(player)

    // Local UI state, per the handoff: these are view preferences, not app state.
    var selectedTab by rememberSaveable { mutableStateOf(PlayerTab.Overview) }
    var descriptionExpanded by rememberSaveable { mutableStateOf(false) }
    var upNextLayout by rememberSaveable { mutableStateOf(UpNextLayout.List) }

    // Hoisted so a seek from the Key Moments tab counts as an interaction too: the overlay comes
    // back and its 3-second countdown restarts, exactly as if the scrubber had been tapped.
    val controls = rememberPlayerControlsState()

    val togglePlay: () -> Unit = { if (player.isPlaying) player.pause() else player.play() }
    val seekToMs: (Long) -> Unit = { positionMs ->
        player.seekTo(positionMs)
        if (!player.isPlaying) player.play()
        controls.keepVisible()
    }

    val viewport: @Composable (Modifier) -> Unit = { viewportModifier ->
        FloatingPlayerViewport(
            player = player,
            progress = progress,
            chapters = state.video.chapters,
            onTogglePlay = togglePlay,
            onSeekTo = seekToMs,
            onToggleFullscreen = onToggleFullscreen,
            isMuted = state.isMuted,
            onToggleMute = { onEvent(PlayerUiEvent.OnToggleMute) },
            onSeekBackward = { onEvent(PlayerUiEvent.OnSeekBackward) },
            onSeekForward = { onEvent(PlayerUiEvent.OnSeekForward) },
            controls = controls,
            modifier = viewportModifier,
        )
    }

    val details: @Composable (Modifier) -> Unit = { detailsModifier ->
        Column(
            modifier = detailsModifier,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            PlayerActionBar(
                isLiked = state.isLiked,
                likeCount = state.likeCount,
                download = state.download,
                onLikeClick = { onEvent(PlayerUiEvent.OnLikeClick) },
                onShareClick = { onEvent(PlayerUiEvent.OnShareClick) },
                onDownloadClick = { onEvent(PlayerUiEvent.OnDownloadClick) },
                modifier = Modifier.padding(horizontal = 14.dp),
            )

            PlayerTabRow(
                selected = selectedTab,
                momentCount = state.video.chapters.size,
                onSelect = { selectedTab = it },
                modifier = Modifier.padding(horizontal = 14.dp),
            )

            when (selectedTab) {
                PlayerTab.Overview -> OverviewTab(
                    video = state.video,
                    isSubscribed = state.isSubscribed,
                    descriptionExpanded = descriptionExpanded,
                    onToggleDescription = { descriptionExpanded = !descriptionExpanded },
                    onSubscribeClick = { onEvent(PlayerUiEvent.OnSubscribeClick) },
                )

                PlayerTab.KeyMoments -> KeyMomentsTab(
                    chapters = state.video.chapters,
                    currentPositionSeconds = progress.positionSeconds,
                    onSeekTo = { seconds -> seekToMs(seconds * 1000) },
                )

                PlayerTab.UpNext -> UpNextTab(
                    videos = state.relatedVideos,
                    layout = upNextLayout,
                    onToggleLayout = {
                        upNextLayout =
                            if (upNextLayout == UpNextLayout.List) UpNextLayout.Carousel else UpNextLayout.List
                    },
                    onVideoClick = { videoId -> onEvent(PlayerUiEvent.OnRelatedVideoClick(videoId)) },
                )
            }

            Spacer(Modifier.height(28.dp))
        }
    }

    if (isExpandedWidth) {
        // Tablets / unfolded foldables: the viewport stays put while the details scroll beside it.
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            viewport(Modifier.weight(0.6f))
            details(
                Modifier
                    .weight(0.4f)
                    .verticalScroll(rememberScrollState())
                    .safeBottomPadding(),
            )
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .safeBottomPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Breathing room between the header and the viewport, which otherwise sat tight under it.
            viewport(Modifier.padding(horizontal = 14.dp, vertical = 4.dp))
            details(Modifier)
        }
    }
}

/** Collapse chevron, "NOW PLAYING" eyebrow, and a balancing slot where the overflow menu will go. */
@Composable
private fun PlayerHeader(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        HeaderIconButton(
            // A left arrow, not the handoff's collapse chevron: on device the chevron read as
            // "expand/collapse" rather than "go back".
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.player_back),
            onClick = onBack,
        )
        Text(
            text = stringResource(R.string.player_now_playing),
            style = StreamlyType.Eyebrow,
            color = TextMuted,
        )
        // The handoff's overflow menu has no actions in Streamly yet; the space is held so the
        // eyebrow stays optically centred.
        Spacer(Modifier.size(42.dp))
    }
}

@Composable
private fun HeaderIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(StreamlyShape.IconButton)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun MessageContent(
    message: String,
    onRetry: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (onRetry != null) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text(stringResource(R.string.player_retry))
            }
        }
    }
}

/**
 * Owns the two window-level side effects of fullscreen — the system bars and the requested
 * orientation — so they can never drift apart or be left behind.
 *
 * Both are driven by [isFullscreen] and both are undone in `onDispose`, which covers leaving the
 * Player by any route: the toggle, a rotation, system back, an Up Next swap, or the whole screen
 * being popped off the back stack. Nothing outside this function touches `requestedOrientation`.
 */
@Composable
private fun FullscreenWindowEffect(
    isFullscreen: Boolean,
    lockLandscape: Boolean,
) {
    val activity = LocalContext.current as? Activity ?: return

    DisposableEffect(activity, isFullscreen, lockLandscape) {
        val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)

        if (isFullscreen) {
            // Transient bars so a swipe still brings them back without leaving fullscreen.
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
            if (lockLandscape) {
                // USER_LANDSCAPE, not LANDSCAPE: the viewer can still flip between both landscape
                // directions while fullscreen.
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
            }
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }

        onDispose {
            controller.show(WindowInsetsCompat.Type.systemBars())
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
}

private fun Context.copyToClipboard(text: String) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(null, text))
}

private val previewVideo = Video(
    id = "1",
    title = "Fjord light at 4 a.m. — a slow drive north of Tromsø",
    description = "We left Tromsø at 3:40 a.m. to catch the blue hour over Ullsfjorden. No narration, " +
        "no music — just the road, the engine and the light coming up over the water.",
    videoUrl = "",
    thumbnailUrl = "",
    category = "Travel",
    duration = 754,
    isShort = false,
    channel = Channel("Nordlys Films", "nordlys", "", 412_000),
    stats = VideoStats(1_200_000, 48_200, 3_104, System.currentTimeMillis() / 1000 - 3 * 86_400),
    chapters = listOf(
        Chapter(0, "Departure"),
        Chapter(96, "Blue hour"),
        Chapter(221, "Ferry crossing"),
        Chapter(388, "Reindeer"),
    ),
    tags = listOf("nordic", "slowtv", "4k"),
)

@Preview(showBackground = true, backgroundColor = 0xFF121820, heightDp = 760)
@Composable
private fun PlayerDetailsPreview() {
    StreamlyTheme(darkTheme = true) {
        Column(
            Modifier.background(MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            OverviewTab(
                video = previewVideo,
                isSubscribed = false,
                descriptionExpanded = false,
                onToggleDescription = {},
                onSubscribeClick = {},
            )
            KeyMomentsTab(
                chapters = previewVideo.chapters,
                currentPositionSeconds = 120,
                onSeekTo = {},
            )
        }
    }
}
