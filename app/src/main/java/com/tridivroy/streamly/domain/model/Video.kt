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
)
