package com.tridivroy.streamly

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tridivroy.streamly.core.media.NowPlaying
import com.tridivroy.streamly.core.media.NowPlayingStore
import com.tridivroy.streamly.domain.model.DownloadStatus
import com.tridivroy.streamly.domain.model.ThemeMode
import com.tridivroy.streamly.domain.repository.DownloadRepository
import com.tridivroy.streamly.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Activity-level state: the persisted theme choice, plus the two things the app chrome shows
 * regardless of which tab is open — the mini-player and the bottom nav's download badge.
 *
 * These live here rather than in Home's and Downloads' own state because they are not screen
 * content: the same mini-player and the same badge appear above and inside the nav bar on every
 * tab that has one.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    preferencesRepository: PreferencesRepository,
    downloadRepository: DownloadRepository,
    private val nowPlayingStore: NowPlayingStore,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = preferencesRepository.userPreferences
        .map { it.themeMode }
        // Dark is the initial value as well as the stored default, so the first frame is never light.
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.Dark)

    /**
     * Whether a session (including a guest one) exists, for picking the start destination.
     *
     * `null` means DataStore has not been read yet — the nav graph waits rather than guessing, since
     * guessing wrong would either flash onboarding at a returning user or flash Home at a new one.
     */
    val hasSession: StateFlow<Boolean?> = preferencesRepository.userPreferences
        .map { it.session != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** What the mini-player shows, or `null` when nothing is loaded. */
    val nowPlaying: StateFlow<NowPlaying?> = nowPlayingStore.nowPlaying

    /** Queued or in-flight downloads — the mint count on the Downloads tab. */
    val activeDownloadCount: StateFlow<Int> = downloadRepository.downloads
        .map { downloads ->
            downloads.count { it.status == DownloadStatus.Queued || it.status == DownloadStatus.Downloading }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), 0)

    fun onMiniPlayerToggle() = nowPlayingStore.togglePlay()

    /** Stops playback and removes the mini-player entirely. */
    fun onMiniPlayerDismiss() = nowPlayingStore.dismiss()

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
