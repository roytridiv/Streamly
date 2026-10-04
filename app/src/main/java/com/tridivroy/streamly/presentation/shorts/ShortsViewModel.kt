package com.tridivroy.streamly.presentation.shorts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.tridivroy.streamly.core.media.NowPlayingStore
import com.tridivroy.streamly.core.media.ShortsPlayerPool
import com.tridivroy.streamly.core.media.isNetworkError
import com.tridivroy.streamly.domain.repository.ConnectivityObserver
import com.tridivroy.streamly.domain.repository.PreferencesRepository
import com.tridivroy.streamly.domain.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
import javax.inject.Inject

/**
 * Owns the Shorts [ShortsPlayerPool] (separate from the app-wide player) and decides which page
 * plays. The pool lives as long as this ViewModel: it survives rotation and is released when the
 * Shorts entry leaves the back stack.
 *
 * Shorts always stream (they can't be downloaded), so connectivity matters throughout: opening the
 * feed offline shows [ShortsUiState.Offline] instead of a pager of endless spinners, and losing the
 * network mid-feed pauses it under an overlay ([ShortsUiState.Success.isOffline]). Both recover by
 * themselves when the connection returns.
 */
@HiltViewModel
class ShortsViewModel @Inject constructor(
    private val videoRepository: VideoRepository,
    private val playerPool: ShortsPlayerPool,
    private val nowPlayingStore: NowPlayingStore,
    private val preferencesRepository: PreferencesRepository,
    private val connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ShortsUiState>(ShortsUiState.Loading)
    val uiState: StateFlow<ShortsUiState> = _uiState.asStateFlow()

    private val _effects = Channel<ShortsUiEffect>(Channel.BUFFERED)
    val effects: Flow<ShortsUiEffect> = _effects.receiveAsFlow()

    private var loadJob: Job? = null

    /** Persisted engagement (DataStore); kept so a (re)load shows likes and saves immediately. */
    private var engagement = Engagement()

    /** Kept outside the UI state for the same reason: a Retry rebuilds the state, not the pool. */
    private var isMuted = false

    private val isOnline: Boolean get() = connectivityObserver.isOnline.value

    /** Only the playing page counts: a preloaded page failing in the background is recovered on retry. */
    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            val state = _uiState.value as? ShortsUiState.Success ?: return
            if (state.isOffline || player !== playerPool.playerFor(state.activeIndex)) return

            val error: PlaybackException? = player.playerError
            val failedOnNetwork = events.contains(Player.EVENT_PLAYER_ERROR) &&
                error != null && (error.isNetworkError() || !isOnline)
            // Out of buffer with no network: say so now rather than after Media3's load retries.
            val stalledOffline = events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED) &&
                player.playbackState == Player.STATE_BUFFERING && !isOnline
            if (failedOnNetwork || stalledOffline) markOffline()
        }
    }

    init {
        playerPool.addListener(playerListener)
        observeEngagement()
        observeConnectivity()
        loadShorts()
    }

    /** The player for [index], or `null` when that page isn't loaded (UI shows its thumbnail). */
    fun playerFor(index: Int): Player? = playerPool.playerFor(index)

    fun onEvent(event: ShortsUiEvent) {
        when (event) {
            is ShortsUiEvent.OnPageSettled -> activatePage(event.index)

            ShortsUiEvent.OnTogglePlay -> updateSuccess { state ->
                if (state.isPaused) playerPool.play(state.activeIndex) else playerPool.pauseAll()
                state.copy(isPaused = !state.isPaused)
            }

            ShortsUiEvent.OnToggleMute -> toggleMute()

            // The state updates via observeEngagement() once DataStore has persisted the change.
            is ShortsUiEvent.OnLikeClick -> viewModelScope.launch {
                preferencesRepository.toggleFavorite(event.videoId)
            }

            is ShortsUiEvent.OnSaveClick -> viewModelScope.launch {
                preferencesRepository.toggleSaved(event.videoId)
            }

            is ShortsUiEvent.OnFollowClick -> viewModelScope.launch {
                if (event.channelHandle.isNotBlank()) preferencesRepository.toggleSubscription(event.channelHandle)
            }

            is ShortsUiEvent.OnShareClick -> viewModelScope.launch {
                _effects.send(ShortsUiEffect.Share(event.video))
            }

            ShortsUiEvent.OnScreenStart -> (_uiState.value as? ShortsUiState.Success)?.let { state ->
                // A video left playing on the shared player (the mini-player keeps it going after the
                // Player screen is popped) would bleed under the short about to start.
                nowPlayingStore.pause()
                if (!state.isPaused && !state.isOffline) playerPool.play(state.activeIndex)
            }

            ShortsUiEvent.OnScreenStop -> playerPool.pauseAll()

            ShortsUiEvent.Retry -> retry()
        }
    }

    /** Applied to the whole pool, so it holds across swipes and survives a reload of the feed. */
    fun setMuted(muted: Boolean) {
        isMuted = muted
        playerPool.setMuted(muted)
        updateSuccess { it.copy(isMuted = muted) }
    }

    fun toggleMute() = setMuted(!isMuted)

    /** Retry does nothing while still offline; the button's own "Checking…" is the feedback. */
    private fun retry() {
        when (val state = _uiState.value) {
            ShortsUiState.Offline -> if (isOnline) loadShorts()
            is ShortsUiState.Success -> if (state.isOffline && isOnline) resumeAfterReconnect()
            else -> loadShorts()
        }
    }

    private fun markOffline() {
        playerPool.pauseAll()
        updateSuccess { it.copy(isOffline = true) }
    }

    private fun resumeAfterReconnect() {
        val state = _uiState.value as? ShortsUiState.Success ?: return
        _uiState.value = state.copy(isOffline = false)
        if (state.isPaused) return
        playerPool.recover(state.activeIndex)
    }

    /** Recovers both offline states by itself once the network is back, so Retry is optional. */
    private fun observeConnectivity() {
        viewModelScope.launch {
            connectivityObserver.isOnline.collect { online ->
                val state = _uiState.value
                when {
                    online && state == ShortsUiState.Offline -> loadShorts()
                    online && state is ShortsUiState.Success && state.isOffline -> resumeAfterReconnect()
                    // Dropped while the active short is still buffering: nothing more will arrive.
                    !online && state is ShortsUiState.Success &&
                        playerPool.playerFor(state.activeIndex)?.playbackState == Player.STATE_BUFFERING -> markOffline()
                }
            }
        }
    }

    private fun loadShorts() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = ShortsUiState.Loading
            if (!isOnline) {
                _uiState.value = ShortsUiState.Offline
                return@launch
            }
            _uiState.value = videoRepository.getShortsVideos().fold(
                onSuccess = { videos ->
                    val playable = videos.filter { it.videoUrl.isNotBlank() }
                    if (playable.isEmpty()) {
                        ShortsUiState.Empty
                    } else {
                        ShortsUiState.Success(
                            videos = playable,
                            activeIndex = 0,
                            preloadIndex = playerPool.activate(0, playable),
                            likedIds = engagement.liked,
                            savedIds = engagement.saved,
                            followedChannels = engagement.followed,
                            isMuted = isMuted,
                        )
                    }
                },
                onFailure = { ShortsUiState.Error(it.message) },
            )
        }
    }

    private fun observeEngagement() {
        viewModelScope.launch {
            preferencesRepository.userPreferences
                .map { Engagement(it.favoriteVideoIds, it.savedVideoIds, it.subscribedChannels) }
                .distinctUntilChanged()
                .collect { latest ->
                    engagement = latest
                    updateSuccess {
                        it.copy(
                            likedIds = latest.liked,
                            savedIds = latest.saved,
                            followedChannels = latest.followed,
                        )
                    }
                }
        }
    }

    /** The three persisted sets the rail reads, grouped so one flow carries them all. */
    private data class Engagement(
        val liked: Set<String> = emptySet(),
        val saved: Set<String> = emptySet(),
        val followed: Set<String> = emptySet(),
    )

    private fun activatePage(index: Int) = updateSuccess { state ->
        if (index == state.activeIndex || index !in state.videos.indices) return@updateSuccess state
        // Swiping always resumes playback, like other short-video feeds — unless the feed is stalled
        // offline, where the new page stays paused under the overlay too.
        val preloadIndex = playerPool.activate(index, state.videos)
        if (state.isOffline) playerPool.pauseAll()
        state.copy(
            activeIndex = index,
            preloadIndex = preloadIndex,
            isPaused = false,
        )
    }

    private inline fun updateSuccess(transform: (ShortsUiState.Success) -> ShortsUiState.Success) {
        val state = _uiState.value as? ShortsUiState.Success ?: return
        _uiState.value = transform(state)
    }

    override fun onCleared() {
        playerPool.release()
    }
}
