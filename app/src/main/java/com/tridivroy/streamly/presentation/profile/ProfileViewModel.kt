package com.tridivroy.streamly.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tridivroy.streamly.core.storage.ImageCacheCleaner
import com.tridivroy.streamly.domain.model.DownloadStatus
import com.tridivroy.streamly.domain.model.SignInMethod
import com.tridivroy.streamly.domain.model.UserSession
import com.tridivroy.streamly.domain.repository.DownloadRepository
import com.tridivroy.streamly.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Profile state is derived, not owned: the session, theme and stats all come from the flows the rest
 * of the app already writes to, so signing in or finishing a download updates this screen without it
 * knowing anything about either.
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    downloadRepository: DownloadRepository,
    private val imageCacheCleaner: ImageCacheCleaner,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _effects = Channel<ProfileUiEffect>(Channel.BUFFERED)
    val effects: Flow<ProfileUiEffect> = _effects.receiveAsFlow()

    /**
     * Set the moment sign-out starts. Clearing the session makes the preferences flow emit a
     * sessionless state, which would crossfade this screen to its own sign-in card for a frame on its
     * way to onboarding. While this is true the last signed-in frame is held instead.
     */
    private var signingOut = false

    init {
        viewModelScope.launch {
            combine(
                preferencesRepository.userPreferences,
                downloadRepository.downloads,
            ) { prefs, downloads ->
                val session = prefs.session
                when {
                    session == null && signingOut -> _uiState.value

                    // Keep any in-flight sign-in spinner rather than resetting it on every emission.
                    session == null -> (_uiState.value as? ProfileUiState.SignedOut) ?: ProfileUiState.SignedOut()

                    else -> ProfileUiState.SignedIn(
                        session = session,
                        stats = WatchStats(
                            videosWatched = prefs.watchedVideoIds.size,
                            downloads = downloads.count { it.status == DownloadStatus.Downloaded },
                            favourites = prefs.favoriteVideoIds.size,
                        ),
                        themeMode = prefs.themeMode,
                        // Preserved across emissions so the measured size does not flicker away.
                        cacheBytes = (_uiState.value as? ProfileUiState.SignedIn)?.cacheBytes,
                    )
                }
            }.collect { state ->
                _uiState.value = state
                if (state is ProfileUiState.SignedIn && state.cacheBytes == null) measureCache()
            }
        }
    }

    fun onEvent(event: ProfileUiEvent) {
        when (event) {
            is ProfileUiEvent.OnSignInClick -> signIn(event.method)

            ProfileUiEvent.OnSignOutClick -> viewModelScope.launch {
                signingOut = true
                preferencesRepository.signOut()
                _effects.send(ProfileUiEffect.NavigateToOnboarding)
            }

            is ProfileUiEvent.OnThemeModeSelect -> viewModelScope.launch {
                preferencesRepository.setThemeMode(event.mode)
            }

            ProfileUiEvent.OnClearCacheClick -> viewModelScope.launch {
                val freed = imageCacheCleaner.clear()
                _uiState.update { state ->
                    if (state is ProfileUiState.SignedIn) state.copy(cacheBytes = imageCacheCleaner.size()) else state
                }
                _effects.send(ProfileUiEffect.CacheCleared(freed))
            }

            ProfileUiEvent.OnDownloadsClick -> viewModelScope.launch {
                _effects.send(ProfileUiEffect.NavigateToDownloads)
            }
        }
    }

    /**
     * Mock sign-in: there is no identity provider behind the buttons yet, so this writes the
     * placeholder account straight to DataStore. The short delay is not fake work — it keeps the
     * button's spinner on screen long enough to read as a response rather than a flicker, and it is
     * the one line that goes when a real provider is wired in.
     */
    private fun signIn(method: SignInMethod) {
        if ((_uiState.value as? ProfileUiState.SignedOut)?.pendingMethod != null) return
        viewModelScope.launch {
            _uiState.value = ProfileUiState.SignedOut(pendingMethod = method)
            delay(SIGN_IN_FEEDBACK_MS)
            preferencesRepository.signIn(UserSession.forMethod(method))
        }
    }

    private fun measureCache() {
        viewModelScope.launch {
            val size = imageCacheCleaner.size()
            _uiState.update { state ->
                if (state is ProfileUiState.SignedIn) state.copy(cacheBytes = size) else state
            }
        }
    }

    private companion object {
        const val SIGN_IN_FEEDBACK_MS = 450L
    }
}
