# Streamly - Agentic Coding & Architectural Rules

## Core Architecture & Technical Stack
- Architecture: Strict Clean Architecture with `:domain`, `:data`, and `:presentation` separation.
- Domain Layer: Pure Kotlin module with NO Android, Framework, Retrofit, or Ktor dependencies.
- Presentation Pattern: MVVM + MVI combined. Each screen ViewModel exposes a single immutable `StateFlow<UiState>` and consumes a sealed `UiEvent` intent.
- UI Framework: 100% Jetpack Compose. Strictly NO XML layout files.
- Adaptive UI: Support `WindowSizeClass` for foldables and tablets.
- Navigation: Use Navigation 3 (Nav3).
- Network Client: Use Ktor as the HTTP client.
- Dependency Injection: Hilt.
- Local Storage & Auth: Preference DataStore for auth session persistence.

## Media3 (ExoPlayer) Rules
- Standard Video Player: Single shared ExoPlayer instance managed via Hilt/Lifecycle. Reused across navigation and orientation changes without re-initialization.
- Shorts Feed: Full-screen vertical pager with player pooling/pager strategy. Maximum 1-2 active player instances at a time to prevent audio bleed and memory leaks.
- Downloads Module: Use Media3 `DownloadManager` for actual HLS video downloading, real-time progress tracking, and offline playback.
- Streaming Protocol: HLS (`.m3u8`) format with adaptive bitrate support.

## Code Quality & Conventions
- Always implement explicit UI states: `Loading`, `Success`, `Empty`, and `Error` (with retry intents).
- Write idiomatic Compose code with proper recomposition performance considerations.

## Agent Execution Log & Prompt History
Chronological log of the key agent prompt workflows. Append new entries at the bottom; keep each entry short and point to the files it touched.
Raw prompts (verbatim, timestamped) are captured automatically in `docs/prompt-history.md` by the `UserPromptSubmit` hook in `.claude/settings.json`.

| # | Workflow | Status | Commit |
|---|----------|--------|--------|
| 1 | Setup & Dependencies | Done | `d15dda8`, `bd8fd44` |
| 2 | Domain & Data Layer | Done | `4636bc9` |
| 3 | Media3 ExoPlayer & Home Screen MVI UI | Done | `6dd6fe4` |
| 4 | Player Screen & Navigation 3 Routing | Done | `4dd76bb` |
| 5 | Shorts Feed & Home/Shorts Tabs | Done | `6f6e98f` |
| 6 | Offline Downloads & DataStore Preferences | Done | `9c15b62` |
| 7 | Nordic Mint Theme & Tabbed Player | Done (uncommitted) | — |

### 1. Setup & Dependencies
- Prompt: Configure the version catalog and wire KSP, Hilt, Ktor, Media3, and Navigation 3.
- Output: `gradle/libs.versions.toml`, root + `app/build.gradle.kts`, `StreamlyApp` (`@HiltAndroidApp`), manifest registration, theme moved `ui/theme` → `core/theme`.

### 2. Domain & Data Layer
- Prompt: Implement video domain models, repository contract, and Ktor-backed data source.
- Domain (pure Kotlin): `domain/model/Video.kt`, `domain/repository/VideoRepository.kt`.
- Data: `data/remote/dto/VideoDto.kt`, `data/mapper/VideoMapper.kt`, `data/repository/VideoRepositoryImpl.kt`.
- DI: `core/network/NetworkModule.kt` (Ktor client), `core/di/RepositoryModule.kt`.

### 3. Media3 ExoPlayer & Home Screen MVI Compose UI
- Prompt: Provide a single shared ExoPlayer via Hilt and build the Home feed screen with MVI.
- Player: `core/di/MediaModule.kt` (singleton `ExoPlayer`, HLS-ready).
- Home MVI: `presentation/home/HomeUiState.kt` (Loading/Success/Empty/Error), `HomeUiEvent.kt` (intents + `NavigateToPlayer` effect), `HomeViewModel.kt` (`StateFlow<HomeUiState>`), `HomeScreen.kt` (`HomeRoute`).

### 4. Player Screen & Navigation 3 Routing
- Prompt: Add the Player screen bound to the shared ExoPlayer and replace the temporary root in `MainActivity` with a Nav3 back stack (Home → Player).
- Navigation: `presentation/navigation/StreamlyNavKeys.kt` (`HomeKey`, `PlayerKey(videoId)`), `StreamlyNavDisplay.kt` → renamed `NavGraph.kt` in step 5 (`NavDisplay` + saveable-state and per-entry ViewModel-store decorators); `MainActivity.kt` now hosts the nav graph with `WindowSizeClass`.
- Player MVI: `presentation/player/PlayerUiState.kt` (Loading/Success/Empty/Error), `PlayerUiEvent.kt` (`Retry`), `PlayerViewModel.kt` (Hilt assisted `videoId`, drives shared `ExoPlayer`, stops but never releases it), `PlayerScreen.kt` (`PlayerView` via `AndroidView`; fullscreen on compact height, side-by-side on expanded width).
- Build: `lifecycle-viewmodel-navigation3` added (`gradle/libs.versions.toml`, `app/build.gradle.kts`); player strings in `res/values/strings.xml`.

### 5. Shorts Feed & Home/Shorts Tabs
- Prompt: Build a TikTok/Reels-style vertical Shorts feed (`getShortsVideos()`, VerticalPager, metadata overlay, like/share, progress), with lag-free playback (pause off-screen, play active), plus Nav3 tab switching between Home and Shorts.
- Player pool: `core/media/ShortsPlayerPool.kt` — 2 ExoPlayers max, page `i` → slot `i % 2`; active plays, neighbour preloaded paused on first frame; released when the Shorts entry is popped. `core/media/VideoMediaItem.kt` (`Video.toMediaItem()`, HLS MIME tagging) now shared with `PlayerViewModel`.
- Shorts MVI: `presentation/shorts/ShortsUiState.kt` (Loading/Success/Empty/Error; `activeIndex`, `preloadIndex`, `isPaused`, `likedIds`), `ShortsUiEvent.kt` (page settled, toggle play, like, share, screen start/stop, retry + `Share` effect), `ShortsViewModel.kt`, `ShortsScreen.kt` (`VerticalPager`, switches playback on `settledPage`, tap-to-pause, buffering spinner + polled progress bar, share via `ACTION_SEND` chooser).
- Navigation: `presentation/navigation/NavGraph.kt` (`StreamlyNavGraph`; bottom bar on compact width, `NavigationRail` on medium/expanded, hidden on Player; back stack `[Home]` / `[Home, Shorts]`), `ShortsKey` in `StreamlyNavKeys.kt`; nav/shorts strings in `res/values/strings.xml`.
- Verified on device (SM-M015G, Android 10): 2 player inits, swipe plays preloaded clip, tap-pause works, both players released on switching back to Home.

### 6. Offline Downloads & DataStore Preferences
- Prompt: Media3 `DownloadManager` + service for offline HLS with state tracking (Downloading/Downloaded/Failed); DataStore for favourites, playback quality and theme; Compose Downloads screen with offline playback; Downloads tab in Nav3. 100% Compose, Clean Architecture.
- Domain: `domain/model/VideoDownload.kt` (`DownloadStatus` Queued/Downloading/Downloaded/Failed), `domain/model/UserPreferences.kt` (`PlaybackQuality`, `ThemeMode`), `domain/repository/DownloadRepository.kt`, `domain/repository/PreferencesRepository.kt`.
- Data: `data/local/datastore/UserPreferencesRepository.kt` (implements `PreferencesRepository`); `core/di/DataStoreModule.kt`; bindings in `core/di/RepositoryModule.kt`.
- Media: `core/media/DownloadTracker.kt` (implements `DownloadRepository`; `DownloadHelper` picks one rendition capped by the quality pref; video metadata stored in the request for offline use; polled progress), `core/media/MediaDownloadService.kt` (Hilt `DownloadService`, progress notification, `PlatformScheduler`), `core/di/DownloadModule.kt` (DB provider, non-evicting `SimpleCache`, `DownloadManager`); `core/di/MediaModule.kt` now plays through a read-only `CacheDataSource`, so downloads play offline in every player.
- Presentation: `presentation/downloads/` (`DownloadsUiState`, `DownloadsUiEvent`, `DownloadsViewModel`, `DownloadsScreen` — adaptive grid, progress, retry, remove); `presentation/settings/` (`SettingsViewModel`, `SettingsSheet` opened from Home); Player gets a download button, offline metadata fallback, downloaded stream keys and the quality cap; Shorts likes persist as favourites; `MainViewModel` + `MainActivity` apply the theme (system-bar icons follow it); `core/theme/StreamlyIcons.kt` (download icon); `DownloadsKey` + tab in `NavGraph.kt`.
- Manifest/resources: `FOREGROUND_SERVICE(_DATA_SYNC)`, `POST_NOTIFICATIONS` (requested on first download), `RECEIVE_BOOT_COMPLETED`; `MediaDownloadService` (`dataSync`) and `PlatformSchedulerService`; download/settings strings.
- Verified on device (SM-M015G, Android 10): theme + quality persist across reinstall; 480p download of a 6-min HLS stream (46 MB) with live progress and notification; cold start with mobile data off plays it from the Downloads tab.

### 7. Nordic Mint / Sage Green Theme & Tabbed Player UI
- Prompt: Apply the finalized Nordic Mint / Sage Green aesthetic (`#121820` ground, `#2EC4B6` mint accent) and a tabbed player UI across the presentation layer, `Color.kt` and `Theme.kt`.
- Theme: `core/theme/Color.kt` replaced the template purple with the brand palette — mint primary, sage green secondary, frost blue tertiary, full light/dark token sets, plus a `StreamlyMediaColors` object for colours that sit on video (letterbox, scrim, on-media text, like tint) and therefore must not follow the scheme. `core/theme/Theme.kt` wires both schemes and **drops dynamic colour**: a wallpaper-derived scheme would break the brand and the contrast of overlays drawn over video.
- Tabbed player: `presentation/player/PlayerScreen.kt` now shows `PrimaryTabRow` with Details / Up Next under the video (beside it on expanded widths; untouched in landscape fullscreen). `PlayerTab` enum in `PlayerUiState.kt`; `PlayerUiState.Success.relatedVideos`; `PlayerUiEvent.OnRelatedVideoClick` + `PlayerUiEffect.NavigateToVideo`.
- Up Next: `PlayerViewModel.loadRelatedVideos()` reuses `getHomeVideos()`, same-category first, capped at 10, failing silently to an empty tab. `NavGraph.kt` replaces the Player entry rather than stacking one, so back still returns to the originating tab.
- Shared: `presentation/common/VideoFormat.kt` holds `formatDuration()`, previously private to `HomeScreen`. Home and Shorts overlays switched from `Color.Black`/`Color.White` to `StreamlyMediaColors`. New strings: `player_tab_details`, `player_tab_up_next`, `player_up_next_empty`.
- Verified: `./gradlew assembleDebug` BUILD SUCCESSFUL, zero errors (only the pre-existing `@StringRes` KT-73255 warnings). Not yet run on device.
