package com.tridivroy.streamly.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.tridivroy.streamly.domain.model.PlaybackQuality
import com.tridivroy.streamly.domain.model.ThemeMode
import com.tridivroy.streamly.domain.model.SignInMethod
import com.tridivroy.streamly.domain.model.UserPreferences
import com.tridivroy.streamly.domain.model.UserSession
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
                savedVideoIds = prefs[Keys.SAVED_VIDEO_IDS].orEmpty(),
                subscribedChannels = prefs[Keys.SUBSCRIBED_CHANNELS].orEmpty(),
                watchedVideoIds = prefs[Keys.WATCHED_VIDEO_IDS].orEmpty(),
                session = prefs.toSession(),
                playbackQuality = prefs[Keys.PLAYBACK_QUALITY].toEnumOrNull<PlaybackQuality>() ?: PlaybackQuality.Auto,
                themeMode = prefs[Keys.THEME_MODE].toEnumOrNull<ThemeMode>() ?: ThemeMode.Dark,
            )
        }
        .distinctUntilChanged()

    override suspend fun toggleFavorite(videoId: String) {
        dataStore.edit { prefs ->
            prefs[Keys.FAVORITE_VIDEO_IDS] = prefs[Keys.FAVORITE_VIDEO_IDS].orEmpty().toggle(videoId)
        }
    }

    override suspend fun toggleSaved(videoId: String) {
        dataStore.edit { prefs -> prefs[Keys.SAVED_VIDEO_IDS] = prefs[Keys.SAVED_VIDEO_IDS].orEmpty().toggle(videoId) }
    }

    override suspend fun toggleSubscription(channelHandle: String) {
        dataStore.edit { prefs -> prefs[Keys.SUBSCRIBED_CHANNELS] = prefs[Keys.SUBSCRIBED_CHANNELS].orEmpty().toggle(channelHandle) }
    }

    override suspend fun markWatched(videoId: String) {
        dataStore.edit { prefs -> prefs[Keys.WATCHED_VIDEO_IDS] = prefs[Keys.WATCHED_VIDEO_IDS].orEmpty() + videoId }
    }

    override suspend fun signIn(session: UserSession) {
        dataStore.edit { prefs ->
            prefs[Keys.SESSION_DISPLAY_NAME] = session.displayName
            prefs[Keys.SESSION_HANDLE] = session.handle
            prefs[Keys.SESSION_AVATAR_URL] = session.avatarUrl
            prefs[Keys.SESSION_IS_PREMIUM] = session.isPremium
            prefs[Keys.SESSION_METHOD] = session.method.name
            prefs[Keys.SESSION_SIGNED_IN_AT] = session.signedInAtEpochSeconds
        }
    }

    /** Clears the session only. Favourites, downloads and settings are the device's, not the account's. */
    override suspend fun signOut() {
        dataStore.edit { prefs ->
            prefs.remove(Keys.SESSION_DISPLAY_NAME)
            prefs.remove(Keys.SESSION_HANDLE)
            prefs.remove(Keys.SESSION_AVATAR_URL)
            prefs.remove(Keys.SESSION_IS_PREMIUM)
            prefs.remove(Keys.SESSION_METHOD)
            prefs.remove(Keys.SESSION_SIGNED_IN_AT)
        }
    }

    override suspend fun setPlaybackQuality(quality: PlaybackQuality) {
        dataStore.edit { it[Keys.PLAYBACK_QUALITY] = quality.name }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    /** A session exists only once a handle has been stored; every other field has a safe default. */
    private fun Preferences.toSession(): UserSession? {
        val handle = this[Keys.SESSION_HANDLE]?.takeIf { it.isNotBlank() } ?: return null
        return UserSession(
            displayName = this[Keys.SESSION_DISPLAY_NAME].orEmpty(),
            handle = handle,
            avatarUrl = this[Keys.SESSION_AVATAR_URL].orEmpty(),
            isPremium = this[Keys.SESSION_IS_PREMIUM] ?: false,
            method = this[Keys.SESSION_METHOD].toEnumOrNull<SignInMethod>() ?: SignInMethod.Email,
            signedInAtEpochSeconds = this[Keys.SESSION_SIGNED_IN_AT] ?: 0L,
        )
    }

    private object Keys {
        val FAVORITE_VIDEO_IDS = stringSetPreferencesKey("favorite_video_ids")
        val SAVED_VIDEO_IDS = stringSetPreferencesKey("saved_video_ids")
        val SUBSCRIBED_CHANNELS = stringSetPreferencesKey("subscribed_channels")
        val WATCHED_VIDEO_IDS = stringSetPreferencesKey("watched_video_ids")
        val SESSION_DISPLAY_NAME = stringPreferencesKey("session_display_name")
        val SESSION_HANDLE = stringPreferencesKey("session_handle")
        val SESSION_AVATAR_URL = stringPreferencesKey("session_avatar_url")
        val SESSION_IS_PREMIUM = booleanPreferencesKey("session_is_premium")
        val SESSION_METHOD = stringPreferencesKey("session_method")
        val SESSION_SIGNED_IN_AT = longPreferencesKey("session_signed_in_at")
        val PLAYBACK_QUALITY = stringPreferencesKey("playback_quality")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }
}

private fun Set<String>.toggle(value: String): Set<String> = if (value in this) this - value else this + value

private inline fun <reified T : Enum<T>> String?.toEnumOrNull(): T? =
    this?.let { name -> enumValues<T>().firstOrNull { it.name == name } }
