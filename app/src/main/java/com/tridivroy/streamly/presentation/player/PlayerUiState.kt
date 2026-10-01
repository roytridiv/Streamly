package com.tridivroy.streamly.presentation.player

import com.tridivroy.streamly.domain.model.Video

sealed interface PlayerUiState {

    data object Loading : PlayerUiState

    data class Success(val video: Video) : PlayerUiState

    /** The video exists but has no playable stream URL. */
    data object Empty : PlayerUiState

    /** [message] is `null` when there is nothing more useful than a generic error to show. */
    data class Error(val message: String?) : PlayerUiState
}
