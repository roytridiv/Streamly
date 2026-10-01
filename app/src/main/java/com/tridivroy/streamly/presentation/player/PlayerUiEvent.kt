package com.tridivroy.streamly.presentation.player

/** User actions sent from [PlayerScreen] to [PlayerViewModel]. */
sealed interface PlayerUiEvent {
    data object Retry : PlayerUiEvent
}
