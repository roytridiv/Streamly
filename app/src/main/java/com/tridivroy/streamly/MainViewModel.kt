package com.tridivroy.streamly

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tridivroy.streamly.domain.model.ThemeMode
import com.tridivroy.streamly.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Activity-level state: the persisted theme choice. */
@HiltViewModel
class MainViewModel @Inject constructor(
    preferencesRepository: PreferencesRepository,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = preferencesRepository.userPreferences
        .map { it.themeMode }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.System)
}
