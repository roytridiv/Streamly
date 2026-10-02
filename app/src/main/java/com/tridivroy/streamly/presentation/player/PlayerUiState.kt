package com.tridivroy.streamly.presentation.player

import com.tridivroy.streamly.domain.model.Video
import com.tridivroy.streamly.domain.model.VideoDownload

sealed interface PlayerUiState {

    data object Loading : PlayerUiState

    data class Success(
        val video: Video,
        /** Offline download of this video, or `null` if never downloaded. */
        val download: VideoDownload? = null,
        /** "Up Next" suggestions. Empty while they load, or if they could not be fetched. */
        val relatedVideos: List<Video> = emptyList(),
    ) : PlayerUiState

    /** The video exists but has no playable stream URL. */
    data object Empty : PlayerUiState

    /** [message] is `null` when there is nothing more useful than a generic error to show. */
    data class Error(val message: String?) : PlayerUiState
}

/** The tabs shown under (or beside) the video surface. */
enum class PlayerTab {
    Details,
    UpNext,
}
