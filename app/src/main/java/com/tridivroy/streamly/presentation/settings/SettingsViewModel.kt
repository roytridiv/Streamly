package com.tridivroy.streamly.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tridivroy.streamly.domain.model.PlaybackQuality
import com.tridivroy.streamly.domain.model.ThemeMode
import com.tridivroy.streamly.domain.model.UserPreferences
import com.tridivroy.streamly.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Settings only has Loading and Success: the preferences flow never errors (unreadable files fall
 * back to defaults) and is never empty (defaults always exist).
 */
sealed interface SettingsUiState {
    data object Loading : SettingsUiState
    data class Success(val preferences: UserPreferences) : SettingsUiState
}

sealed interface SettingsUiEvent {
    data class OnThemeModeSelect(val mode: ThemeMode) : SettingsUiEvent
    data class OnPlaybackQualitySelect(val quality: PlaybackQuality) : SettingsUiEvent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = preferencesRepository.userPreferences
        .map<UserPreferences, SettingsUiState> { SettingsUiState.Success(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState.Loading)

    fun onEvent(event: SettingsUiEvent) {
        viewModelScope.launch {
            when (event) {
                is SettingsUiEvent.OnThemeModeSelect -> preferencesRepository.setThemeMode(event.mode)
                is SettingsUiEvent.OnPlaybackQualitySelect -> preferencesRepository.setPlaybackQuality(event.quality)
            }
        }
    }
}
