package com.tridivroy.streamly.presentation.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.tridivroy.streamly.core.theme.SageMint
import com.tridivroy.streamly.core.theme.StreamlyTheme
import com.tridivroy.streamly.domain.model.SignInMethod
import com.tridivroy.streamly.presentation.common.safeBottomPadding
import com.tridivroy.streamly.presentation.common.safeTopPadding
import com.tridivroy.streamly.presentation.profile.components.SignInCard

/**
 * The app's start destination while there is no session — account or guest.
 *
 * It reuses the Profile screen's [SignInCard] rather than a second copy of the same pitch, adding the
 * "Continue as guest" button that only makes sense here: inside Profile there is nothing to skip past.
 */
@Composable
fun OnboardingRoute(
    onSignedIn: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnSignedIn by rememberUpdatedState(onSignedIn)

    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    OnboardingUiEffect.NavigateToHome -> currentOnSignedIn()
                }
            }
        }
    }

    OnboardingScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        modifier = modifier,
    )
}

@Composable
fun OnboardingScreen(
    uiState: OnboardingUiState,
    onEvent: (OnboardingUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            // The same mint wash the splash fades out on, so the first real screen continues from it
            // rather than cutting to a flat ground.
            .background(
                Brush.radialGradient(
                    colors = listOf(SageMint.copy(alpha = 0.08f), Color.Transparent),
                    radius = 900f,
                ),
            )
            .safeTopPadding()
            .safeBottomPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(Modifier.height(24.dp))
            SignInCard(
                pendingMethod = uiState.pendingMethod,
                onSignIn = { method -> onEvent(OnboardingUiEvent.OnSignInClick(method)) },
                onContinueAsGuest = { onEvent(OnboardingUiEvent.OnContinueAsGuestClick) },
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121820, heightDp = 820)
@Composable
private fun OnboardingPreview() {
    StreamlyTheme(darkTheme = true) {
        OnboardingScreen(uiState = OnboardingUiState(), onEvent = {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121820, heightDp = 820)
@Composable
private fun OnboardingPendingPreview() {
    StreamlyTheme(darkTheme = true) {
        OnboardingScreen(
            uiState = OnboardingUiState(pendingMethod = SignInMethod.Google),
            onEvent = {},
        )
    }
}
