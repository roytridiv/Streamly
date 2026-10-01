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
