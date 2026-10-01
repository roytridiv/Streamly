package com.tridivroy.streamly.data.mapper

import com.tridivroy.streamly.data.remote.dto.VideoDto
import com.tridivroy.streamly.domain.model.Video

fun VideoDto.toDomain(): Video = Video(
    id = id,
    title = title.orEmpty(),
    description = description.orEmpty(),
    videoUrl = videoUrl,
    thumbnailUrl = thumbnailUrl.orEmpty(),
    category = category.orEmpty(),
    duration = duration ?: 0L,
    isShort = isShort ?: false,
)
