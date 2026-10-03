package com.tridivroy.streamly.data.repository

import com.tridivroy.streamly.data.mapper.toDomain
import com.tridivroy.streamly.data.remote.dto.VideoDto
import com.tridivroy.streamly.domain.model.Channel
import com.tridivroy.streamly.domain.model.Chapter
import com.tridivroy.streamly.domain.model.Video
import com.tridivroy.streamly.domain.model.VideoStats
import com.tridivroy.streamly.domain.repository.VideoRepository
import com.tridivroy.streamly.shared.currentTimeMillis
import com.tridivroy.streamly.shared.platformLog
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlin.coroutines.cancellation.CancellationException

/**
 * Fetches videos from the Streamly API. When the API is unreachable or fails,
 * falls back to public HLS test streams so the app always has playable content.
 */
class VideoRepositoryImpl(
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
            platformLog(TAG, "getVideoById($id) failed, using fallback", remote.exceptionOrNull())
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
        remote.exceptionOrNull()?.let { platformLog(TAG, "Remote fetch failed, using fallback", it) }
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

        fun avatar(seed: String) = "https://picsum.photos/seed/$seed-avatar/96/96"

        /** Upload time [daysAgo] days before class init, for the "3 days ago" label. */
        fun daysAgo(daysAgo: Long): Long = currentTimeMillis() / 1000 - daysAgo * 86_400

        fun chapters(vararg pairs: Pair<Long, String>): List<Chapter> =
            pairs.map { (position, label) -> Chapter(position, label) }

        val NORDLYS = Channel("Nordlys Films", "nordlys", avatar("nordlys"), 412_000)
        val SLOW_WORKSHOP = Channel("Slow Workshop", "slowworkshop", avatar("slowworkshop"), 198_000)
        val POLAR_TAPES = Channel("Polar Tapes", "polartapes", avatar("polartapes"), 1_120_000)
        val INTERFACE_CLUB = Channel("Interface Club", "interfaceclub", avatar("interfaceclub"), 86_000)
        val KESTREL = Channel("Kestrel Plays", "kestrel", avatar("kestrel"), 540_000)

        val FALLBACK_HOME = listOf(
            Video(
                id = "fallback-bbb",
                title = "Big Buck Bunny",
                description = "Blender Foundation open movie about a giant rabbit and three bullying rodents. " +
                        "Rendered entirely in Blender and released under Creative Commons.",
                videoUrl = "https://storage.googleapis.com/shaka-demo-assets/bbb-dark-truths-hls/hls.m3u8",
                thumbnailUrl = thumbnail("bbb"),
                category = "Kids & Animation",
                duration = 635,
                isShort = false,
                channel = NORDLYS,
                stats = VideoStats(1_200_000, 48_200, 3_104, daysAgo(3)),
                chapters = chapters(
                    0L to "Opening",
                    96L to "The meadow",
                    221L to "First trap",
                    388L to "The chase",
                    530L to "Payback",
                ),
                tags = listOf("animation", "kids", "openmovie"),
            ),
            Video(
                id = "fallback-elephants-dream",
                title = "Elephant's Dream — Open Educational Film",
                description = "The world's first open-source computer-generated short movie, great for open graphics education.",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                thumbnailUrl = thumbnail("elephantsdream"),
                category = "Educational",
                duration = 653,
                isShort = false,
                channel = SLOW_WORKSHOP,
                stats = VideoStats(950_000, 52_000, 2_410, daysAgo(5)),
                chapters = chapters(
                    0L to "Intro",
                    150L to "The Elevator",
                    400L to "Machine Room",
                ),
                tags = listOf("educational", "animation", "cinematic"),
            ),
            Video(
                id = "fallback-tos",
                title = "Tears of Steel (VFX Learning)",
                description = "Blender Foundation sci-fi short film set in a dystopian Amsterdam, widely used for visual effects education.",
                videoUrl = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
                thumbnailUrl = thumbnail("tos"),
                category = "Sci-Fi & VFX",
                duration = 734,
                isShort = false,
                channel = POLAR_TAPES,
                stats = VideoStats(640_000, 21_400, 1_890, daysAgo(7)),
                chapters = chapters(
                    0L to "The bridge",
                    180L to "Flashback",
                    520L to "The machines",
                ),
                tags = listOf("scifi", "vfx", "learning"),
            ),
            Video(
                id = "fallback-caminandes",
                title = "For Bigger Blazes — Educational Sample",
                description = "Google Cloud Sample content demonstrating high bitrate streaming.",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                thumbnailUrl = thumbnail("blazes"),
                category = "Kids & Tech",
                duration = 15,
                isShort = false,
                channel = KESTREL,
                stats = VideoStats(410_000, 18_200, 890, daysAgo(2)),
                chapters = chapters(0L to "Start", 10L to "Fire Test"),
                tags = listOf("kids", "tech"),
            ),
            Video(
                id = "fallback-bipbop",
                title = "Apple BipBop (Adaptive Stream Test)",
                description = "Apple's HLS test stream with multiple bitrates for adaptive stream verification.",
                videoUrl = "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_fmp4/master.m3u8",
                thumbnailUrl = thumbnail("bipbop"),
                category = "Test",
                duration = 600,
                isShort = false,
                channel = INTERFACE_CLUB,
                stats = VideoStats(210_000, 9_800, 412, daysAgo(5)),
                chapters = chapters(0L to "Bars", 140L to "Bitrate shift"),
                tags = listOf("hls", "testing"),
            ),
        )

        val FALLBACK_SHORTS = listOf(
            Video(
                id = "fallback-short-angel-one",
                title = "Angel One — Sci-Fi Clip",
                description = "Vertical HLS stream sample for quick player switching.",
                videoUrl = "https://storage.googleapis.com/shaka-demo-assets/angel-one-hls/hls.m3u8",
                thumbnailUrl = thumbnail("angelone", portrait = true),
                category = "Shorts",
                duration = 60,
                isShort = true,
                channel = POLAR_TAPES,
                stats = VideoStats(84_200, 9_400, 604, daysAgo(2)),
                soundLabel = "Original sound · Nordlys",
                tags = listOf("short", "scifi"),
            ),
            Video(
                id = "fallback-short-pts-shift",
                title = "Fast Motion Shorts Demo",
                description = "Short Mux HLS test clip.",
                videoUrl = "https://test-streams.mux.dev/pts_shift/master.m3u8",
                thumbnailUrl = thumbnail("ptsshift", portrait = true),
                category = "Shorts",
                duration = 165,
                isShort = true,
                channel = INTERFACE_CLUB,
                stats = VideoStats(31_900, 3_100, 388, daysAgo(6)),
                soundLabel = "Tape Pad #4 · Polar Tapes",
                tags = listOf("short", "test"),
            ),
        )
    }
}