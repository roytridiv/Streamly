package com.tridivroy.streamly.presentation.home

/** User actions sent from [HomeScreen] to [HomeViewModel]. */
sealed interface HomeUiEvent {
    data class OnVideoClick(val videoId: String) : HomeUiEvent
    data class OnCategorySelect(val category: String?) : HomeUiEvent
    data object Refresh : HomeUiEvent
}

/** One-off actions sent from [HomeViewModel] to [HomeScreen], consumed exactly once. */
sealed interface HomeUiEffect {
    data class NavigateToPlayer(val videoId: String) : HomeUiEffect
}
