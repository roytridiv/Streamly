package com.tridivroy.streamly.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
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
import com.tridivroy.streamly.core.theme.StreamlyIcons
import com.tridivroy.streamly.core.theme.StreamlyTheme
import com.tridivroy.streamly.core.theme.TextMuted
import com.tridivroy.streamly.domain.model.Channel
import com.tridivroy.streamly.domain.model.Video
import com.tridivroy.streamly.domain.model.VideoStats
import com.tridivroy.streamly.presentation.common.safeTopPadding
import com.tridivroy.streamly.presentation.home.components.CategoryChips
import com.tridivroy.streamly.presentation.home.components.HomeTopBar
import com.tridivroy.streamly.presentation.home.components.TopBarAction
import com.tridivroy.streamly.presentation.home.components.VideoCard

/** Stateful entry point: wires [HomeViewModel] to the stateless [HomeScreen]. */
@Composable
fun HomeRoute(
    onNavigateToPlayer: (videoId: String) -> Unit,
    onNavigateToProfile: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnNavigateToPlayer by rememberUpdatedState(onNavigateToPlayer)
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    is HomeUiEffect.NavigateToPlayer -> currentOnNavigateToPlayer(effect.videoId)
                }
            }
        }
    }

    val currentOnNavigateToProfile by rememberUpdatedState(onNavigateToProfile)

    HomeScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onProfileClick = { currentOnNavigateToProfile() },
    )
}

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onEvent: (HomeUiEvent) -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .safeTopPadding()
            .padding(top = 10.dp),
    ) {
        HomeTopBar(
            actions = listOf(
                TopBarAction(
                    icon = StreamlyIcons.Refresh,
                    contentDescription = stringResource(R.string.home_refresh),
                    onClick = { onEvent(HomeUiEvent.Refresh) },
                ),
                TopBarAction(
                    icon = StreamlyIcons.User,
                    contentDescription = stringResource(R.string.profile_open),
                    onClick = onProfileClick,
                ),
            ),
        )

        when (uiState) {
            HomeUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }

            is HomeUiState.Error -> ErrorContent(
                message = uiState.message ?: stringResource(R.string.home_error_generic),
                onRetry = { onEvent(HomeUiEvent.Refresh) },
                modifier = Modifier.fillMaxSize(),
            )

            is HomeUiState.Success -> SuccessContent(state = uiState, onEvent = onEvent)
        }
    }
}

@Composable
private fun SuccessContent(
    state: HomeUiState.Success,
    onEvent: (HomeUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        if (state.categories.isNotEmpty()) {
            CategoryChips(
                categories = state.categories,
                selected = state.selectedCategory,
                onSelect = { onEvent(HomeUiEvent.OnCategorySelect(it)) },
            )
        }

        if (state.isRefreshing) {
            LinearProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        val videos = state.visibleVideos
        if (videos.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.home_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                )
            }
        } else {
            LazyColumn(
                // Bottom padding clears the mini-player, which docks over this list.
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                items(videos, key = { it.id }) { video ->
                    VideoCard(
                        video = video,
                        isDownloaded = video.id in state.downloadedIds,
                        onClick = { onEvent(HomeUiEvent.OnVideoClick(video.id)) },
                    )
                }
            }
        }
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
            Text(stringResource(R.string.home_retry))
        }
    }
}

private val previewVideos = listOf(
    Video(
        id = "1",
        title = "Fjord light at 4 a.m. — a slow drive north of Tromsø",
        description = "",
        videoUrl = "",
        thumbnailUrl = "",
        category = "Travel",
        duration = 754,
        isShort = false,
        channel = Channel("Nordlys Films", "nordlys", "", 412_000),
        stats = VideoStats(1_200_000, 48_200, 3_104, System.currentTimeMillis() / 1000 - 3 * 86_400),
    ),
    Video(
        id = "2",
        title = "Building a cabin desk from one oak board",
        description = "",
        videoUrl = "",
        thumbnailUrl = "",
        category = "Design",
        duration = 1268,
        isShort = false,
        channel = Channel("Slow Workshop", "slowworkshop", "", 198_000),
        stats = VideoStats(640_000, 21_400, 0, System.currentTimeMillis() / 1000 - 7 * 86_400),
    ),
)

@Preview(showBackground = true, backgroundColor = 0xFF121820)
@Composable
private fun HomeScreenSuccessPreview() {
    StreamlyTheme(darkTheme = true) {
        HomeScreen(
            uiState = HomeUiState.Success(
                videos = previewVideos,
                categories = previewVideos.map { it.category }.distinct(),
                downloadedIds = setOf("1"),
            ),
            onEvent = {},
            onProfileClick = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121820)
@Composable
private fun HomeScreenErrorPreview() {
    StreamlyTheme(darkTheme = true) {
        HomeScreen(uiState = HomeUiState.Error(message = null), onEvent = {}, onProfileClick = {})
    }
}
