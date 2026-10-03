package com.tridivroy.streamly.domain.repository

import com.tridivroy.streamly.domain.model.Video

interface VideoRepository {
    suspend fun getHomeVideos(): Result<List<Video>>
    suspend fun getShortsVideos(): Result<List<Video>>
    suspend fun getVideoById(id: String): Result<Video>
}
