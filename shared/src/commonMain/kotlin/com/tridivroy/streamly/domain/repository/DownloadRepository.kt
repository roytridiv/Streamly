package com.tridivroy.streamly.domain.repository

import com.tridivroy.streamly.domain.model.Video
import com.tridivroy.streamly.domain.model.VideoDownload
import kotlinx.coroutines.flow.Flow

interface DownloadRepository {
    /** All known downloads (any status), newest first. */
    val downloads: Flow<List<VideoDownload>>

    /** Queues [video] for offline download. Fails if its stream can't be inspected. */
    suspend fun download(video: Video): Result<Unit>

    /** Cancels an in-progress download or deletes a finished one. */
    fun remove(videoId: String)

    /** Pauses or resumes one in-flight download. Finished downloads are unaffected. */
    fun setPaused(videoId: String, paused: Boolean)

    /** Metadata of a fully downloaded video, available without network; `null` if not downloaded. */
    suspend fun getDownloadedVideo(videoId: String): Video?
}
