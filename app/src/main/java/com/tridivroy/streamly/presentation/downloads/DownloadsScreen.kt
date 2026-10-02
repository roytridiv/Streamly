package com.tridivroy.streamly.presentation.downloads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.tridivroy.streamly.R
import com.tridivroy.streamly.core.storage.StorageUsage
import com.tridivroy.streamly.core.theme.StreamlyLogoTile
import com.tridivroy.streamly.core.theme.StreamlyTheme
import com.tridivroy.streamly.core.theme.TextMuted
import com.tridivroy.streamly.domain.model.Channel
import com.tridivroy.streamly.domain.model.DownloadStatus
import com.tridivroy.streamly.domain.model.Video
import com.tridivroy.streamly.domain.model.VideoDownload
import com.tridivroy.streamly.presentation.downloads.components.DownloadRow
import com.tridivroy.streamly.presentation.downloads.components.FadedDivider
import com.tridivroy.streamly.presentation.downloads.components.StorageUsageCard

/** Stateful entry point: wires [DownloadsViewModel] to the stateless [DownloadsScreen]. */
@Composable
fun DownloadsRoute(
    onNavigateToPlayer: (videoId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DownloadsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnNavigateToPlayer by rememberUpdatedState(onNavigateToPlayer)
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    is DownloadsUiEffect.NavigateToPlayer -> currentOnNavigateToPlayer(effect.videoId)
                }
            }
        }
    }

    DownloadsScreen(uiState = uiState, onEvent = viewModel::onEvent, modifier = modifier)
}

@Composable
fun DownloadsScreen(
    uiState: DownloadsUiState,
    onEvent: (DownloadsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = 10.dp),
    ) {
        DownloadsHeader(
            summary = (uiState as? DownloadsUiState.Success)?.let { state ->
                stringResource(R.string.downloads_summary, state.readyCount, state.savingCount)
            },
        )

        when (uiState) {
            DownloadsUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }

            DownloadsUiState.Empty -> EmptyContent(Modifier.fillMaxSize())

            is DownloadsUiState.Error -> ErrorContent(
                message = uiState.message ?: stringResource(R.string.downloads_error_generic),
                onRetry = { onEvent(DownloadsUiEvent.Retry) },
                modifier = Modifier.fillMaxSize(),
            )

            is DownloadsUiState.Success -> SuccessContent(state = uiState, onEvent = onEvent)
        }
    }
}

@Composable
private fun DownloadsHeader(
    summary: String?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .padding(bottom = 16.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(R.string.downloads_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (summary != null) {
            Text(text = summary, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
    }
}

@Composable
private fun SuccessContent(
    state: DownloadsUiState.Success,
    onEvent: (DownloadsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        // Bottom padding clears the mini-player, which docks over this list.
        contentPadding = PaddingValues(bottom = 90.dp),
    ) {
        if (state.storage.isKnown) {
            item(key = "storage") {
                StorageUsageCard(
                    storage = state.storage,
                    modifier = Modifier.padding(horizontal = 14.dp),
                )
            }
            item(key = "divider") {
                FadedDivider(
                    Modifier.padding(start = 14.dp, end = 14.dp, top = 20.dp, bottom = 10.dp),
                )
            }
        }

        items(state.downloads, key = { it.video.id }) { download ->
            DownloadRow(
                download = download,
                onClick = {
                    // A failed row retries; a finished one plays. Anything else is still working.
                    if (download.status == DownloadStatus.Failed) {
                        onEvent(DownloadsUiEvent.OnRetryDownloadClick(download.video))
                    } else {
                        onEvent(DownloadsUiEvent.OnDownloadClick(download))
                    }
                },
                onRemove = { onEvent(DownloadsUiEvent.OnRemoveClick(download.video.id)) },
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
    }
}

/** The outlined logo and "Nothing saved yet". */
@Composable
private fun EmptyContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 20.dp, vertical = 44.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StreamlyLogoTile(outlined = true, modifier = Modifier.size(56.dp))
        Text(
            text = stringResource(R.string.downloads_empty_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.downloads_empty_hint),
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
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
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text(stringResource(R.string.downloads_retry))
        }
    }
}

private val previewDownloads = listOf(
    VideoDownload(
        video = Video(
            id = "1",
            title = "Fjord light at 4 a.m. — a slow drive north of Tromsø",
            description = "",
            videoUrl = "",
            thumbnailUrl = "",
            category = "Travel",
            duration = 754,
            isShort = false,
            channel = Channel("Nordlys Films", "nordlys", "", 412_000),
        ),
        status = DownloadStatus.Downloaded,
        progressPercent = 100f,
        bytesDownloaded = 612_000_000,
        videoHeight = 1080,
    ),
    VideoDownload(
        video = Video(
            id = "2",
            title = "Building a cabin desk from one oak board",
            description = "",
            videoUrl = "",
            thumbnailUrl = "",
            category = "Design",
            duration = 1268,
            isShort = false,
            channel = Channel("Slow Workshop", "slowworkshop", "", 198_000),
        ),
        status = DownloadStatus.Downloading,
        progressPercent = 42f,
        bytesDownloaded = 411_000_000,
        videoHeight = 720,
    ),
)

@Preview(showBackground = true, backgroundColor = 0xFF121820, heightDp = 700)
@Composable
private fun DownloadsScreenPreview() {
    StreamlyTheme(darkTheme = true) {
        DownloadsScreen(
            uiState = DownloadsUiState.Success(
                downloads = previewDownloads,
                storage = StorageUsage(
                    totalBytes = 64_000_000_000,
                    freeBytes = 23_200_000_000,
                    streamlyBytes = 2_200_000_000,
                ),
            ),
            onEvent = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121820, heightDp = 500)
@Composable
private fun DownloadsEmptyPreview() {
    StreamlyTheme(darkTheme = true) {
        DownloadsScreen(uiState = DownloadsUiState.Empty, onEvent = {})
    }
}
