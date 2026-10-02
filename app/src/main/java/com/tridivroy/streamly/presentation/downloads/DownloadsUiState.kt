package com.tridivroy.streamly.presentation.downloads

import com.tridivroy.streamly.core.storage.StorageUsage
import com.tridivroy.streamly.domain.model.DownloadStatus
import com.tridivroy.streamly.domain.model.VideoDownload

sealed interface DownloadsUiState {

    data object Loading : DownloadsUiState

    data class Success(
        val downloads: List<VideoDownload>,
        /** Device storage figures for the usage card; hidden while [StorageUsage.isKnown] is false. */
        val storage: StorageUsage = StorageUsage(),
    ) : DownloadsUiState {
        val readyCount: Int get() = downloads.count { it.status == DownloadStatus.Downloaded }
        val savingCount: Int
            get() = downloads.count { it.status == DownloadStatus.Queued || it.status == DownloadStatus.Downloading }
    }

    data object Empty : DownloadsUiState

    /** [message] is `null` when there is nothing more useful than a generic error to show. */
    data class Error(val message: String?) : DownloadsUiState
}
