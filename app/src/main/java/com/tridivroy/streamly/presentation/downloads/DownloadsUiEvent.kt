package com.tridivroy.streamly.presentation.downloads

import com.tridivroy.streamly.domain.model.Video
import com.tridivroy.streamly.domain.model.VideoDownload

/** User actions sent from [DownloadsScreen] to [DownloadsViewModel]. */
sealed interface DownloadsUiEvent {
    data class OnDownloadClick(val download: VideoDownload) : DownloadsUiEvent
    data class OnRemoveClick(val videoId: String) : DownloadsUiEvent
    /** Pauses an in-flight download, or resumes a paused one. */
    data class OnPauseToggleClick(val download: VideoDownload) : DownloadsUiEvent
    data class OnRetryDownloadClick(val video: Video) : DownloadsUiEvent
    /** Re-reads the download list after an error. */
    data object Retry : DownloadsUiEvent
}

/** One-off actions sent from [DownloadsViewModel] to [DownloadsScreen], consumed exactly once. */
sealed interface DownloadsUiEffect {
    data class NavigateToPlayer(val videoId: String) : DownloadsUiEffect
}
