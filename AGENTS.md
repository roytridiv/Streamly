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
| 7 | Nordic Mint Theme & Tabbed Player | Superseded by 8 | — |
| 8 | Nordic Mint UI Refresh (design handoff) | Done (uncommitted) | — |
| 9 | Profile & Sign-In screen | Done (uncommitted) | — |
| 10 | Onboarding start destination & session flow | Done (uncommitted) | — |
| 11 | Bottom nav on the Player & mini-player handover | Done (uncommitted) | — |

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

### 8. Nordic Mint UI Refresh (design handoff)
- Prompt: Read `F:\design_handoff_streamly_nordic_mint\README.md` and the `Streamly App v2 -Bolt-.dc.html` prototype; implement the Nordic Mint theme, splash, Home, Player, Shorts, Downloads and bottom nav; install the v2-bolt logo resources. Supersedes workflow 7, whose theme and two-tab Player this replaces.
- **Deviation from the handoff, on request:** the handoff's hard rule is "UI layer only — no ViewModels, MVI contracts, DataStore, repositories or Media3 setup". Several designed elements had no data behind them (channel, views, upload age, subscribers, like counts, chapters, saved/followed state, device storage, downloaded resolution). Asked, and was told to lift the rule and extend the data layer too. The Media3 *setup* is still untouched: the shared `ExoPlayer`, `ShortsPlayerPool`, `DownloadManager`, cache and source factories are unchanged; the UI only calls `play`/`pause`/`seekTo` on handles it is already given.

**Theme & brand**
- `core/theme/Color.kt` — handoff tokens (`SlateCharcoal`, `SoftCharcoal`, `SurfaceRaised`, `SageMint`, `MintText`, `MintTint`, `OnMint`, text ramp, `StorageOther`) plus `StreamlyBrand` for tokens with no Material slot and the fixed on-video colours. Replaces the old `StreamlyMediaColors`.
- `core/theme/Shape.kt` (new) — `Card` 16, `Viewport` 12, `SmallThumbnail` 12, `ActionButton` 14, `DurationBadge` 7, `IconButton` 12, `FrostedTile` 18, and `asymmetricPill(height)` scaling the 17/6 ratio.
- `core/theme/Type.kt` — handoff scale (24/19/15/13.5/12.5/11), Medium headings, SemiBold badges only; `StreamlyType` for mono timestamps/durations/eyebrows and the wordmark. Resolves to the platform sans/mono — drop Inter and JetBrains Mono into `res/font` and repoint `InterFamily`/`MonoFamily` for an exact match.
- `core/theme/Theme.kt` — dark `ColorScheme` wired to the tokens (`MintTint`/`MintText` → `primaryContainer`/`onPrimaryContainer`, `SoftCharcoal` → `surfaceContainer`); dynamic colour off; a light scheme kept so the DataStore `ThemeMode` preference still works.
- `core/theme/StreamlyLogo.kt` (new) — the mark drawn in Compose via `PathParser` from the same path data as the drawable, because the splash animates the play mark and the bolt separately. `StreamlyLogoTile` (with `outlined` for the Downloads empty state) and `StreamlyBoltGlyph`.
- `core/theme/StreamlyIcons.kt` — the handoff's Phosphor set built from the prototype's path data, avoiding material-icons-extended.
- Assets: `res/drawable/ic_streamly_logo.xml`, `ic_launcher_foreground.xml`, `ic_launcher_monochrome.xml`, `mipmap-anydpi-v26/ic_launcher(_round).xml`, `values/colors.xml` (`ic_launcher_background` #1E2630), `values/themes.xml` (`Theme.Streamly.Starting` on the SplashScreen API), `core-splashscreen:1.0.1`. The green template `drawable/ic_launcher_background.xml` was deleted.

**Data layer (the lifted rule)**
- `domain/model/Video.kt` — added `Channel`, `VideoStats`, `Chapter`, `soundLabel`, `tags`, all defaulted.
- `domain/model/VideoDownload.kt` — added `videoHeight`. `domain/model/UserPreferences.kt` + `PreferencesRepository` + `data/local/datastore/UserPreferencesRepository.kt` — added `savedVideoIds`, `subscribedChannels`, `toggleSaved`, `toggleSubscription`.
- `data/remote/dto/VideoDto.kt`, `data/mapper/VideoMapper.kt` — `ChannelDto`, `ChapterDto` and the new fields; chapters sorted ascending on the way in.
- `data/repository/VideoRepositoryImpl.kt` — the fallback catalogue now carries channels, stats and chapters.
- `core/media/DownloadTracker.kt` — `DownloadMetadata` persists channel/stats/chapters/tags so downloads keep their metadata offline, plus `selectedVideoHeight()` reading the rendition actually written to the cache.
- `core/media/NowPlayingStore.kt` (new) — mirrors the shared player for the mini-player; polls position at 2Hz only while playing. `PlayerViewModel` registers each video with it.
- `core/storage/DeviceStorage.kt` (new) — real `StatFs` figures for the Downloads usage card, instead of the prototype's mocked 64 GB.

**Shared UI**
- `presentation/common/VideoFormat.kt` — `formatDuration`, `formatCount` (1.2M), `formatRelativeAge` (3 days ago), `formatBytes`.
- `presentation/common/PlaybackProgress.kt` (new) — `rememberPlaybackProgress(player)`, the one place that polls position; ticks only while playing.
- `presentation/components/StreamlyToast.kt` (new) — the handoff's SurfaceRaised pill with a mint edge, 1.8s.

**Screens**
- `presentation/splash/StreamlySplash.kt` (new) — radial glow, tile 0.82→1 (700ms, `cubic-bezier(.2,.8,.2,1)`), bolt strike from (5,−14) at 500ms, flash ring at 620ms, wordmark rise at 900ms, exit 2.1s. `MainActivity` calls `installSplashScreen()` so the system icon and this are one shot; `rememberSaveable` stops a rotation replaying it.
- `presentation/navigation/components/StreamlyBottomBar.kt` (new) — 66dp bar, asymmetric mint pill, bolt glyph for Shorts, mint download badge; `StreamlyNavRail` keeps medium/expanded widths working. `MiniPlayer.kt` (new) — 58dp, mint edge, 2dp progress line. `NavGraph.kt` rewired; `MainViewModel` now also owns `nowPlaying` and `activeDownloadCount` (chrome, not screen content).
- `presentation/home/components/` (new) — `HomeTopBar.kt` (30dp tile + wordmark; the handoff's Search/Bell slots carry Refresh and Settings, the actions Streamly actually has), `CategoryChips.kt`, `VideoCard.kt` (mint duration badge, frosted "Offline" tag, avatar, meta line). `HomeScreen.kt` rebuilt; `HomeUiState` gained `downloadedIds`.
- `presentation/player/components/` (new) — `FloatingPlayerViewport.kt` (12dp pane, mint glow, watermark, 54dp frosted play, 3dp scrubber with chapter ticks and the "0:42 · Blue hour" label), `PlayerActionBar.kt` (Like/Share/Download, the download button's three live states with a progress ring and left-to-right MintTint fill), `PlayerDetailTabs.kt` (sliding indicator, Overview with Subscribe and the expandable description, Key Moments chips, Up Next list ↔ carousel). `PlayerScreen.kt` rebuilt; `PlayerUiState`/`Event` gained liked, subscribed, share and the `PlayerTab`/`UpNextLayout` enums. Seeking and play/pause go straight to the `Player` handle the screen already receives — position is not pumped through state.
- `presentation/shorts/components/ShortsOverlays.kt` (new) — glowing bolt header with "1 / 4", 50dp frosted right rail, bottom-left overlay with Follow and sound row, 2dp mint progress bar. `ShortsScreen.kt` rebuilt; `ShortsUiState`/`Event` gained `savedIds`, `followedChannels`, `OnSaveClick`, `OnFollowClick`.
- `presentation/downloads/components/` (new) — `StorageUsageCard.kt` (segmented bar with a mint glow, legend, plus `FadedDivider`), `DownloadRow.kt` (118dp thumbnail, Offline Ready tag, pulsing % over a 3dp bar, "612 MB · 1080p"). `DownloadsScreen.kt` rebuilt with the outlined-logo empty state; `DownloadsUiState` gained `storage`, `readyCount`, `savingCount`.

**Known substitutions** (where the prototype cannot be matched literally on Android)
- Frosted surfaces are translucent fills, not backdrop blurs: `Modifier.blur` is API 31+ and does not sample what is behind it. The handoff's own opacity values are used.
- `material3` 1.3.0's `IconButton` is circle-only (`shape` lands in 1.4), so 12dp-radius icon buttons are clipped clickable boxes.
- The Shorts rail's Comments entry shows the count but is not clickable — Streamly has no comments screen, so it is a tile rather than a dead button. The Player header's overflow slot is likewise held empty.
- Verified: `./gradlew clean assembleDebug` BUILD SUCCESSFUL, zero errors. The only warnings are the pre-existing KT-73255 annotation-target class. **Not yet run on device.**
- Follow-up fix: orientation lock on fullscreen exit. The toggle gated `requestedOrientation` on `isCompactWidth` in both directions, but a phone in landscape is roughly 800x360dp and so reads as Medium width, not Compact: the lock applied on the way in and the unlock was unreachable on the way out, stranding the whole app in landscape. `PlayerScreen.kt` now saves whether it locked (`lockedLandscape`) instead of re-deriving it, and a single `FullscreenWindowEffect` owns both the system bars and the orientation: set on enter, cleared on exit, and cleared again in `onDispose` so leaving the Player by any route (button, rotation, back, Up Next swap, pop) cannot leave the app locked or immersive. `ImmersiveMode` and the `requestLandscape` helper are folded into it; nothing else touches `requestedOrientation`.
- Follow-up fix: a fullscreen toggle. `StreamlyIcons` gained `Fullscreen`/`FullscreenExit`; the button sits at the end of the scrubber label row in `FloatingPlayerViewport.kt`, so it fades with the rest of the overlay. The viewport takes an `isFullscreen` flag that drops the floating treatment (rounded clip, mint border, ambient glow) for a pane that fills the window, and `PlayerScreen.kt` now owns `isFullscreen` as saveable state: the old `FullscreenSurface` with Media3 chrome is gone, so the overlay and its auto-hide behave identically in both modes. Rotating a phone into landscape still enters fullscreen -- the button and the device drive the same state. Entering on a compact width also requests `SCREEN_ORIENTATION_USER_LANDSCAPE` (a 16:9 video in portrait is mostly letterbox); exiting hands orientation back with `UNSPECIFIED`. `BackHandler` leaves fullscreen before leaving the Player.
- Follow-up fix: the overlay controls were composed unconditionally, so they never left the screen once playback began. `presentation/player/components/PlayerControlsState.kt` (new) holds their visibility plus an `interactionCount` that restarts a 3s countdown; `FloatingPlayerViewport.kt` wraps the centre button and scrubber in `AnimatedVisibility` (removed from composition, not faded, so a hidden overlay cannot swallow taps), toggles on a tap anywhere on the surface, and keeps them up while paused. The state is hoisted into `PlayerScreen.kt` so a Key Moments seek resets the countdown too.

### 9. Profile & Sign-In screen
- Prompt: A modern Profile & Sign-In screen in the Nordic Mint aesthetic, with signed-out and signed-in states backed by DataStore and an instant mock login.
- Domain: `domain/model/UserSession.kt` (new) — `UserSession` (display name, handle, avatar, premium flag, `SignInMethod`, signed-in timestamp) and `UserSession.mock()`, the placeholder account the buttons create until there is a real identity provider. `UserPreferences` gained `session` and `watchedVideoIds`; `PreferencesRepository` gained `signIn`, `signOut` and `markWatched`.
- Data: `UserPreferencesRepository` persists the session across six keys and reads it back through a private `Preferences.toSession()`; a session exists only once a handle is stored. `signOut()` clears the session **only** — favourites, downloads and settings belong to the device, not the account.
- Stats come from data the app already keeps rather than invented numbers: Watched (`watchedVideoIds`, which `PlayerViewModel.startPlayback` adds to), Offline (completed downloads) and Favourites.
- `core/storage/ImageCacheCleaner.kt` (new) — measures and clears Coil's thumbnail cache for "Clear image cache". Scoped to images on purpose: the other cache is the Media3 download cache, and clearing that would delete the viewer's offline videos behind an innocuous label.
- Presentation: `presentation/profile/` — `ProfileUiState.kt` (Loading / SignedOut / SignedIn, `WatchStats`, events, effects), `ProfileViewModel.kt` (state derived by combining the preferences and downloads flows, so signing in or finishing a download updates the screen without it knowing about either), `ProfileScreen.kt`, and `components/SignInCard.kt`, `ProfileHeader.kt` (header, avatar, Premium badge, stats row), `ProfileSettingsList.kt` (theme chips, Downloads, Clear cache, Sign out).
- Navigation: `ProfileKey` in `StreamlyNavKeys.kt`, an entry in `NavGraph.kt`, and the Home top bar's second action is now Profile instead of Settings — Profile hosts the theme toggle and links to the existing sheet for playback quality, so no control has two homes. The bottom nav keeps the handoff's three tabs.
- Icons added to `StreamlyIcons`: `User`, `Mail`, `ChevronRight`, `SignOut`, `Spark`, `CloudOff`, `Shield`.
- Notes: the Google button shows a neutral monogram, **not** Google's logo — the real mark has its own brand guidelines and hand-approximating it would ship something that looks official but is not; the official drawable from the Sign-In SDK replaces it when auth is wired up. The signed-in state carries a "MOCK SESSION" footer so a demo build is never mistaken for a real account. `ImageCacheCleaner` opts in to `ExperimentalCoilApi` explicitly.
- Verified: `./gradlew clean assembleDebug` BUILD SUCCESSFUL, zero errors; only the pre-existing KT-73255 annotation-target warnings remain. **Not yet run on device.**

### 10. Onboarding start destination & session flow
- Prompt: Onboarding/Auth as the start destination when DataStore holds no session (Google, Email, Continue as Guest); returning users skip straight to Home; Profile's Sign Out confirms, clears the session and returns to Onboarding with the back stack cleared.
- Domain: `SignInMethod.Guest` added, and `UserSession.mock()` became `UserSession.forMethod()` — Guest is a real session with no identity (no name, avatar or Premium), which is what stops onboarding reappearing for a returning guest. `UserSession.isGuest` for the UI.
- `MainViewModel.hasSession: StateFlow<Boolean?>` — `null` while DataStore is still being read.
- `presentation/onboarding/` (new) — `OnboardingScreen.kt` + `OnboardingViewModel.kt`. Reuses Profile's `SignInCard` rather than a second copy of the same pitch; the card gained an optional `onContinueAsGuest`, rendered only where skipping is a real option (inside Profile there is nothing to skip past). All three buttons go through one `start(method)` that writes the session, then emits `NavigateToHome`.
- `NavGraph.kt` — the start destination is now chosen from the session: while `hasSession` is `null` the graph renders a bare background and waits, because rooting at Home and then yanking a returning user to onboarding (or the reverse) is visible and jarring; the splash is still on top, so nothing blank is ever seen. `rememberNavBackStack(if (sessionExists) HomeKey else OnboardingKey)` is evaluated once, and later sign-in/sign-out move the stack explicitly rather than re-rooting it under whatever the user is looking at. New `resetTo(key)` helper pushes first and trims after, so the stack is never momentarily empty — NavDisplay has nothing to render in that state. `OnboardingKey` in `StreamlyNavKeys.kt`.
- Back behaviour falls out of the single-entry stack: back on Onboarding exits the app rather than reaching Home, and back on Home after signing in exits rather than returning to Onboarding.
- Profile: `OnSignOutClick` now goes through an `AlertDialog` ("Sign out?"), and `ProfileUiEffect.NavigateToOnboarding` drives `backStack.resetTo(OnboardingKey)`. `ProfileViewModel.signingOut` holds the last signed-in frame while the session clears — without it the preferences flow emits a sessionless state and the screen crossfades to its own sign-in card for a frame on the way to onboarding.
- Verified: `./gradlew clean assembleDebug` BUILD SUCCESSFUL, zero errors; only the pre-existing KT-73255 annotation-target warnings. **Not yet run on device.**

### 11. Bottom nav on the Player & mini-player handover
- Prompt: Keep the BottomNavigationBar visible on the Player so tabs can be switched directly while a video plays.
- `NavGraph.kt` — the bar's visibility and the highlighted tab are now separate questions. `showNavBar` is true on top-level tabs and on the Player (minus fullscreen), and `activeTab` is found by scanning *down* the stack: the Player sits on top of whichever tab opened it, so `[Home, Player]` highlights Home instead of leaving every item unselected. `selectTab` already trimmed to the root before pushing, so switching tabs straight from the Player needed no change.
- Fullscreen: `PlayerRoute`/`PlayerScreen` report `onFullscreenChange`, since only the nav graph can hide the bar, and a `DisposableEffect` reports `false` on the way out so leaving the Player mid-fullscreen never leaves the bar hidden on the tab underneath.
- Insets: the Box holding `NavDisplay` now consumes the navigation-bar insets whenever the bottom bar is shown, because the bar already sits over them — screens above it (Player, Shorts) were padding for the same insets a second time.
- `showMiniPlayer` is keyed on the top of the stack rather than the active tab, so the mini-player does not dock over the Player it mirrors.
- **Playback now survives leaving the Player.** `PlayerViewModel.onCleared()` no longer stops and clears the shared player; the mini-player picks the same video up mid-stream. Found while tracing this change: the mini-player built in workflow 8 could never appear at all, because reaching Home or Downloads popped the Player, which stopped the playback the mini-player was meant to continue. A visible bar that silently killed playback when used would have been the same bug wearing a hat.
- Audio bleed: `NowPlayingStore.pause()` (new) is called from `ShortsViewModel` on `OnScreenStart`, so a video left running by the mini-player is paused before the Shorts pool starts — AGENTS.md's "1-2 active players" rule still holds. The shared player is still never released, the mini-player can pause it, and the process-lifecycle observer pauses it on background. `NowPlayingStore.onPlaybackStopped` was removed as dead code.
- Verified: `./gradlew clean assembleDebug` BUILD SUCCESSFUL, zero errors; only the pre-existing KT-73255 annotation-target warnings. **Not yet run on device.**
