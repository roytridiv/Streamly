package com.tridivroy.streamly.presentation.shorts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.tridivroy.streamly.core.media.ShortsPlayerPool
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
 */
@HiltViewModel
class ShortsViewModel @Inject constructor(
    private val videoRepository: VideoRepository,
    private val playerPool: ShortsPlayerPool,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ShortsUiState>(ShortsUiState.Loading)
    val uiState: StateFlow<ShortsUiState> = _uiState.asStateFlow()

    private val _effects = Channel<ShortsUiEffect>(Channel.BUFFERED)
    val effects: Flow<ShortsUiEffect> = _effects.receiveAsFlow()

    private var loadJob: Job? = null

    /** Persisted favourites (DataStore); kept so a (re)load shows likes immediately. */
    private var favoriteIds: Set<String> = emptySet()

    init {
        observeFavorites()
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

            // The state updates via observeFavorites() once DataStore has persisted the change.
            is ShortsUiEvent.OnLikeClick -> viewModelScope.launch {
                preferencesRepository.toggleFavorite(event.videoId)
            }

            is ShortsUiEvent.OnShareClick -> viewModelScope.launch {
                _effects.send(ShortsUiEffect.Share(event.video))
            }

            ShortsUiEvent.OnScreenStart -> (_uiState.value as? ShortsUiState.Success)?.let { state ->
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
                            likedIds = favoriteIds,
                        )
                    }
                },
                onFailure = { ShortsUiState.Error(it.message) },
            )
        }
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            preferencesRepository.userPreferences
                .map { it.favoriteVideoIds }
                .distinctUntilChanged()
                .collect { ids ->
                    favoriteIds = ids
                    updateSuccess { it.copy(likedIds = ids) }
                }
        }
    }

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
