package com.tridivroy.streamly.presentation.player

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
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
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.tridivroy.streamly.R
import com.tridivroy.streamly.core.theme.StreamlyIcons
import com.tridivroy.streamly.domain.model.DownloadStatus
import com.tridivroy.streamly.domain.model.Video
import com.tridivroy.streamly.domain.model.VideoDownload
import kotlin.math.roundToInt

/** Stateful entry point: wires [PlayerViewModel] to the stateless [PlayerScreen]. */
@Composable
fun PlayerRoute(
    videoId: String,
    windowSizeClass: WindowSizeClass,
    onBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel<PlayerViewModel, PlayerViewModel.Factory>(
        key = videoId,
        creationCallback = { factory -> factory.create(videoId) },
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    is PlayerUiEffect.DownloadFailed -> snackbarHostState.showSnackbar(
                        context.getString(R.string.player_download_error, effect.message.orEmpty()),
                    )
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

    PlayerScreen(
        uiState = uiState,
        player = viewModel.player,
        windowSizeClass = windowSizeClass,
        snackbarHostState = snackbarHostState,
        onEvent = onEvent,
        onBack = onBack,
    )
}

@Composable
fun PlayerScreen(
    uiState: PlayerUiState,
    player: Player,
    windowSizeClass: WindowSizeClass,
    snackbarHostState: SnackbarHostState,
    onEvent: (PlayerUiEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Phone in landscape: give the whole screen to the video.
    val isFullscreen = windowSizeClass.heightSizeClass == WindowHeightSizeClass.Compact &&
        uiState is PlayerUiState.Success

    if (isFullscreen) {
        ImmersiveMode()
        VideoSurface(
            player = player,
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black),
        )
    } else {
        PlayerScaffold(
            uiState = uiState,
            player = player,
            isExpandedWidth = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded,
            snackbarHostState = snackbarHostState,
            onEvent = onEvent,
            onBack = onBack,
            modifier = modifier,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerScaffold(
    uiState: PlayerUiState,
    player: Player,
    isExpandedWidth: Boolean,
    snackbarHostState: SnackbarHostState,
    onEvent: (PlayerUiEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.player_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)

        when (uiState) {
            PlayerUiState.Loading -> Box(contentModifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            PlayerUiState.Empty -> MessageContent(
                message = stringResource(R.string.player_empty),
                onRetry = null,
                modifier = contentModifier,
            )

            is PlayerUiState.Error -> MessageContent(
                message = uiState.message ?: stringResource(R.string.player_error_generic),
                onRetry = { onEvent(PlayerUiEvent.Retry) },
                modifier = contentModifier,
            )

            is PlayerUiState.Success -> if (isExpandedWidth) {
                // Tablets / unfolded foldables: video and details side by side.
                Row(contentModifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    VideoSurface(
                        player = player,
                        modifier = Modifier
                            .weight(0.65f)
                            .aspectRatio(16f / 9f)
                            .background(Color.Black),
                    )
                    VideoDetails(
                        video = uiState.video,
                        download = uiState.download,
                        onDownloadClick = { onEvent(PlayerUiEvent.OnDownloadClick) },
                        modifier = Modifier.weight(0.35f),
                    )
                }
            } else {
                Column(contentModifier) {
                    VideoSurface(
                        player = player,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .background(Color.Black),
                    )
                    VideoDetails(
                        video = uiState.video,
                        download = uiState.download,
                        onDownloadClick = { onEvent(PlayerUiEvent.OnDownloadClick) },
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }
}

/**
 * Hosts Media3's [PlayerView] (no Compose player UI in media3 1.4). The view is detached from the
 * shared player on dispose so the next surface — e.g. after rotation — can take it over cleanly.
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun VideoSurface(
    player: Player,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        factory = { context ->
            PlayerView(context).apply {
                setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                setShowNextButton(false)
                setShowPreviousButton(false)
                keepScreenOn = true
            }
        },
        update = { view -> view.player = player },
        onRelease = { view -> view.player = null },
        modifier = modifier,
    )
}

@Composable
private fun VideoDetails(
    video: Video,
    download: VideoDownload?,
    onDownloadClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.verticalScroll(rememberScrollState())) {
        Text(text = video.title, style = MaterialTheme.typography.titleLarge)
        if (video.category.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = video.category,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(12.dp))
        DownloadButton(download = download, onClick = onDownloadClick)
        if (video.description.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(text = video.description, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** One button that reflects and drives the download lifecycle (see PlayerUiEvent.OnDownloadClick). */
@Composable
private fun DownloadButton(
    download: VideoDownload?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val icon = when (download?.status) {
        null -> StreamlyIcons.Download
        DownloadStatus.Queued, DownloadStatus.Downloading -> Icons.Filled.Close
        DownloadStatus.Downloaded -> Icons.Filled.CheckCircle
        DownloadStatus.Failed -> Icons.Filled.Refresh
    }
    val label = when (download?.status) {
        null -> stringResource(R.string.player_download)
        DownloadStatus.Queued -> stringResource(R.string.player_download_queued)
        DownloadStatus.Downloading -> stringResource(R.string.player_download_cancel, download.progressPercent?.roundToInt() ?: 0)
        DownloadStatus.Downloaded -> stringResource(R.string.player_downloaded)
        DownloadStatus.Failed -> stringResource(R.string.player_download_failed)
    }
    OutlinedButton(onClick = onClick, modifier = modifier) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(8.dp))
        Text(label)
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
        Text(text = message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        if (onRetry != null) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text(stringResource(R.string.player_retry))
            }
        }
    }
}

/** Hides system bars while in composition; restores them when leaving fullscreen. */
@Composable
private fun ImmersiveMode() {
    val activity = LocalContext.current as? Activity ?: return
    DisposableEffect(activity) {
        val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
        onDispose { controller.show(WindowInsetsCompat.Type.systemBars()) }
    }
}
