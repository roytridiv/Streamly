package com.tridivroy.streamly.presentation.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.tridivroy.streamly.core.media.DownloadTracker
import com.tridivroy.streamly.core.media.toMediaItem
import com.tridivroy.streamly.domain.model.DownloadStatus
import com.tridivroy.streamly.domain.model.PlaybackQuality
import com.tridivroy.streamly.domain.model.Video
import com.tridivroy.streamly.domain.model.VideoDownload
import com.tridivroy.streamly.domain.repository.DownloadRepository
import com.tridivroy.streamly.domain.repository.PreferencesRepository
import com.tridivroy.streamly.domain.repository.VideoRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Drives the app-wide [ExoPlayer] for one video. The player is shared (see MediaModule), so this
 * ViewModel only swaps media items and stops playback when its entry leaves the back stack — it
 * never releases the player. Surviving rotation means playback continues without re-preparing.
 *
 * Downloaded videos play offline: metadata falls back to the download when the API is unreachable,
 * and the downloaded item (with its HLS stream keys) is played from the download cache.
 */
@HiltViewModel(assistedFactory = PlayerViewModel.Factory::class)
class PlayerViewModel @AssistedInject constructor(
    @Assisted private val videoId: String,
    private val videoRepository: VideoRepository,
    private val downloadRepository: DownloadRepository,
    private val downloadTracker: DownloadTracker,
    private val preferencesRepository: PreferencesRepository,
    private val exoPlayer: ExoPlayer,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(videoId: String): PlayerViewModel
    }

    /** Exposed as [Player] so the UI can attach a surface but not rebuild or release the player. */
    val player: Player get() = exoPlayer

    private val _uiState = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading)
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val _effects = Channel<PlayerUiEffect>(Channel.BUFFERED)
    val effects: Flow<PlayerUiEffect> = _effects.receiveAsFlow()

    private var loadJob: Job? = null

    /** Latest download state of this video, kept so a (re)load can include it immediately. */
    private var download: VideoDownload? = null

    private val playerListener = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            if (exoPlayer.currentMediaItem?.mediaId == videoId) {
                _uiState.value = PlayerUiState.Error(error.localizedMessage)
            }
        }
    }

    init {
        exoPlayer.addListener(playerListener)
        observeDownload()
        loadVideo()
    }

    fun onEvent(event: PlayerUiEvent) {
        when (event) {
            PlayerUiEvent.Retry -> loadVideo()
            PlayerUiEvent.OnDownloadClick -> toggleDownload()
            is PlayerUiEvent.OnRelatedVideoClick -> viewModelScope.launch {
                _effects.send(PlayerUiEffect.NavigateToVideo(event.videoId))
            }
        }
    }

    private fun loadVideo() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = PlayerUiState.Loading
            _uiState.value = videoRepository.getVideoById(videoId)
                .recoverCatching { error -> downloadRepository.getDownloadedVideo(videoId) ?: throw error }
                .fold(
                    onSuccess = { video ->
                        if (video.videoUrl.isBlank()) {
                            PlayerUiState.Empty
                        } else {
                            startPlayback(video)
                            PlayerUiState.Success(video, download)
                        }
                    },
                    onFailure = { PlayerUiState.Error(it.message) },
                )
            (_uiState.value as? PlayerUiState.Success)?.let { success -> loadRelatedVideos(success.video) }
        }
    }

    /**
     * Fills the "Up Next" tab: other videos from the same category first, then anything else, so the
     * tab is not empty just because a category holds a single video. Suggestions are a nicety, so a
     * failure here leaves the list empty rather than failing the screen.
     */
    private fun loadRelatedVideos(video: Video) {
        viewModelScope.launch {
            val others = videoRepository.getHomeVideos().getOrNull()
                ?.filter { it.id != video.id }
                ?: return@launch
            val related = others
                .sortedByDescending { it.category.equals(video.category, ignoreCase = true) }
                .take(MAX_RELATED_VIDEOS)
            _uiState.update { state ->
                if (state is PlayerUiState.Success) state.copy(relatedVideos = related) else state
            }
        }
    }

    private suspend fun startPlayback(video: Video) {
        applyQualityCap(preferencesRepository.userPreferences.first().playbackQuality)
        val mediaItem = downloadTracker.downloadedMediaItem(video.id) ?: video.toMediaItem()
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    private fun applyQualityCap(quality: PlaybackQuality) {
        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters.buildUpon()
            .apply {
                val maxHeight = quality.maxVideoHeight
                if (maxHeight != null) setMaxVideoSize(Int.MAX_VALUE, maxHeight) else clearVideoSizeConstraints()
            }
            .build()
    }

    private fun observeDownload() {
        viewModelScope.launch {
            downloadRepository.downloads
                .map { downloads -> downloads.find { it.video.id == videoId } }
                .distinctUntilChanged()
                .collect { latest ->
                    download = latest
                    _uiState.update { state -> if (state is PlayerUiState.Success) state.copy(download = latest) else state }
                }
        }
    }

    /** Not downloaded / failed → start; queued / downloading → cancel; downloaded → delete. */
    private fun toggleDownload() {
        val state = _uiState.value as? PlayerUiState.Success ?: return
        when (state.download?.status) {
            null, DownloadStatus.Failed -> viewModelScope.launch {
                downloadRepository.download(state.video).onFailure { error ->
                    _effects.send(PlayerUiEffect.DownloadFailed(error.message))
                }
            }

            DownloadStatus.Queued, DownloadStatus.Downloading, DownloadStatus.Downloaded ->
                downloadRepository.remove(videoId)
        }
    }

    override fun onCleared() {
        exoPlayer.removeListener(playerListener)
        // Only stop our own item, in case another screen already took over the shared player.
        if (exoPlayer.currentMediaItem?.mediaId == videoId) {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
        }
    }

    private companion object {
        const val MAX_RELATED_VIDEOS = 10
    }
}
