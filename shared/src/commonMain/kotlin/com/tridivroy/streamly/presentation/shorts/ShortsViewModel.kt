package com.tridivroy.streamly.presentation.shorts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tridivroy.streamly.presentation.common.VideoSurfaceHandle
import com.tridivroy.streamly.domain.repository.PreferencesRepository
import com.tridivroy.streamly.domain.repository.VideoRepository
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tridivroy.streamly.core.di.SharedModule
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

/**
 * Owns the Shorts [ShortsPlayerController] (separate from the app-wide player) and decides which page
 * plays. The pool lives as long as this ViewModel: it survives rotation and is released when the
 * Shorts entry leaves the back stack.
 */
class ShortsViewModel(
    private val videoRepository: VideoRepository,
    private val playerPool: ShortsPlayerController,
    private val nowPlayingStore: NowPlayingController,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ShortsUiState>(ShortsUiState.Loading)
    val uiState: StateFlow<ShortsUiState> = _uiState.asStateFlow()

    private val _effects = Channel<ShortsUiEffect>(Channel.BUFFERED)
    val effects: Flow<ShortsUiEffect> = _effects.receiveAsFlow()

    private var loadJob: Job? = null

    /** Persisted engagement (DataStore); kept so a (re)load shows likes and saves immediately. */
    private var engagement = Engagement()

    init {
        observeEngagement()
        loadShorts()
    }

    /** The player for [index], or `null` when that page isn't loaded (UI shows its thumbnail). */
    fun playerFor(index: Int): VideoSurfaceHandle? = playerPool.playerFor(index)

    fun onEvent(event: ShortsUiEvent) {
        when (event) {
            is ShortsUiEvent.OnPageSettled -> activatePage(event.index)

            ShortsUiEvent.OnTogglePlay -> updateSuccess { state ->
                if (state.isPaused) playerPool.play(state.activeIndex) else playerPool.pauseAll()
                state.copy(isPaused = !state.isPaused)
            }

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
                if (!state.isPaused) playerPool.play(state.activeIndex)
            }

            ShortsUiEvent.OnScreenStop -> playerPool.pauseAll()

            ShortsUiEvent.Retry -> loadShorts()
        }
    }

    private fun loadShorts() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = ShortsUiState.Loading
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
        // Swiping always resumes playback, like other short-video feeds.
        state.copy(
            activeIndex = index,
            preloadIndex = playerPool.activate(index, state.videos),
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

/**
 * Builds [ShortsViewModel] from the shared service locator, in place of `hiltViewModel()`.
 *
 * Three of its four dependencies are platform-provided — the player pool, the app-wide player and
 * preferences all sit on Android frameworks — so the locator hands over whatever the host
 * registered, falling back to the no-op implementations on a platform that has none.
 */
val ShortsViewModelFactory = viewModelFactory {
    initializer {
        ShortsViewModel(
            videoRepository = SharedModule.videoRepository,
            playerPool = SharedModule.shortsPlayerController,
            nowPlayingStore = SharedModule.nowPlayingController,
            preferencesRepository = SharedModule.preferencesRepository,
        )
    }
}
