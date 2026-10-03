package com.tridivroy.streamly.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tridivroy.streamly.domain.model.DownloadStatus
import com.tridivroy.streamly.domain.repository.DownloadRepository
import com.tridivroy.streamly.domain.repository.VideoRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val videoRepository: VideoRepository,
    private val downloadRepository: DownloadRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _effects = Channel<HomeUiEffect>(Channel.BUFFERED)
    val effects: Flow<HomeUiEffect> = _effects.receiveAsFlow()

    private var loadJob: Job? = null

    init {
        loadVideos()
        observeDownloads()
    }

    /** Keeps the "Offline" tag on each card in step with the download list. */
    private fun observeDownloads() {
        viewModelScope.launch {
            downloadRepository.downloads
                .map { downloads -> downloads.filter { it.status == DownloadStatus.Downloaded }.map { it.video.id }.toSet() }
                .distinctUntilChanged()
                .collect { ids ->
                    _uiState.update { state -> if (state is HomeUiState.Success) state.copy(downloadedIds = ids) else state }
                }
        }
    }

    fun onEvent(event: HomeUiEvent) {
        when (event) {
            is HomeUiEvent.OnVideoClick -> viewModelScope.launch {
                _effects.send(HomeUiEffect.NavigateToPlayer(event.videoId))
            }

            is HomeUiEvent.OnCategorySelect -> _uiState.update { state ->
                if (state is HomeUiState.Success) state.copy(selectedCategory = event.category) else state
            }

            HomeUiEvent.Refresh -> loadVideos()
        }
    }

    private fun loadVideos() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            // Keep current content visible while refreshing; full-screen loader only on first load or after an error.
            val previous = _uiState.value as? HomeUiState.Success
            _uiState.value = previous?.copy(isRefreshing = true) ?: HomeUiState.Loading

            _uiState.value = videoRepository.getHomeVideos().fold(
                onSuccess = { videos ->
                    val categories = videos.map { it.category }.filter { it.isNotBlank() }.distinct()
                    HomeUiState.Success(
                        videos = videos,
                        categories = categories,
                        selectedCategory = previous?.selectedCategory?.takeIf { it in categories },
                        downloadedIds = previous?.downloadedIds.orEmpty(),
                    )
                },
                onFailure = { HomeUiState.Error(it.message) },
            )
        }
    }
}
