package com.tridivroy.streamly.core.media

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadHelper
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadService
import com.tridivroy.streamly.domain.model.Channel
import com.tridivroy.streamly.domain.model.Chapter
import com.tridivroy.streamly.domain.model.DownloadStatus
import com.tridivroy.streamly.domain.model.Video
import com.tridivroy.streamly.domain.model.VideoDownload
import com.tridivroy.streamly.domain.model.VideoStats
import com.tridivroy.streamly.domain.repository.DownloadRepository
import com.tridivroy.streamly.domain.repository.PreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Bridges Media3's [DownloadManager] to the domain [DownloadRepository].
 *
 * - Starting/removing goes through [MediaDownloadService] so downloads continue in the background.
 * - Video metadata is stored in the download request itself, so the Downloads list and the Player
 *   work offline without the API.
 * - [DownloadManager] has no progress callback, so progress is polled while anything downloads.
 *
 * Must first be created on the main thread (DownloadManager callbacks arrive there).
 */
@OptIn(UnstableApi::class)
@Singleton
class DownloadTracker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadManager: DownloadManager,
    private val httpDataSourceFactory: HttpDataSource.Factory,
    private val renderersFactory: RenderersFactory,
    private val preferencesRepository: PreferencesRepository,
    private val json: Json,
) : DownloadRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** Immutable snapshots keyed by video ID; Media3's [Download] progress mutates in place. */
    private val snapshots = MutableStateFlow<Map<String, Snapshot>>(emptyMap())
    private var progressJob: Job? = null

    override val downloads: Flow<List<VideoDownload>> = snapshots.map { byId ->
        byId.values.sortedByDescending { it.startTimeMs }.map { it.download }
    }

    init {
        downloadManager.addListener(object : DownloadManager.Listener {
            override fun onDownloadChanged(manager: DownloadManager, download: Download, finalException: Exception?) {
                put(download)
                updateProgressPolling()
            }

            override fun onDownloadRemoved(manager: DownloadManager, download: Download) {
                snapshots.update { it - download.request.id }
                updateProgressPolling()
            }
        })

        scope.launch {
            val stored = withContext(Dispatchers.IO) { readIndex() }
            // Listener updates that raced with the read are newer, so they win.
            snapshots.update { live -> stored + live }
            updateProgressPolling()
        }
    }

    override suspend fun download(video: Video): Result<Unit> = try {
        val quality = preferencesRepository.userPreferences.first().playbackQuality
        val trackSelection = DownloadHelper.getDefaultTrackSelectorParameters(context).buildUpon()
            .apply { quality.maxVideoHeight?.let { setMaxVideoSize(Int.MAX_VALUE, it) } }
            // Keep a single rendition (best that fits the cap) instead of every HLS variant.
            .setForceHighestSupportedBitrate(true)
            .build()

        // DownloadHelper is main-thread bound: it calls back on the looper that prepared it.
        val request = withContext(Dispatchers.Main) {
            val helper = DownloadHelper.forMediaItem(
                video.toMediaItem(),
                trackSelection,
                renderersFactory,
                httpDataSourceFactory,
            )
            try {
                helper.awaitPrepared()
                val metadata = video.toMetadata(videoHeight = helper.selectedVideoHeight())
                helper.getDownloadRequest(
                    video.id,
                    json.encodeToString(DownloadMetadata.serializer(), metadata).encodeToByteArray(),
                )
            } finally {
                helper.release()
            }
        }
        DownloadService.sendAddDownload(context, MediaDownloadService::class.java, request, /* foreground = */ true)
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    override fun remove(videoId: String) {
        DownloadService.sendRemoveDownload(context, MediaDownloadService::class.java, videoId, /* foreground = */ false)
    }

    override fun setPaused(videoId: String, paused: Boolean) {
        DownloadService.sendSetStopReason(
            context,
            MediaDownloadService::class.java,
            videoId,
            if (paused) STOP_REASON_PAUSED else Download.STOP_REASON_NONE,
            /* foreground = */ false,
        )
    }

    override suspend fun getDownloadedVideo(videoId: String): Video? =
        completedDownload(videoId)?.let { decodeMetadata(it)?.toVideo() }

    /**
     * Playable item for a finished download, carrying its stream keys so the player only requests
     * the downloaded rendition — required for HLS to play offline. `null` if not downloaded.
     */
    suspend fun downloadedMediaItem(videoId: String): MediaItem? =
        completedDownload(videoId)?.request?.toMediaItem()

    private suspend fun completedDownload(videoId: String): Download? = withContext(Dispatchers.IO) {
        try {
            downloadManager.downloadIndex.getDownload(videoId)?.takeIf { it.state == Download.STATE_COMPLETED }
        } catch (e: IOException) {
            null
        }
    }

    private fun readIndex(): Map<String, Snapshot> = try {
        downloadManager.downloadIndex.getDownloads().use { cursor ->
            buildMap {
                while (cursor.moveToNext()) {
                    val download = cursor.download
                    download.toSnapshot()?.let { put(download.request.id, it) }
                }
            }
        }
    } catch (e: IOException) {
        emptyMap()
    }

    private fun put(download: Download) {
        val snapshot = download.toSnapshot()
        snapshots.update { if (snapshot == null) it - download.request.id else it + (download.request.id to snapshot) }
    }

    private fun updateProgressPolling() {
        val anyDownloading = snapshots.value.values.any { it.download.status == DownloadStatus.Downloading }
        if (!anyDownloading) {
            progressJob?.cancel()
            progressJob = null
        } else if (progressJob?.isActive != true) {
            progressJob = scope.launch {
                while (isActive) {
                    delay(PROGRESS_POLL_MS)
                    downloadManager.currentDownloads.forEach(::put)
                }
            }
        }
    }

    private fun Download.toSnapshot(): Snapshot? {
        val status = when (state) {
            // STATE_STOPPED means something set a stopReason. Ours (STOP_REASON_PAUSED) is the user's
            // doing and is resumable; any other stop reason is the download manager waiting, which the
            // UI should not offer a resume button for.
            Download.STATE_STOPPED -> if (stopReason == STOP_REASON_PAUSED) DownloadStatus.Paused else DownloadStatus.Queued
            Download.STATE_QUEUED, Download.STATE_RESTARTING -> DownloadStatus.Queued
            Download.STATE_DOWNLOADING -> DownloadStatus.Downloading
            Download.STATE_COMPLETED -> DownloadStatus.Downloaded
            Download.STATE_FAILED -> DownloadStatus.Failed
            else -> return null // STATE_REMOVING: about to disappear.
        }
        val video = decodeMetadata(this)?.toVideo() ?: return null
        return Snapshot(
            download = VideoDownload(
                video = video,
                status = status,
                // C.PERCENTAGE_UNSET (-1) until the total size is known.
                progressPercent = percentDownloaded.takeIf { it >= 0f },
                bytesDownloaded = bytesDownloaded,
                videoHeight = decodeMetadata(this)?.videoHeight,
            ),
            startTimeMs = startTimeMs,
        )
    }

    private fun decodeMetadata(download: Download): DownloadMetadata? =
        runCatching { json.decodeFromString(DownloadMetadata.serializer(), download.request.data.decodeToString()) }.getOrNull()

    private data class Snapshot(val download: VideoDownload, val startTimeMs: Long)

    internal companion object {
        const val PROGRESS_POLL_MS = 1_000L

        /**
         * Our own stop reason, so a user pause can be told apart from the download manager stopping a
         * download itself. Any non-zero value works; Media3 only compares it to `STOP_REASON_NONE`.
         */
        const val STOP_REASON_PAUSED = 1
    }
}

/**
 * Height of the video rendition the helper selected, so Downloads can label what is on disk rather
 * than what was asked for. `null` when the manifest does not declare one, or on any surprise — a
 * missing label must never fail a download.
 */
@OptIn(UnstableApi::class)
private fun DownloadHelper.selectedVideoHeight(): Int? = runCatching {
    (0 until periodCount)
        .flatMap { period -> getTracks(period).groups }
        .filter { group -> group.type == C.TRACK_TYPE_VIDEO }
        .flatMap { group ->
            (0 until group.length)
                .filter { group.isTrackSelected(it) }
                .map { group.getTrackFormat(it).height }
        }
        .filter { it != Format.NO_VALUE }
        .maxOrNull()
}.getOrNull()

private suspend fun DownloadHelper.awaitPrepared() = suspendCancellableCoroutine { continuation ->
    prepare(object : DownloadHelper.Callback {
        override fun onPrepared(helper: DownloadHelper) = continuation.resume(Unit)
        override fun onPrepareError(helper: DownloadHelper, e: IOException) = continuation.resumeWithException(e)
    })
}

/**
 * Video fields persisted inside the download request, so a downloaded video shows its channel,
 * chapters and stats with no network (the domain model stays serialization-free).
 */
@Serializable
private data class DownloadMetadata(
    val id: String,
    val title: String,
    val description: String,
    val videoUrl: String,
    val thumbnailUrl: String,
    val category: String,
    val duration: Long,
    val isShort: Boolean,
    val channel: ChannelMetadata? = null,
    val viewCount: Long = 0L,
    val likeCount: Long = 0L,
    val commentCount: Long = 0L,
    val publishedAt: Long? = null,
    val chapters: List<ChapterMetadata> = emptyList(),
    val soundLabel: String? = null,
    val tags: List<String> = emptyList(),
    /** Height of the rendition actually written to the cache, for the "720p" label in Downloads. */
    val videoHeight: Int? = null,
) {
    fun toVideo() = Video(
        id = id,
        title = title,
        description = description,
        videoUrl = videoUrl,
        thumbnailUrl = thumbnailUrl,
        category = category,
        duration = duration,
        isShort = isShort,
        channel = channel?.toChannel() ?: Channel.Unknown,
        stats = VideoStats(viewCount, likeCount, commentCount, publishedAt),
        chapters = chapters.map { Chapter(it.position, it.label) },
        soundLabel = soundLabel,
        tags = tags,
    )
}

@Serializable
private data class ChannelMetadata(
    val name: String,
    val handle: String,
    val avatarUrl: String,
    val subscriberCount: Long,
) {
    fun toChannel() = Channel(name, handle, avatarUrl, subscriberCount)
}

@Serializable
private data class ChapterMetadata(val position: Long, val label: String)

private fun Video.toMetadata(videoHeight: Int? = null) = DownloadMetadata(
    id = id,
    title = title,
    description = description,
    videoUrl = videoUrl,
    thumbnailUrl = thumbnailUrl,
    category = category,
    duration = duration,
    isShort = isShort,
    channel = channel.takeIf { it != Channel.Unknown }
        ?.let { ChannelMetadata(it.name, it.handle, it.avatarUrl, it.subscriberCount) },
    viewCount = stats.viewCount,
    likeCount = stats.likeCount,
    commentCount = stats.commentCount,
    publishedAt = stats.publishedAtEpochSeconds,
    chapters = chapters.map { ChapterMetadata(it.positionSeconds, it.label) },
    soundLabel = soundLabel,
    tags = tags,
    videoHeight = videoHeight,
)
