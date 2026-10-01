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
)
