package com.tridivroy.streamly.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tridivroy.streamly.domain.model.SignInMethod
import com.tridivroy.streamly.domain.model.UserSession
import com.tridivroy.streamly.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** [pendingMethod] is the button showing its spinner; `null` means nothing is in flight. */
data class OnboardingUiState(
    val pendingMethod: SignInMethod? = null,
)

sealed interface OnboardingUiEvent {
    data class OnSignInClick(val method: SignInMethod) : OnboardingUiEvent
    data object OnContinueAsGuestClick : OnboardingUiEvent
}

sealed interface OnboardingUiEffect {
    /** Session written; the nav graph swaps onboarding out for Home. */
    data object NavigateToHome : OnboardingUiEffect
}

/**
 * Writes a session and then says so. Every route out of onboarding goes through [start], including
 * "Continue as guest" — a guest is a session, which is what stops onboarding reappearing next launch.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _effects = Channel<OnboardingUiEffect>(Channel.BUFFERED)
    val effects: Flow<OnboardingUiEffect> = _effects.receiveAsFlow()

    fun onEvent(event: OnboardingUiEvent) {
        when (event) {
            is OnboardingUiEvent.OnSignInClick -> start(event.method)
            OnboardingUiEvent.OnContinueAsGuestClick -> start(SignInMethod.Guest)
        }
    }

    /**
     * There is no identity provider behind these buttons yet, so this writes the session straight to
     * DataStore. The short delay keeps the spinner on screen long enough to read as a response rather
     * than a flicker, and is the one line that goes when real auth is wired in.
     */
    private fun start(method: SignInMethod) {
        if (_uiState.value.pendingMethod != null) return
        viewModelScope.launch {
            _uiState.value = OnboardingUiState(pendingMethod = method)
            delay(SIGN_IN_FEEDBACK_MS)
            preferencesRepository.signIn(UserSession.forMethod(method))
            _effects.send(OnboardingUiEffect.NavigateToHome)
        }
    }

    private companion object {
        const val SIGN_IN_FEEDBACK_MS = 450L
    }
}
