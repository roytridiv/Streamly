package com.tridivroy.streamly.presentation.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Back stack destinations. Serializable so the back stack survives process death and rotation. */
@Serializable
data object HomeKey : NavKey

@Serializable
data class PlayerKey(val videoId: String) : NavKey

@Serializable
data object ShortsKey : NavKey

@Serializable
data object DownloadsKey : NavKey

@Serializable
data object ProfileKey : NavKey

/** Start destination while DataStore holds no session — account or guest. */
@Serializable
data object OnboardingKey : NavKey
