package com.tridivroy.streamly.presentation.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tridivroy.streamly.domain.model.DownloadStatus
import com.tridivroy.streamly.domain.repository.DownloadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val downloadRepository: DownloadRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<DownloadsUiState>(DownloadsUiState.Loading)
    val uiState: StateFlow<DownloadsUiState> = _uiState.asStateFlow()

    private val _effects = Channel<DownloadsUiEffect>(Channel.BUFFERED)
    val effects: Flow<DownloadsUiEffect> = _effects.receiveAsFlow()

    private var observeJob: Job? = null

    init {
        observeDownloads()
    }

    fun onEvent(event: DownloadsUiEvent) {
        when (event) {
            // Only finished downloads are playable offline.
            is DownloadsUiEvent.OnDownloadClick -> if (event.download.status == DownloadStatus.Downloaded) {
                viewModelScope.launch { _effects.send(DownloadsUiEffect.NavigateToPlayer(event.download.video.id)) }
            }

            is DownloadsUiEvent.OnRemoveClick -> downloadRepository.remove(event.videoId)

            // On failure the item simply stays in the Failed state.
            is DownloadsUiEvent.OnRetryDownloadClick -> viewModelScope.launch { downloadRepository.download(event.video) }

            DownloadsUiEvent.Retry -> observeDownloads()
        }
    }

    private fun observeDownloads() {
        observeJob?.cancel()
        _uiState.value = DownloadsUiState.Loading
        observeJob = viewModelScope.launch {
            downloadRepository.downloads
                .catch { _uiState.value = DownloadsUiState.Error(it.message) }
                .collect { downloads ->
                    _uiState.value = if (downloads.isEmpty()) DownloadsUiState.Empty else DownloadsUiState.Success(downloads)
                }
        }
    }
}
