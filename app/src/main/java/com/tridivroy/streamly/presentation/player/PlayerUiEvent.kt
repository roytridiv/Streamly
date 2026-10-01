package com.tridivroy.streamly.presentation.player

/** User actions sent from [PlayerScreen] to [PlayerViewModel]. */
sealed interface PlayerUiEvent {
    data object Retry : PlayerUiEvent
    /** Starts, cancels or deletes the offline download depending on its current state. */
    data object OnDownloadClick : PlayerUiEvent
}

/** One-off actions sent from [PlayerViewModel] to [PlayerScreen], consumed exactly once. */
sealed interface PlayerUiEffect {
    data class DownloadFailed(val message: String?) : PlayerUiEffect
}
