package com.tridivroy.streamly.presentation.downloads

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import coil.compose.AsyncImage
import com.tridivroy.streamly.R
import com.tridivroy.streamly.core.theme.StreamlyTheme
import com.tridivroy.streamly.domain.model.DownloadStatus
import com.tridivroy.streamly.domain.model.Video
import com.tridivroy.streamly.domain.model.VideoDownload
import kotlin.math.roundToInt

/** Stateful entry point: wires [DownloadsViewModel] to the stateless [DownloadsScreen]. */
@Composable
fun DownloadsRoute(
    onNavigateToPlayer: (videoId: String) -> Unit,
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

    DownloadsScreen(uiState = uiState, onEvent = viewModel::onEvent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    uiState: DownloadsUiState,
    onEvent: (DownloadsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.downloads_title)) }) },
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)

        when (uiState) {
            DownloadsUiState.Loading -> Box(contentModifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            DownloadsUiState.Empty -> Box(contentModifier.padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.downloads_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
            }

            is DownloadsUiState.Error -> Column(
                modifier = contentModifier.padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = uiState.message ?: stringResource(R.string.downloads_error_generic),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = { onEvent(DownloadsUiEvent.Retry) }) {
                    Text(stringResource(R.string.downloads_retry))
                }
            }

            // Adaptive columns: one on phones, more on tablets / unfolded foldables.
            is DownloadsUiState.Success -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 340.dp),
                modifier = contentModifier,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(uiState.downloads, key = { it.video.id }) { download ->
                    DownloadItem(download = download, onEvent = onEvent)
                }
            }
        }
    }
}

@Composable
private fun DownloadItem(
    download: VideoDownload,
    onEvent: (DownloadsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val video = download.video
    Card(
        onClick = { onEvent(DownloadsUiEvent.OnDownloadClick(download)) },
        enabled = download.status == DownloadStatus.Downloaded,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            // Thumbnail may not load offline; the placeholder keeps the layout stable.
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = null,
                placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                error = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(128.dp)
                    .aspectRatio(16f / 9f),
            )
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                DownloadStatusLine(download)
            }
            if (download.status == DownloadStatus.Failed) {
                IconButton(onClick = { onEvent(DownloadsUiEvent.OnRetryDownloadClick(video)) }) {
                    Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.downloads_retry_download))
                }
            }
            IconButton(onClick = { onEvent(DownloadsUiEvent.OnRemoveClick(video.id)) }) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.downloads_remove))
            }
        }
    }
}

@Composable
private fun DownloadStatusLine(download: VideoDownload) {
    val style = MaterialTheme.typography.bodySmall
    when (download.status) {
        DownloadStatus.Queued -> Text(stringResource(R.string.download_status_queued), style = style)

        DownloadStatus.Downloading -> {
            val percent = download.progressPercent
            Text(
                text = if (percent != null) {
                    stringResource(R.string.download_status_downloading, percent.roundToInt())
                } else {
                    stringResource(R.string.download_status_downloading_indeterminate)
                },
                style = style,
            )
            Spacer(Modifier.height(6.dp))
            if (percent != null) {
                LinearProgressIndicator(progress = { percent / 100f }, modifier = Modifier.fillMaxWidth())
            } else {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        }

        DownloadStatus.Downloaded -> Text(
            text = stringResource(
                R.string.download_status_downloaded,
                Formatter.formatShortFileSize(LocalContext.current, download.bytesDownloaded),
            ),
            style = style,
            color = MaterialTheme.colorScheme.primary,
        )

        DownloadStatus.Failed -> Text(
            text = stringResource(R.string.download_status_failed),
            style = style,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DownloadsScreenPreview() {
    fun video(id: String, title: String) = Video(id, title, "", "", "", "", 600, false)
    StreamlyTheme {
        DownloadsScreen(
            uiState = DownloadsUiState.Success(
                listOf(
                    VideoDownload(video("1", "Big Buck Bunny"), DownloadStatus.Downloaded, 100f, 48_000_000),
                    VideoDownload(video("2", "Tears of Steel"), DownloadStatus.Downloading, 42f, 12_000_000),
                    VideoDownload(video("3", "Sintel"), DownloadStatus.Failed, null, 0),
                ),
            ),
            onEvent = {},
        )
    }
}
