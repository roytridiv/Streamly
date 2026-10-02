package com.tridivroy.streamly.presentation.shorts

import com.tridivroy.streamly.domain.model.Video

/** User actions sent from [ShortsScreen] to [ShortsViewModel]. */
sealed interface ShortsUiEvent {
    /** The pager came to rest on [index]. */
    data class OnPageSettled(val index: Int) : ShortsUiEvent
    data object OnTogglePlay : ShortsUiEvent
    data class OnLikeClick(val videoId: String) : ShortsUiEvent
    data class OnSaveClick(val videoId: String) : ShortsUiEvent
    /** [channelHandle] without the leading "@". */
    data class OnFollowClick(val channelHandle: String) : ShortsUiEvent
    data class OnShareClick(val video: Video) : ShortsUiEvent
    data object OnScreenStart : ShortsUiEvent
    data object OnScreenStop : ShortsUiEvent
    data object Retry : ShortsUiEvent
}

/** One-off actions sent from [ShortsViewModel] to [ShortsScreen], consumed exactly once. */
sealed interface ShortsUiEffect {
    data class Share(val video: Video) : ShortsUiEffect
}
