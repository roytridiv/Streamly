package com.tridivroy.streamly.data.repository

import android.util.Log
import com.tridivroy.streamly.data.mapper.toDomain
import com.tridivroy.streamly.data.remote.dto.VideoDto
import com.tridivroy.streamly.domain.model.Video
import com.tridivroy.streamly.domain.repository.VideoRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * Fetches videos from the Streamly API. When the API is unreachable or fails,
 * falls back to public HLS test streams so the app always has playable content.
 */
class VideoRepositoryImpl @Inject constructor(
    private val client: HttpClient,
) : VideoRepository {

    override suspend fun getHomeVideos(): Result<List<Video>> =
        fetchOrFallback(FALLBACK_HOME) {
            client.get("videos").body<List<VideoDto>>().map { it.toDomain() }
        }

    override suspend fun getShortsVideos(): Result<List<Video>> =
        fetchOrFallback(FALLBACK_SHORTS) {
            client.get("shorts").body<List<VideoDto>>().map { it.toDomain() }
        }

    override suspend fun getVideoById(id: String): Result<Video> {
        val remote = runCatchingCancellable { client.get("videos/$id").body<VideoDto>().toDomain() }
        if (remote.isSuccess) return remote

        val fallback = (FALLBACK_HOME + FALLBACK_SHORTS).find { it.id == id }
        return if (fallback != null) {
            Log.w(TAG, "getVideoById($id) failed, using fallback", remote.exceptionOrNull())
            Result.success(fallback)
        } else {
            remote
        }
    }

    private suspend fun fetchOrFallback(
        fallback: List<Video>,
        fetch: suspend () -> List<Video>,
    ): Result<List<Video>> {
        val remote = runCatchingCancellable { fetch() }
        remote.exceptionOrNull()?.let { Log.w(TAG, "Remote fetch failed, using fallback", it) }
        return Result.success(remote.getOrElse { fallback })
    }

    /** Like [runCatching], but rethrows [CancellationException] so coroutine cancellation still works. */
    private suspend fun <T> runCatchingCancellable(block: suspend () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    private companion object {
        const val TAG = "VideoRepository"

        fun thumbnail(seed: String, portrait: Boolean = false) =
            if (portrait) "https://picsum.photos/seed/$seed/360/640"
            else "https://picsum.photos/seed/$seed/640/360"

        val FALLBACK_HOME = listOf(
            Video(
                id = "fallback-bbb",
                title = "Big Buck Bunny",
                description = "Blender Foundation open movie about a giant rabbit and three bullying rodents.",
                videoUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                thumbnailUrl = thumbnail("bbb"),
                category = "Animation",
                duration = 635,
                isShort = false,
            ),
            Video(
                id = "fallback-tos",
                title = "Tears of Steel",
                description = "Blender Foundation sci-fi short film set in a dystopian Amsterdam.",
                videoUrl = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
                thumbnailUrl = thumbnail("tos"),
                category = "Sci-Fi",
                duration = 734,
                isShort = false,
            ),
            Video(
                id = "fallback-bipbop",
                title = "Apple BipBop (fMP4)",
                description = "Apple's advanced HLS test stream with multiple bitrates.",
                videoUrl = "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_fmp4/master.m3u8",
                thumbnailUrl = thumbnail("bipbop"),
                category = "Test",
                duration = 600,
                isShort = false,
            ),
            Video(
                id = "fallback-mux-test",
                title = "Mux Test Stream",
                description = "Mux public HLS test stream.",
                videoUrl = "https://test-streams.mux.dev/test_001/stream.m3u8",
                thumbnailUrl = thumbnail("muxtest"),
                category = "Test",
                duration = 510,
                isShort = false,
            ),
            Video(
                id = "fallback-dark-truths",
                title = "Big Buck Bunny: Dark Truths",
                description = "Shaka Player demo stream.",
                videoUrl = "https://storage.googleapis.com/shaka-demo-assets/bbb-dark-truths-hls/hls.m3u8",
                thumbnailUrl = thumbnail("darktruths"),
                category = "Animation",
                duration = 372,
                isShort = false,
            ),
        )

        val FALLBACK_SHORTS = listOf(
            Video(
                id = "fallback-short-angel-one",
                title = "Angel One",
                description = "Short Shaka Player demo clip.",
                videoUrl = "https://storage.googleapis.com/shaka-demo-assets/angel-one-hls/hls.m3u8",
                thumbnailUrl = thumbnail("angelone", portrait = true),
                category = "Drama",
                duration = 60,
                isShort = true,
            ),
            Video(
                id = "fallback-short-pts-shift",
                title = "Mux PTS Shift",
                description = "Short Mux HLS test clip.",
                videoUrl = "https://test-streams.mux.dev/pts_shift/master.m3u8",
                thumbnailUrl = thumbnail("ptsshift", portrait = true),
                category = "Test",
                duration = 165,
                isShort = true,
            ),
        )
    }
}
