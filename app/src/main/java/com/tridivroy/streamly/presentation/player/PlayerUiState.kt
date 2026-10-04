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
        /** Persisted as a favourite. Drives the Like button's fill and count. */
        val isLiked: Boolean = false,
        /** The user subscribes to this video's channel. */
        val isSubscribed: Boolean = false,
        /** Mirrors the shared player's volume, so it carries over to the next video and the mini-player. */
        val isMuted: Boolean = false,
        /**
         * Playback stalled because the network dropped mid-stream: the video is paused under a
         * "No Internet Connection" overlay. Never set while playing a download.
         */
        val isOffline: Boolean = false,
    ) : PlayerUiState {
        /**
         * Like count with the user's own optimistic like folded in, so tapping Like moves the number
         * without waiting for a server that has no such endpoint.
         */
        val likeCount: Long get() = video.stats.likeCount + if (isLiked) 1 else 0
    }

    /** No network and the video isn't downloaded, so playback can't start. Retries itself when back online. */
    data object Offline : PlayerUiState

    /** The video exists but has no playable stream URL. */
    data object Empty : PlayerUiState

    /** [message] is `null` when there is nothing more useful than a generic error to show. */
    data class Error(val message: String?) : PlayerUiState
}

/** The tabs shown under (or beside) the video surface. */
enum class PlayerTab {
    Overview,
    KeyMoments,
    UpNext,
}

/** Up Next's two layouts. Purely a UI preference, remembered per Player entry. */
enum class UpNextLayout {
    List,
    Carousel,
}
