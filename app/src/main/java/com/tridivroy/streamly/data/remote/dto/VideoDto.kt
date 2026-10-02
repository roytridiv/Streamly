package com.tridivroy.streamly.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VideoDto(
    val id: String,
    val title: String? = null,
    val description: String? = null,
    @SerialName("video_url") val videoUrl: String,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    val category: String? = null,
    /** Length in seconds. */
    val duration: Long? = null,
    @SerialName("is_short") val isShort: Boolean? = null,
    val channel: ChannelDto? = null,
    @SerialName("view_count") val viewCount: Long? = null,
    @SerialName("like_count") val likeCount: Long? = null,
    @SerialName("comment_count") val commentCount: Long? = null,
    /** Unix seconds. */
    @SerialName("published_at") val publishedAt: Long? = null,
    val chapters: List<ChapterDto>? = null,
    @SerialName("sound_label") val soundLabel: String? = null,
    val tags: List<String>? = null,
)

@Serializable
data class ChannelDto(
    val name: String? = null,
    val handle: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("subscriber_count") val subscriberCount: Long? = null,
)

@Serializable
data class ChapterDto(
    /** Offset from the start, in seconds. */
    val position: Long,
    val label: String? = null,
)
