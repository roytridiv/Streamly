package com.tridivroy.streamly.presentation.player

/** User actions sent from [PlayerScreen] to [PlayerViewModel]. */
sealed interface PlayerUiEvent {
    data object Retry : PlayerUiEvent
    /** Starts, cancels or deletes the offline download depending on its current state. */
    data object OnDownloadClick : PlayerUiEvent
    /** An "Up Next" suggestion was tapped. */
    data class OnRelatedVideoClick(val videoId: String) : PlayerUiEvent
    /** Optimistic toggle, persisted as a favourite. */
    data object OnLikeClick : PlayerUiEvent
    /** Optimistic toggle, persisted against the channel handle. */
    data object OnSubscribeClick : PlayerUiEvent
    data object OnShareClick : PlayerUiEvent
    data object OnToggleMute : PlayerUiEvent
    /** +10s, clamped to the end. */
    data object OnSeekForward : PlayerUiEvent
    /** -10s, clamped to the start. */
    data object OnSeekBackward : PlayerUiEvent
}

/** One-off actions sent from [PlayerViewModel] to [PlayerScreen], consumed exactly once. */
sealed interface PlayerUiEffect {
    data class DownloadFailed(val message: String?) : PlayerUiEffect
    /** Replace this Player entry with [videoId]'s, so Up Next doesn't stack Player screens. */
    data class NavigateToVideo(val videoId: String) : PlayerUiEffect
    /** Copy the video's link and confirm with the "Link copied" toast. */
    data class CopyLink(val url: String) : PlayerUiEffect
}
