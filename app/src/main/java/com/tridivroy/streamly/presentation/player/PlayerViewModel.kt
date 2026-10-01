package com.tridivroy.streamly.presentation.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.tridivroy.streamly.core.media.toMediaItem
import com.tridivroy.streamly.domain.model.Video
import com.tridivroy.streamly.domain.repository.VideoRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Drives the app-wide [ExoPlayer] for one video. The player is shared (see MediaModule), so this
 * ViewModel only swaps media items and stops playback when its entry leaves the back stack — it
 * never releases the player. Surviving rotation means playback continues without re-preparing.
 */
@HiltViewModel(assistedFactory = PlayerViewModel.Factory::class)
class PlayerViewModel @AssistedInject constructor(
    @Assisted private val videoId: String,
    private val videoRepository: VideoRepository,
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

    private var loadJob: Job? = null

    private val playerListener = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            if (exoPlayer.currentMediaItem?.mediaId == videoId) {
                _uiState.value = PlayerUiState.Error(error.localizedMessage)
            }
        }
    }

    init {
        exoPlayer.addListener(playerListener)
        loadVideo()
    }

    fun onEvent(event: PlayerUiEvent) {
        when (event) {
            PlayerUiEvent.Retry -> loadVideo()
        }
    }

    private fun loadVideo() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = PlayerUiState.Loading
            _uiState.value = videoRepository.getVideoById(videoId).fold(
                onSuccess = { video ->
                    if (video.videoUrl.isBlank()) {
                        PlayerUiState.Empty
                    } else {
                        startPlayback(video)
                        PlayerUiState.Success(video)
                    }
                },
                onFailure = { PlayerUiState.Error(it.message) },
            )
        }
    }

    private fun startPlayback(video: Video) {
        exoPlayer.setMediaItem(video.toMediaItem())
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    override fun onCleared() {
        exoPlayer.removeListener(playerListener)
        // Only stop our own item, in case another screen already took over the shared player.
        if (exoPlayer.currentMediaItem?.mediaId == videoId) {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
        }
    }
}
