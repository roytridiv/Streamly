package com.tridivroy.streamly.presentation.profile

import com.tridivroy.streamly.domain.model.SignInMethod
import com.tridivroy.streamly.domain.model.ThemeMode
import com.tridivroy.streamly.domain.model.UserSession

/**
 * Profile has no error or empty state: everything it shows comes from DataStore, which falls back to
 * defaults rather than failing, and "signed out" is a designed state rather than an absence of data.
 */
sealed interface ProfileUiState {

    data object Loading : ProfileUiState

    /** The sign-in pitch. [pendingMethod] is the button currently showing its spinner. */
    data class SignedOut(
        val pendingMethod: SignInMethod? = null,
    ) : ProfileUiState

    data class SignedIn(
        val session: UserSession,
        val stats: WatchStats,
        val themeMode: ThemeMode,
        /** Coil's disk cache, for the "Clear cache" row. `null` until measured. */
        val cacheBytes: Long? = null,
    ) : ProfileUiState
}

/** The three figures on the profile card, all counted from data the app already keeps. */
data class WatchStats(
    val videosWatched: Int = 0,
    val downloads: Int = 0,
    val favourites: Int = 0,
)

sealed interface ProfileUiEvent {
    data class OnSignInClick(val method: SignInMethod) : ProfileUiEvent
    data object OnSignOutClick : ProfileUiEvent
    data class OnThemeModeSelect(val mode: ThemeMode) : ProfileUiEvent
    data object OnClearCacheClick : ProfileUiEvent
    data object OnDownloadsClick : ProfileUiEvent
}

sealed interface ProfileUiEffect {
    data object NavigateToDownloads : ProfileUiEffect

    /** Session cleared: the nav graph resets the back stack to onboarding. */
    data object NavigateToOnboarding : ProfileUiEffect
    /** Shows the "Cache cleared" toast with the amount reclaimed. */
    data class CacheCleared(val freedBytes: Long) : ProfileUiEffect
}
