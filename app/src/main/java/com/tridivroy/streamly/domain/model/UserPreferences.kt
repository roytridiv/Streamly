package com.tridivroy.streamly.domain.model

data class UserPreferences(
    val favoriteVideoIds: Set<String> = emptySet(),
    /** Videos saved from the Shorts rail. Separate from favourites, which Like drives. */
    val savedVideoIds: Set<String> = emptySet(),
    /** Channel handles the user subscribes to -- drives Subscribe and Follow. */
    val subscribedChannels: Set<String> = emptySet(),
    /** IDs of videos playback has started for, behind the Profile screen's "watched" stat. */
    val watchedVideoIds: Set<String> = emptySet(),
    /** The signed-in account, or `null` when signed out. */
    val session: UserSession? = null,
    val playbackQuality: PlaybackQuality = PlaybackQuality.Auto,
    /**
     * Defaults to [ThemeMode.Dark], not System.
     *
     * Nordic Mint is a dark-only design; the light scheme exists so an explicit Light choice works.
     * Defaulting to System meant every device without a system dark mode -- anything pre-Android 10,
     * or just a phone in light mode -- opened the app in the light variant and never showed the brand.
     */
    val themeMode: ThemeMode = ThemeMode.Dark,
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
