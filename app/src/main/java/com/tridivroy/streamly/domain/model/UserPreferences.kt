package com.tridivroy.streamly.domain.model

data class UserPreferences(
    val favoriteVideoIds: Set<String> = emptySet(),
    val playbackQuality: PlaybackQuality = PlaybackQuality.Auto,
    val themeMode: ThemeMode = ThemeMode.System,
)

/** Caps streaming and download resolution. [maxVideoHeight] `null` means no cap (adaptive). */
enum class PlaybackQuality(val maxVideoHeight: Int?) {
    Auto(null),
    High(1080),
    Medium(720),
    Low(480),
}

enum class ThemeMode {
    System,
    Light,
    Dark,
}
