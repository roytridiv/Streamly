package com.tridivroy.streamly.data.mapper

import com.tridivroy.streamly.data.remote.dto.ChannelDto
import com.tridivroy.streamly.data.remote.dto.ChapterDto
import com.tridivroy.streamly.data.remote.dto.VideoDto
import com.tridivroy.streamly.domain.model.Channel
import com.tridivroy.streamly.domain.model.Chapter
import com.tridivroy.streamly.domain.model.Video
import com.tridivroy.streamly.domain.model.VideoStats

fun VideoDto.toDomain(): Video = Video(
    id = id,
    title = title.orEmpty(),
    description = description.orEmpty(),
    videoUrl = videoUrl,
    thumbnailUrl = thumbnailUrl.orEmpty(),
    category = category.orEmpty(),
    duration = duration ?: 0L,
    isShort = isShort ?: false,
    channel = channel?.toDomain() ?: Channel.Unknown,
    stats = VideoStats(
        viewCount = viewCount ?: 0L,
        likeCount = likeCount ?: 0L,
        commentCount = commentCount ?: 0L,
        publishedAtEpochSeconds = publishedAt,
    ),
    // The Player's Key Moments assume ascending order; the API does not promise it.
    chapters = chapters?.map { it.toDomain() }?.sortedBy { it.positionSeconds }.orEmpty(),
    soundLabel = soundLabel,
    tags = tags.orEmpty(),
)

private fun ChannelDto.toDomain(): Channel = Channel(
    name = name.orEmpty(),
    handle = handle.orEmpty().removePrefix("@"),
    avatarUrl = avatarUrl.orEmpty(),
    subscriberCount = subscriberCount ?: 0L,
)

private fun ChapterDto.toDomain(): Chapter = Chapter(
    positionSeconds = position,
    label = label.orEmpty(),
)
