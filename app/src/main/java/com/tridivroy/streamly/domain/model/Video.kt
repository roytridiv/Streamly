package com.tridivroy.streamly.domain.model

data class Video(
    val id: String,
    val title: String,
    val description: String,
    val videoUrl: String,
    val thumbnailUrl: String,
    val category: String,
    /** Length in seconds. */
    val duration: Long,
    val isShort: Boolean,
    /** Who published it. [Channel.Unknown] when the source does not say. */
    val channel: Channel = Channel.Unknown,
    val stats: VideoStats = VideoStats(),
    /** Named positions in the timeline, ascending. Empty when the source has no chapters. */
    val chapters: List<Chapter> = emptyList(),
    /** Shorts only: the audio track credited in the overlay. */
    val soundLabel: String? = null,
    /** Hashtags shown above the description. */
    val tags: List<String> = emptyList(),
)

data class Channel(
    val name: String,
    /** Without the leading "@". */
    val handle: String,
    val avatarUrl: String,
    val subscriberCount: Long,
) {
    /** Fallback initial for the avatar tile when [avatarUrl] fails or is blank. */
    val initial: String get() = name.firstOrNull()?.uppercase() ?: "?"

    companion object {
        val Unknown = Channel(name = "", handle = "", avatarUrl = "", subscriberCount = 0L)
    }
}

data class VideoStats(
    val viewCount: Long = 0L,
    val likeCount: Long = 0L,
    val commentCount: Long = 0L,
    /** Upload time, for the "3 days ago" label. `null` when unknown. */
    val publishedAtEpochSeconds: Long? = null,
)

/** A named position in a video's timeline — the Player's "Key Moments". */
data class Chapter(
    val positionSeconds: Long,
    val label: String,
)
