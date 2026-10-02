package com.tridivroy.streamly.domain.model

data class VideoDownload(
    val video: Video,
    val status: DownloadStatus,
    /** 0–100, or `null` while the total size is still unknown. */
    val progressPercent: Float?,
    val bytesDownloaded: Long,
    /** Height of the downloaded rendition, e.g. 720. `null` when the source did not declare one. */
    val videoHeight: Int? = null,
)

enum class DownloadStatus {
    /** Waiting to start, or paused until requirements (e.g. network) are met. */
    Queued,
    Downloading,
    Downloaded,
    Failed,
}
