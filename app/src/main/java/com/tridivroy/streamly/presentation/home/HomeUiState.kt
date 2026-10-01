package com.tridivroy.streamly.presentation.home

import com.tridivroy.streamly.domain.model.Video

sealed interface HomeUiState {

    data object Loading : HomeUiState

    data class Success(
        val videos: List<Video>,
        val categories: List<String>,
        /** `null` means "All". */
        val selectedCategory: String? = null,
        val isRefreshing: Boolean = false,
    ) : HomeUiState {
        val visibleVideos: List<Video>
            get() = if (selectedCategory == null) videos else videos.filter { it.category == selectedCategory }
    }

    /** [message] is `null` when there is nothing more useful than a generic error to show. */
    data class Error(val message: String?) : HomeUiState
}
