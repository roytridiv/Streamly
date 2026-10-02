# Streamly — Project Overview & Architecture Guide

Streamly is a 100% Jetpack Compose video app: an Onboarding/Auth gate, a Home feed, a tabbed Player, a TikTok/Reels-style Shorts feed, offline HLS downloads, and a Profile screen — all in the Nordic Mint design. This guide explains how it is put together, why the key decisions were made, and where to find things.

> **Status** — Workflows 1–12 in [`AGENTS.md`](../AGENTS.md) are implemented; 7–12 are committed-pending. Since workflow 8 the UI follows the Nordic Mint design handoff, and the app opens on an Onboarding/Auth screen. Smoke-tested on a physical device (moto e5 plus, Android 8.0 / API 26) in workflow 12. The backend base URL is still a placeholder (`BuildConfig.BASE_URL`), so the app runs on built-in public HLS test streams (see [Q8](#q8-what-happens-when-the-api-is-unreachable)).

---

## Contents

1. [Executive Architecture Overview](#1-executive-architecture-overview)
2. [Core Technical Decisions](#2-core-technical-decisions)
3. [Key Files & Responsibilities](#3-key-files--responsibilities)
4. [Technical Walkthrough & Q&A](#4-technical-walkthrough--qa)

---

## 1. Executive Architecture Overview

### Layers

Streamly follows Clean Architecture with four layers. **Today they are package boundaries inside the single `:app` Gradle module** (`com.tridivroy.streamly.{core,data,domain,presentation}`), not separate Gradle modules. The dependency rules are kept by convention and code review; see [Q1](#q1-are-core-data-domain-and-presentation-separate-gradle-modules) for what splitting into modules would add.

| Layer | Responsibility | Depends on | Must not depend on |
|---|---|---|---|
| `domain` | Models (`Video` + `Channel`/`VideoStats`/`Chapter`, `VideoDownload`, `UserPreferences`, `UserSession`) and repository interfaces | Kotlin stdlib, `kotlinx.coroutines` | Android, Ktor, Media3, DataStore |
| `data` | Repository implementations: Ktor API + DTO mapping, DataStore | `domain` | `presentation` |
| `core` | Cross-cutting infrastructure: Hilt modules, Ktor client, Media3 players, download manager, theme | `domain` | `presentation` |
| `presentation` | Compose screens, ViewModels (MVI), Navigation 3 graph | `domain`, and `core` for players and theme | `data` |

`core/media/DownloadTracker` implements a domain interface (`DownloadRepository`), the same way `data` does. It lives in `core` because it wraps Media3 infrastructure rather than an API or database.

### Data flow: network → Compose UI

```
Ktor HttpClient (core/network/NetworkModule)
   │  GET /videos → List<VideoDto>          (kotlinx.serialization)
   ▼
VideoRepositoryImpl (data)                   DTO → domain via VideoMapper
   │  Result<List<Video>>                    falls back to bundled HLS test streams on failure
   ▼
VideoRepository (domain interface)  ◄── injected by Hilt (RepositoryModule @Binds)
   ▼
HomeViewModel (presentation)                 Result → HomeUiState (Loading / Success / Error)
   │  StateFlow<HomeUiState>                 + Channel<HomeUiEffect> for one-off navigation
   ▼
HomeRoute  → collectAsStateWithLifecycle()
   ▼
HomeScreen (stateless)  ── user intent ──► onEvent(HomeUiEvent) ──► HomeViewModel
```

Every screen follows the same loop. **State flows down** as one immutable `StateFlow<UiState>`, and **intents flow up** as a sealed `UiEvent`. One-off actions such as navigation or a share sheet go through a separate `Channel` of `UiEffect`, so they are never replayed after rotation.

---

## 2. Core Technical Decisions

### 2.1 Media3 ExoPlayer: shared singleton vs. dual-player pool

Streamly uses **two different player strategies** for two different workloads.

| | Standard Player | Shorts feed |
|---|---|---|
| Where | `core/di/MediaModule.provideExoPlayer` | `core/media/ShortsPlayerPool` |
| Instances | 1, app-wide `@Singleton` | 2, owned by `ShortsViewModel` |
| Lifetime | The whole process; never released by screens | Created with the Shorts entry; `release()` when it leaves the back stack |
| Why | Reused across navigation and rotation with no re-init cost (an `AGENTS.md` rule) | Instant swipes need the *next* video ready while the current one plays |

**Shared singleton (Player screen).**
- `PlayerViewModel` swaps media items on the injected `ExoPlayer` and only calls `stop()` / `clearMediaItems()` in `onCleared()`. It never calls `release()`.
- It only stops the player if the current item is still its own, in case another screen has already taken the player over.
- A process-level `ProcessLifecycleOwner` observer pauses playback when the whole app goes to the background.
- Rotation: the ViewModel survives and `PlayerView` is just re-attached (`AndroidView` `update` / `onRelease`), so playback keeps going without re-preparing.

**Dual-player pool (Shorts): how lag-free pre-buffering works.**

```
page index:   0      1      2      3      4
slot (i%2):   A      B      A      B      A

User on page 1 → slot B plays page 1, slot A preloads page 2 (prepared, paused at frame 0)
Swipe to 2     → slot A already holds page 2 → just play()  (no network wait)
                 slot B is re-pointed to page 3 and starts buffering in the background
```

- Page `i` always plays on player `i % 2`, so the next page is already **prepared and paused on its first frame** when the swipe lands.
- On the last page, the pool preloads the previous page instead.
- Playback only switches when `pagerState.settledPage` changes, not on every drag frame, so the players never thrash mid-gesture.
- Only the active and preloaded pages get a `PlayerView`. Every other page shows its thumbnail, which keeps surfaces and decoders to a minimum.
- The `AGENTS.md` limit of "max 1–2 active players" holds, and only one player is ever audible.
- Trade-off: swiping *backwards* past the preloaded page has to load the previous clip, so it is slower than swiping forward.

### 2.2 Offline download architecture

```
PlayerScreen "Download"  ─► PlayerViewModel ─► DownloadRepository.download(video)
                                                    │  (impl: core/media/DownloadTracker)
                                                    ▼
                           DownloadHelper.prepare()   reads the HLS master playlist,
                           with quality cap from      picks ONE rendition ≤ the user's quality
                           DataStore                  (forceHighestSupportedBitrate)
                                                    ▼
                           DownloadRequest(id = video.id, streamKeys, data = video metadata JSON)
                                                    ▼
                           DownloadService.sendAddDownload ─► MediaDownloadService (foreground, dataSync)
                                                    ▼
                           DownloadManager ─► segments written to SimpleCache (filesDir/downloads, never evicted)
                                                    │
                           DownloadManager.Listener + 1 s progress poll
                                                    ▼
                           DownloadTracker.downloads : Flow<List<VideoDownload>> ─► Downloads / Player UI
```

| Component | Responsibility |
|---|---|
| `DownloadModule` | Singletons: `StandaloneDatabaseProvider` (download index), `SimpleCache` with `NoOpCacheEvictor`, `DownloadManager` (4 segment threads, 2 parallel downloads) |
| `MediaDownloadService` | Hilt-injected `DownloadService`: keeps downloads running in the background, shows the progress notification, and uses `PlatformScheduler` to resume when the network returns or after a reboot |
| `DownloadTracker` | Implements `DownloadRepository`: starts and removes downloads, turns Media3 `Download` objects into immutable `VideoDownload` snapshots, and polls progress because `DownloadManager` has no progress callback |

**How offline playback works.**

1. Every player builds media sources from `MediaModule.provideMediaSourceFactory`. That factory reads through a **read-only `CacheDataSource`** over the download cache, so any byte that has been downloaded is served locally first.
2. For HLS, the player must also be told *which* rendition was downloaded, or adaptive streaming will ask for un-cached variants. `PlayerViewModel` therefore plays `DownloadTracker.downloadedMediaItem(id)`, which is `request.toMediaItem()` and carries the download's `streamKeys`.
3. Without network, `VideoRepository.getVideoById` fails. `PlayerViewModel` then falls back to `DownloadRepository.getDownloadedVideo(id)`, which decodes the metadata stored inside the download request. So the title and details still show offline.

**States.** Media3 states map to domain `DownloadStatus`:
- `QUEUED` / `STOPPED` / `RESTARTING` → `Queued`
- `DOWNLOADING` → `Downloading`
- `COMPLETED` → `Downloaded`
- `FAILED` → `Failed`
- `REMOVING` is hidden.

### 2.3 Navigation 3

- **Destinations are keys.** `presentation/navigation/StreamlyNavKeys.kt` defines `@Serializable` `NavKey`s: `OnboardingKey`, `HomeKey`, `ShortsKey`, `DownloadsKey`, `ProfileKey`, and `PlayerKey(videoId)`. Arguments are plain constructor fields, so there are no string routes or argument bundles.
- **The back stack is a list you own.** `StreamlyNavGraph` (in `NavGraph.kt`; called `StreamlyNavDisplay` before Step 5) creates it with `rememberNavBackStack(...)`, which is saveable and survives rotation and process death. Navigating is `backStack.add(PlayerKey(id))`, and going back is `removeLastOrNull()`.
- **The start destination depends on the session.** `MainViewModel.hasSession` is `Boolean?`; while it is `null` the graph renders a bare background and waits rather than guessing, because rooting at Home and then yanking a returning user to onboarding (or the reverse) is visible. The root is then `HomeKey` or `OnboardingKey`, evaluated once. Signing in and signing out move the stack explicitly with `resetTo(key)`, which pushes before trimming so the stack is never momentarily empty.
- **Tabs** sit on top of the Home root: `[Home]`, `[Home, Shorts]`, `[Home, Downloads]`. Selecting a tab pops back to Home and then pushes the tab, so system back always returns to Home. Leaving Shorts pops its entry, which frees its player pool.
- **Adaptive chrome.** A custom `StreamlyBottomBar` on compact widths and a `StreamlyNavRail` on medium and expanded widths. The bar is shown on the three tabs **and on the Player**, so a tab is always one tap away while something plays; it is hidden for fullscreen playback, Onboarding and Profile. Which tab is highlighted is found by scanning *down* the stack, since the Player sits on top of whichever tab opened it. The box holding `NavDisplay` consumes the navigation-bar insets whenever the bar is shown, so screens above it do not pad for them twice.
- **Mini-player.** While the shared player holds a video, a 58dp mini-player docks above the bar on Home and Downloads (keyed on the top of the stack, so it never covers the Player itself). `PlayerViewModel.onCleared()` deliberately leaves playback running so this handover works; `NowPlayingStore.pause()` is called when the Shorts pool takes over, to keep the "1–2 active players" rule.
- **ViewModel scoping** uses two entry decorators on `NavDisplay`:
  - `rememberSaveableStateHolderNavEntryDecorator()`: per-entry `rememberSaveable` state.
  - `rememberViewModelStoreNavEntryDecorator()` (from `lifecycle-viewmodel-navigation3`): each entry gets its own `ViewModelStore`. `hiltViewModel()` is then scoped to that entry, cleared when it is popped, and kept across rotation.

  Without the second decorator, every `hiltViewModel()` would share the Activity's store, and two different videos would get the same `PlayerViewModel`.
- **Per-entry arguments** reach the ViewModel through Hilt assisted injection: `hiltViewModel<PlayerViewModel, PlayerViewModel.Factory>(creationCallback = { it.create(videoId) })`.

### 2.4 MVI state management & DataStore preferences

**MVI contract (each screen):**

| Piece | Type | Example |
|---|---|---|
| State | `sealed interface XUiState`: `Loading`, `Success(...)`, `Empty`, `Error(message)` | `ShortsUiState.Success(videos, activeIndex, preloadIndex, isPaused, likedIds)` |
| Intent | `sealed interface XUiEvent` | `ShortsUiEvent.OnPageSettled(index)`, `Retry` |
| Effect | `sealed interface XUiEffect` via `Channel` → `receiveAsFlow()` | `ShortsUiEffect.Share(video)` |
| Wiring | `XRoute` (stateful: collects state and effects) → `XScreen` (stateless, previewable) | `HomeRoute` / `HomeScreen` |

Conventions:
- The ViewModel exposes exactly one `StateFlow`, and the UI calls a single `onEvent`.
- Effects are collected under `repeatOnLifecycle(STARTED)`, so they are never delivered to a stopped UI.
- `Error` states carry a retry intent.
- Exceptions:
  - Home shows `Empty` inside `Success` when a category filter has no videos.
  - Settings only has `Loading` and `Success`, because the preferences flow can't fail or be empty.

**DataStore preferences.**
- `data/local/datastore/UserPreferencesRepository` implements the domain `PreferencesRepository`. It uses a single Preferences DataStore instance (`DataStoreModule`; DataStore allows only one instance per file).
- It stores:
  - `favorite_video_ids`: a string set. Shorts and Player likes are persisted here.
  - `saved_video_ids`: the Shorts rail's Save action, kept separate from Like.
  - `subscribed_channels`: channel handles, behind both Subscribe (Player) and Follow (Shorts).
  - `watched_video_ids`: added to when playback starts, behind the Profile screen's "Watched" stat.
  - `session_*` (six keys): the signed-in account. A session exists only once a handle is stored.
  - `playback_quality`: an enum name. It caps the Player's track selection and the download rendition.
  - `theme_mode`: an enum name. `MainViewModel` applies it to `StreamlyTheme` and to the system-bar icon colours. **Defaults to `Dark`**, not `System` — see [Q11](#q11-why-does-the-theme-default-to-dark-rather-than-following-the-system).
- An `IOException` while reading falls back to defaults, so a corrupt file never crashes the app. Writes go through `dataStore.edit {}`, which is atomic.

### 2.5 Nordic Mint theme

The UI follows the design handoff in `design_handoff_streamly_nordic_mint/`. Dark is the canonical scheme; the light scheme exists so an explicit Light choice works, and the default is Dark.

- `core/theme/Color.kt` — handoff tokens (`SlateCharcoal` #121820, `SoftCharcoal` #1E2630, `SurfaceRaised`, `SageMint` #2EC4B6, `MintText`, `MintTint`, `OnMint`, the text ramp, `StorageOther`) mapped onto the Material `ColorScheme`: `MintTint`/`MintText` become `primaryContainer`/`onPrimaryContainer`, `SoftCharcoal` becomes `surfaceContainer`.
- `StreamlyBrand` holds what has no Material slot, plus every colour that sits **on video** — letterbox, scrim, on-media text, frosted surfaces, glow, and `MintTintOnMedia`/`MintTextOnMedia`. These are pinned dark values on purpose: the frame underneath is never themed, so a themed pair would be unreadable over footage in the light scheme.
- `core/theme/Shape.kt` — `asymmetricPill(height)` is the brand signature (17/6/17/17 at 34dp, scaled by height), used by chips, badges, the nav indicator and Follow/Subscribe.
- `core/theme/Type.kt` — the handoff scale with Medium headings and SemiBold badges, plus `StreamlyType` for the mono timestamps and the wordmark. Inter and JetBrains Mono are not bundled, so these resolve to the platform sans/mono.
- `core/theme/StreamlyLogo.kt` — the v2-bolt mark drawn in Compose from the same path data as `res/drawable/ic_streamly_logo.xml`, because the splash animates the play mark and the bolt separately.
- Dynamic colour is deliberately off: a wallpaper-derived scheme would break both the brand and the contrast of text drawn over video.

**Known substitutions.** Frosted surfaces are translucent fills, not backdrop blurs (`Modifier.blur` is API 31+ and does not sample what is behind it); material3 1.3's `IconButton` is circle-only, so 12dp-radius icon buttons are clipped clickable boxes.

### 2.6 Session, onboarding and profile

- `UserSession` (display name, handle, avatar, premium flag, `SignInMethod`, timestamp) is persisted in the same DataStore. There is no auth backend yet: `UserSession.forMethod()` builds the session each button produces, and only the callers of `PreferencesRepository.signIn` change when real auth arrives.
- **Guest is a real session** with no identity. That is what makes a returning guest skip onboarding.
- Onboarding reuses the Profile screen's `SignInCard` rather than a second copy of the pitch; the card takes an optional `onContinueAsGuest`, rendered only where skipping is possible.
- Sign-out confirms in a dialog, clears the session and resets the stack to Onboarding. `ProfileViewModel.signingOut` holds the last signed-in frame while the session clears, so the screen does not flash its own sign-in card on the way out.
- Profile's stats come from data the app already keeps — watched IDs, completed downloads, favourites — rather than invented numbers. "Clear cache" is scoped to Coil's image cache; clearing the Media3 download cache would delete the viewer's offline videos behind an innocuous label.

---

## 3. Key Files & Responsibilities

Paths are relative to `app/src/main/java/com/tridivroy/streamly/`.

| Layer | File | Responsibility |
|---|---|---|
| App | `StreamlyApp.kt` | `@HiltAndroidApp` entry point |
| App | `MainActivity.kt` / `MainViewModel.kt` | Edge-to-edge host, applies the persisted theme, computes `WindowSizeClass`, hosts the nav graph |
| Domain | `domain/model/Video.kt` | Core video model |
| Domain | `domain/model/VideoDownload.kt` | Download snapshot + `DownloadStatus` |
| Domain | `domain/model/UserPreferences.kt` | Favourites, saved, subscriptions, watched, session, `PlaybackQuality`, `ThemeMode` |
| Domain | `domain/model/UserSession.kt` | Signed-in account + `SignInMethod` (Google / Email / Guest) |
| Domain | `domain/repository/VideoRepository.kt` | Home, Shorts and by-id video access |
| Domain | `domain/repository/DownloadRepository.kt` | Download, remove, observe, offline metadata |
| Domain | `domain/repository/PreferencesRepository.kt` | Preference reads and writes |
| Data | `data/remote/dto/VideoDto.kt`, `data/mapper/VideoMapper.kt` | API DTO and mapping to domain |
| Data | `data/repository/VideoRepositoryImpl.kt` | Ktor calls with fallback test streams |
| Data | `data/local/datastore/UserPreferencesRepository.kt` | DataStore-backed preferences |
| Core | `core/network/NetworkModule.kt` | Ktor `HttpClient` (CIO, JSON, timeouts, logging), shared `Json` |
| Core | `core/di/MediaModule.kt` | HTTP data source, cache-aware `MediaSource.Factory`, app-wide `ExoPlayer` |
| Core | `core/di/DownloadModule.kt` | Download DB, `SimpleCache`, `DownloadManager` |
| Core | `core/di/DataStoreModule.kt` | Preferences DataStore singleton |
| Core | `core/di/RepositoryModule.kt` | Binds domain interfaces to implementations |
| Core | `core/media/ShortsPlayerPool.kt` | Two-player pool with neighbour preloading |
| Core | `core/media/DownloadTracker.kt` | `DownloadRepository` over Media3 `DownloadManager` |
| Core | `core/media/MediaDownloadService.kt` | Foreground download service + notification + scheduler |
| Core | `core/media/VideoMediaItem.kt` | `Video.toMediaItem()` with explicit HLS MIME type |
| Core | `core/media/NowPlayingStore.kt` | Mirrors the shared player for the mini-player; polls position only while playing |
| Core | `core/storage/DeviceStorage.kt` | Real `StatFs` figures for the Downloads usage card |
| Core | `core/storage/ImageCacheCleaner.kt` | Measures and clears Coil's image cache (never the download cache) |
| Core | `core/theme/` | Nordic Mint `Color.kt`, `Shape.kt` (`asymmetricPill`), `Type.kt`, `Theme.kt`, `StreamlyLogo.kt`, full `StreamlyIcons` set |
| Presentation | `presentation/navigation/NavGraph.kt` | `StreamlyNavGraph`: back stack, tabs, entry decorators |
| Presentation | `presentation/navigation/StreamlyNavKeys.kt` | `OnboardingKey`, `HomeKey`, `ShortsKey`, `DownloadsKey`, `ProfileKey`, `PlayerKey` |
| Presentation | `presentation/navigation/components/` | `StreamlyBottomBar` / `StreamlyNavRail`, `MiniPlayer` |
| Presentation | `presentation/splash/StreamlySplash.kt` | Compose half of the launch animation, after the SplashScreen API icon |
| Presentation | `presentation/onboarding/*` | Start destination when no session exists; Google / Email / Guest |
| Presentation | `presentation/profile/*` | Signed-out and signed-in states, stats, settings list, sign-out dialog |
| Presentation | `presentation/common/*` | `formatDuration`/`formatCount`/`formatRelativeAge`/`formatBytes`, `rememberPlaybackProgress` |
| Presentation | `presentation/components/StreamlyToast.kt` | The handoff's toast pill |
| Presentation | `presentation/home/*` | Home feed MVI, top bar, category chips, video cards |
| Presentation | `presentation/player/*` | Player MVI, floating viewport, auto-hiding controls, fullscreen, Overview / Key Moments / Up Next tabs |
| Presentation | `presentation/shorts/*` | Shorts MVI, `VerticalPager`, overlays, progress |
| Presentation | `presentation/downloads/*` | Downloads list MVI, offline playback entry |
| Presentation | `presentation/settings/*` | Theme and quality bottom sheet |
| Config | `AndroidManifest.xml` | Download service (`dataSync`), `PlatformSchedulerService`, permissions |
| Tooling | `AGENTS.md` (symlinked as `CLAUDE.md`, `.cursorrules`) | Architecture rules + execution log |
| Tooling | `.claude/settings.json`, `.claude/hooks/log-prompt.sh`, `docs/prompt-history.md` | Automatic raw prompt history |

---

## 4. Technical Walkthrough & Q&A

#### Q1. Are `core`, `data`, `domain` and `presentation` separate Gradle modules?
No, not yet. They are packages in `:app`, so the compiler does not stop `domain` from importing Android classes; review does. Moving `domain` into a pure `kotlin("jvm")` module would let the build itself reject framework imports, and would speed up incremental builds. This is the main architecture follow-up.

#### Q2. Why is the ExoPlayer a singleton, and how does it avoid leaks?
Creating a player is expensive, and the rules require reuse across navigation and rotation. The singleton is created with the application context, and screens never release it. `PlayerView` is detached in `AndroidView.onRelease`, so no Activity-bound view keeps a reference to the player. Since workflow 11 `PlayerViewModel.onCleared()` does not stop playback either: leaving the Player hands the video to the mini-player. It is reined in by the mini-player's pause button, `NowPlayingStore.pause()` when Shorts takes over, and the `ProcessLifecycleOwner` observer that pauses on background.

#### Q3. Why does Shorts use its own players instead of the singleton?
A single player can't buffer the next clip while playing the current one. With two pooled players, the next page is prepared and paused on its first frame before the swipe. Capping the pool at two follows the "1–2 active players" rule and stops audio bleed, because only the active slot ever plays.

#### Q4. How does the Shorts feed decide which video plays?
`ShortsPager` watches `pagerState.settledPage` through `snapshotFlow` and sends `OnPageSettled(index)`. `ShortsViewModel` calls `ShortsPlayerPool.activate(index)`, which plays that slot and preloads the neighbour. It then stores `activeIndex` and `preloadIndex` in state, and pages render a `PlayerView` only when they match one of those. Screen `ON_STOP` / `ON_START` pause and resume playback, while a user tap-to-pause is tracked separately as `isPaused`.

#### Q5. How is a ViewModel scoped to a single Navigation 3 destination?
With `rememberViewModelStoreNavEntryDecorator()` on `NavDisplay`. Each back-stack entry gets its own `ViewModelStore`, which is cleared when the entry is popped. Arguments are passed with Hilt assisted injection (`@HiltViewModel(assistedFactory = ...)` and `hiltViewModel(creationCallback = ...)`), so `videoId` is a constructor parameter rather than a `SavedStateHandle` lookup.

#### Q6. How do HLS downloads play back offline?
Three things work together:
1. All players read through a read-only `CacheDataSource` over the download cache.
2. Downloaded items are played with the `streamKeys` from their `DownloadRequest`, so the player only asks for the rendition that was downloaded.
3. Video metadata is stored in the request's `data` bytes, so the Player can show details without the API.

#### Q7. How is download progress tracked when `DownloadManager` has no progress callback?
`DownloadTracker` listens for state changes (`onDownloadChanged` / `onDownloadRemoved`). While any download is in progress, it also polls `currentDownloads` every second. Media3's `Download` progress changes in place, so the tracker copies it into immutable `VideoDownload` snapshots. Otherwise `StateFlow` would compare equal and never emit.

#### Q8. What happens when the API is unreachable?
`VideoRepositoryImpl` falls back to a bundled list of public HLS test streams, so Home and Shorts always have content. Because of this, their `Error` states are rarely reached today. `getVideoById` falls back to the same list, and then to downloaded metadata. Once a real backend is configured, decide whether that fallback should stay in release builds.

#### Q9. Where does user-selected quality take effect?
In two places:
- **Playback:** `PlayerViewModel` sets `trackSelectionParameters.setMaxVideoSize(..., maxHeight)` on the shared player before preparing.
- **Downloads:** `DownloadTracker` passes the same cap to `DownloadHelper` and forces the single highest rendition under it.

`Auto` removes the cap. The Shorts pool does not apply the cap yet.

#### Q10. How are one-off events (navigation, share, snackbars) kept from re-firing on rotation?
They are not part of `UiState`. Each ViewModel sends them through `Channel(BUFFERED).receiveAsFlow()`, so each effect is delivered once to one collector. Routes collect them inside `repeatOnLifecycle(STARTED)` and read callbacks through `rememberUpdatedState`, so a recreated Activity neither replays nor drops them.

#### Q11. Why does the theme default to Dark rather than following the system?
Because "System" made the brand unreachable. Nordic Mint is a dark-only design, and on any device without a system dark mode — anything pre-Android 10, or simply a phone in light mode — `isSystemInDarkTheme()` is false, so the app opened in the light variant and the design was never seen. This was caught on device in workflow 12 (API 26) and the default moved to `Dark` in the model, the DataStore fallback and `MainViewModel`'s initial value. Light and System remain selectable in Profile.

#### Q12. How do the Player overlay controls decide when to hide?
`PlayerControlsState` holds visibility plus an `interactionCount` that is bumped by every interaction and used as a `LaunchedEffect` key, so each new interaction cancels the pending hide and starts a fresh 3-second countdown. The countdown only runs while playing — a paused video keeps its controls, since the play button is the way back out. The controls are wrapped in `AnimatedVisibility` rather than faded with alpha, because a zero-alpha overlay would still swallow the taps meant for the video underneath.

#### Q13. What does the fullscreen toggle actually change?
It flips one piece of saveable state in `PlayerScreen`. The same `FloatingPlayerViewport` then fills the window instead of floating, so the overlay and its auto-hide behave identically in both modes. A single `FullscreenWindowEffect` owns both window side effects — system bars and `requestedOrientation` — and clears both in `onDispose`, so leaving the Player by any route cannot strand the app locked in landscape or immersive. Rotating a phone into landscape still enters fullscreen on its own; the button and the device drive the same state.

---

### Known gaps / next steps

- Split layers into Gradle modules ([Q1](#q1-are-core-data-domain-and-presentation-separate-gradle-modules)).
- Replace the placeholder `BASE_URL` and review the fallback streams for release builds.
- There is no favourites list screen yet; favourites drive the Shorts/Player like state and the Profile stat.
- No download-complete notification, and a failed retry from the Downloads list shows no extra message.
- The quality cap is not applied to the Shorts pool.
- No unit or UI tests yet. Workflow 12 was a manual ADB smoke test, not an automated suite — the flows it covered are the obvious first instrumentation targets.
- Auth is mocked: every button writes a placeholder session, and the Google button shows a neutral monogram rather than Google's logo. Wiring a real provider changes only the callers of `PreferencesRepository.signIn` and that one asset.
- Inter and JetBrains Mono are not bundled, so type falls back to the platform faces.
- The Shorts rail's Comments entry shows a count but has nowhere to go; the Player's overflow slot is likewise empty.
