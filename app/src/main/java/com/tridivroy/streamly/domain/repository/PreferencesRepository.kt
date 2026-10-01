package com.tridivroy.streamly.domain.repository

import com.tridivroy.streamly.domain.model.PlaybackQuality
import com.tridivroy.streamly.domain.model.ThemeMode
import com.tridivroy.streamly.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    val userPreferences: Flow<UserPreferences>

    suspend fun toggleFavorite(videoId: String)
    suspend fun setPlaybackQuality(quality: PlaybackQuality)
    suspend fun setThemeMode(mode: ThemeMode)
}
