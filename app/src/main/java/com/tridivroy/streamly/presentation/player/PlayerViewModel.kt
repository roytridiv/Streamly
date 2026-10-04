package com.tridivroy.streamly.presentation.player

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.tridivroy.streamly.core.media.DownloadTracker
import com.tridivroy.streamly.core.media.NowPlayingStore
import com.tridivroy.streamly.core.media.isNetworkError
import com.tridivroy.streamly.core.media.toMediaItem
import com.tridivroy.streamly.domain.model.DownloadStatus
import com.tridivroy.streamly.domain.model.PlaybackQuality
import com.tridivroy.streamly.domain.model.Video
import com.tridivroy.streamly.domain.model.VideoDownload
import com.tridivroy.streamly.domain.repository.ConnectivityObserver
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
 *
 * Without network, a video that isn't downloaded never reaches the player: it goes straight to
 * [PlayerUiState.Offline] instead of a spinner that waits out Media3's load retries. If the network
 * drops mid-stream, playback pauses under a no-internet overlay ([PlayerUiState.Success.isOffline]).
 * Both recover by themselves when the connection returns. Downloads skip all of this.
 */
@HiltViewModel(assistedFactory = PlayerViewModel.Factory::class)
class PlayerViewModel @AssistedInject constructor(
    @Assisted private val videoId: String,
    private val videoRepository: VideoRepository,
    private val downloadRepository: DownloadRepository,
    private val downloadTracker: DownloadTracker,
    private val nowPlayingStore: NowPlayingStore,
    private val preferencesRepository: PreferencesRepository,
    private val connectivityObserver: ConnectivityObserver,
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

    /** Playing from local storage: network loss can't affect it, so offline handling is skipped. */
    private var isPlayingFromDownload = false

    private val isOnline: Boolean get() = connectivityObserver.isOnline.value

    private val isOurItem: Boolean get() = exoPlayer.currentMediaItem?.mediaId == videoId

    private val playerListener = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            if (!isOurItem) return
            if (!isPlayingFromDownload && (error.isNetworkError() || !isOnline)) {
                markOffline()
            } else {
                // Media3's message ("MediaCodecVideoRenderer error, index=0, format=…") means nothing
                // to a viewer: log it, and let the screen show its generic "Couldn't play" text.
                Log.w(TAG, "Playback failed: ${error.errorCodeName}", error)
                _uiState.value = PlayerUiState.Error(message = null)
            }
        }

        // Ran out of buffer with no network: Media3 would keep retrying behind a spinner for a while
        // before erroring, so say what is wrong straight away.
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_BUFFERING && isOurItem && !isPlayingFromDownload && !isOnline) {
                markOffline()
            }
        }

        // The player's volume is the source of truth for isMuted; setMuted only ever changes the volume.
        override fun onVolumeChanged(volume: Float) {
            _uiState.update { state ->
                if (state is PlayerUiState.Success) state.copy(isMuted = volume == 0f) else state
            }
        }
    }

    /** Volume to go back to on unmute: the last non-zero level the player was at. */
    private var unmutedVolume = 1f

    init {
        exoPlayer.addListener(playerListener)
        observeDownload()
        observeEngagement()
        observeConnectivity()
        loadVideo()
    }

    fun onEvent(event: PlayerUiEvent) {
        when (event) {
            PlayerUiEvent.OnLikeClick -> viewModelScope.launch {
                preferencesRepository.toggleFavorite(videoId)
            }

            PlayerUiEvent.OnSubscribeClick -> viewModelScope.launch {
                val handle = (_uiState.value as? PlayerUiState.Success)?.video?.channel?.handle
                if (!handle.isNullOrBlank()) preferencesRepository.toggleSubscription(handle)
            }

            PlayerUiEvent.OnShareClick -> viewModelScope.launch {
                val video = (_uiState.value as? PlayerUiState.Success)?.video ?: return@launch
                _effects.send(PlayerUiEffect.CopyLink(video.videoUrl))
            }

            PlayerUiEvent.OnToggleMute -> toggleMute()
            PlayerUiEvent.OnSeekForward -> seekForward()
            PlayerUiEvent.OnSeekBackward -> seekBackward()
            PlayerUiEvent.Retry -> retry()
            PlayerUiEvent.OnDownloadClick -> toggleDownload()
            is PlayerUiEvent.OnRelatedVideoClick -> viewModelScope.launch {
                _effects.send(PlayerUiEffect.NavigateToVideo(event.videoId))
            }
        }
    }

    /**
     * Mutes through the volume rather than by disabling the audio track, so unmuting is instant and
     * never re-selects tracks. The state follows via [Player.Listener.onVolumeChanged].
     */
    fun setMuted(muted: Boolean) {
        if (muted) {
            exoPlayer.volume.takeIf { it > 0f }?.let { unmutedVolume = it }
            exoPlayer.volume = 0f
        } else {
            exoPlayer.volume = unmutedVolume
        }
    }

    fun toggleMute() = setMuted(exoPlayer.volume > 0f)

    /** Jumps ahead, stopping at the end. Before the duration is known, the jump is left unclamped. */
    fun seekForward(millis: Long = SEEK_STEP_MS) {
        val target = exoPlayer.currentPosition + millis
        val duration = exoPlayer.duration
        exoPlayer.seekTo(if (duration == C.TIME_UNSET) target else target.coerceAtMost(duration))
    }

    fun seekBackward(millis: Long = SEEK_STEP_MS) {
        exoPlayer.seekTo((exoPlayer.currentPosition - millis).coerceAtLeast(0L))
    }

    private fun loadVideo() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = PlayerUiState.Loading
            // Checked before anything touches the network: a download plays regardless of
            // connectivity, anything else can't play without it.
            val downloadedItem = downloadTracker.downloadedMediaItem(videoId)
            if (downloadedItem == null && !isOnline) {
                _uiState.value = PlayerUiState.Offline
                return@launch
            }
            _uiState.value = videoRepository.getVideoById(videoId)
                .recoverCatching { error -> downloadRepository.getDownloadedVideo(videoId) ?: throw error }
                .fold(
                    onSuccess = { video ->
                        if (video.videoUrl.isBlank()) {
                            PlayerUiState.Empty
                        } else {
                            startPlayback(video, downloadedItem)
                            val prefs = preferencesRepository.userPreferences.first()
                            PlayerUiState.Success(
                                video = video,
                                download = download,
                                isLiked = video.id in prefs.favoriteVideoIds,
                                isSubscribed = video.channel.handle.takeIf { it.isNotBlank() } in prefs.subscribedChannels,
                                // The player is shared, so a mute set on the previous video still holds.
                                isMuted = exoPlayer.volume == 0f,
                                // The connection can drop between the pre-check and here.
                                isOffline = !isPlayingFromDownload && !isOnline,
                            )
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

    private suspend fun startPlayback(video: Video, downloadedItem: MediaItem?) {
        applyQualityCap(preferencesRepository.userPreferences.first().playbackQuality)
        isPlayingFromDownload = downloadedItem != null
        val mediaItem = downloadedItem ?: video.toMediaItem()
        nowPlayingStore.onPlaybackStarted(video)
        // Counts once per video, for the Profile screen's "watched" stat.
        preferencesRepository.markWatched(video.id)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    /** Retry does nothing while still offline; the button's own "Checking…" is the feedback. */
    private fun retry() {
        when (val state = _uiState.value) {
            PlayerUiState.Offline -> if (isOnline) loadVideo()
            is PlayerUiState.Success -> if (state.isOffline && isOnline) resumeAfterReconnect()
            else -> loadVideo()
        }
    }

    private fun markOffline() {
        exoPlayer.pause()
        _uiState.update { state ->
            when (state) {
                is PlayerUiState.Success -> state.copy(isOffline = true)
                // Mid-load: loadVideo() derives isOffline when it builds Success.
                else -> state
            }
        }
    }

    /** Re-prepares only if the stall became an error; otherwise the player picks up where it stopped. */
    private fun resumeAfterReconnect() {
        _uiState.update { state -> if (state is PlayerUiState.Success) state.copy(isOffline = false) else state }
        if (exoPlayer.playerError != null) exoPlayer.prepare()
        exoPlayer.play()
    }

    /** Recovers both offline states by itself once the network is back, so Retry is optional. */
    private fun observeConnectivity() {
        viewModelScope.launch {
            connectivityObserver.isOnline.collect { online ->
                val state = _uiState.value
                when {
                    online && state == PlayerUiState.Offline -> loadVideo()
                    online && state is PlayerUiState.Success && state.isOffline -> resumeAfterReconnect()
                    // Dropped while still buffering: nothing will arrive, so don't leave a spinner up.
                    !online && state is PlayerUiState.Success && isOurItem && !isPlayingFromDownload &&
                        exoPlayer.playbackState == Player.STATE_BUFFERING -> markOffline()
                }
            }
        }
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

    /**
     * Keeps Like and Subscribe in step with DataStore. Both are optimistic in the UI only in the
     * sense that DataStore round-trips in a frame or two; the state here is always the stored truth.
     */
    private fun observeEngagement() {
        viewModelScope.launch {
            preferencesRepository.userPreferences
                .map { prefs -> prefs.favoriteVideoIds to prefs.subscribedChannels }
                .distinctUntilChanged()
                .collect { (favorites, subscriptions) ->
                    _uiState.update { state ->
                        if (state is PlayerUiState.Success) {
                            state.copy(
                                isLiked = videoId in favorites,
                                isSubscribed = state.video.channel.handle.takeIf { it.isNotBlank() } in subscriptions,
                            )
                        } else {
                            state
                        }
                    }
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

            // Paused included: in the Player the button cancels the download outright. Pausing and
            // resuming an individual download belongs on the Downloads screen, which has the room.
            DownloadStatus.Queued,
            DownloadStatus.Downloading,
            DownloadStatus.Paused,
            DownloadStatus.Downloaded,
            -> downloadRepository.remove(videoId)
        }
    }

    /**
     * Leaves playback running.
     *
     * The Player screen is no longer the only place a video can play from: switching tabs from the
     * bottom bar pops this entry, and the mini-player on Home and Downloads picks the same video up
     * mid-stream. Stopping here would make that handover impossible — and did, until the bar was shown
     * on the Player: the mini-player could never appear at all, because reaching the tab that shows it
     * always destroyed the playback it was meant to continue.
     *
     * The player is still never released (it is shared, see MediaModule), the mini-player's button can
     * pause it, [NowPlayingStore] drops it when the player moves on, ShortsViewModel pauses it when the
     * Shorts pool takes over, and the process-lifecycle observer pauses it when the app backgrounds.
     */
    override fun onCleared() {
        exoPlayer.removeListener(playerListener)
    }

    private companion object {
        const val TAG = "PlayerViewModel"
        const val MAX_RELATED_VIDEOS = 10
        const val SEEK_STEP_MS = 10_000L
    }
}
