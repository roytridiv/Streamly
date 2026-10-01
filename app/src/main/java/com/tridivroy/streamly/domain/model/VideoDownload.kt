package com.tridivroy.streamly.domain.model

data class VideoDownload(
    val video: Video,
    val status: DownloadStatus,
    /** 0–100, or `null` while the total size is still unknown. */
    val progressPercent: Float?,
    val bytesDownloaded: Long,
)

enum class DownloadStatus {
    /** Waiting to start, or paused until requirements (e.g. network) are met. */
    Queued,
    Downloading,
    Downloaded,
    Failed,
}
