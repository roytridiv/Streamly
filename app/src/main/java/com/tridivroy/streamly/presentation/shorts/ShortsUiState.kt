package com.tridivroy.streamly.presentation.shorts

import com.tridivroy.streamly.domain.model.Video

sealed interface ShortsUiState {

    data object Loading : ShortsUiState

    data class Success(
        val videos: List<Video>,
        /** Page that is playing. */
        val activeIndex: Int = 0,
        /** Neighbour page prepared in the background, or `null` if there is none. */
        val preloadIndex: Int? = null,
        /** `true` only when the user tapped to pause; lifecycle pauses don't set it. */
        val isPaused: Boolean = false,
        /** Favourited video IDs, persisted in DataStore. */
        val likedIds: Set<String> = emptySet(),
    ) : ShortsUiState

    data object Empty : ShortsUiState

    /** [message] is `null` when there is nothing more useful than a generic error to show. */
    data class Error(val message: String?) : ShortsUiState
}
