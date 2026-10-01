package com.tridivroy.streamly.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.tridivroy.streamly.domain.model.PlaybackQuality
import com.tridivroy.streamly.domain.model.ThemeMode
import com.tridivroy.streamly.domain.model.UserPreferences
import com.tridivroy.streamly.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** [PreferencesRepository] backed by Preferences DataStore. Enums are stored by name. */
@Singleton
class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : PreferencesRepository {

    override val userPreferences: Flow<UserPreferences> = dataStore.data
        // A corrupt or unreadable file falls back to defaults instead of crashing collectors.
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            UserPreferences(
                favoriteVideoIds = prefs[Keys.FAVORITE_VIDEO_IDS].orEmpty(),
                playbackQuality = prefs[Keys.PLAYBACK_QUALITY].toEnumOrNull<PlaybackQuality>() ?: PlaybackQuality.Auto,
                themeMode = prefs[Keys.THEME_MODE].toEnumOrNull<ThemeMode>() ?: ThemeMode.System,
            )
        }
        .distinctUntilChanged()

    override suspend fun toggleFavorite(videoId: String) {
        dataStore.edit { prefs ->
            val current = prefs[Keys.FAVORITE_VIDEO_IDS].orEmpty()
            prefs[Keys.FAVORITE_VIDEO_IDS] = if (videoId in current) current - videoId else current + videoId
        }
    }

    override suspend fun setPlaybackQuality(quality: PlaybackQuality) {
        dataStore.edit { it[Keys.PLAYBACK_QUALITY] = quality.name }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    private object Keys {
        val FAVORITE_VIDEO_IDS = stringSetPreferencesKey("favorite_video_ids")
        val PLAYBACK_QUALITY = stringPreferencesKey("playback_quality")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }
}

private inline fun <reified T : Enum<T>> String?.toEnumOrNull(): T? =
    this?.let { name -> enumValues<T>().firstOrNull { it.name == name } }
