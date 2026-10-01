package com.tridivroy.streamly.presentation.downloads

import com.tridivroy.streamly.domain.model.VideoDownload

sealed interface DownloadsUiState {

    data object Loading : DownloadsUiState

    data class Success(val downloads: List<VideoDownload>) : DownloadsUiState

    data object Empty : DownloadsUiState

    /** [message] is `null` when there is nothing more useful than a generic error to show. */
    data class Error(val message: String?) : DownloadsUiState
}
