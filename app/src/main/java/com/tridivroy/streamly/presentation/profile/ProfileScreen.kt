package com.tridivroy.streamly.presentation.profile

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.tridivroy.streamly.R
import com.tridivroy.streamly.core.theme.StreamlyIcons
import com.tridivroy.streamly.core.theme.StreamlyShape
import com.tridivroy.streamly.core.theme.StreamlyTheme
import com.tridivroy.streamly.core.theme.StreamlyType
import com.tridivroy.streamly.core.theme.TextMuted
import com.tridivroy.streamly.domain.model.SignInMethod
import com.tridivroy.streamly.domain.model.ThemeMode
import com.tridivroy.streamly.domain.model.UserSession
import com.tridivroy.streamly.presentation.common.formatBytes
import com.tridivroy.streamly.presentation.common.safeBottomPadding
import com.tridivroy.streamly.presentation.common.safeTopPadding
import com.tridivroy.streamly.presentation.components.StreamlyToastHost
import com.tridivroy.streamly.presentation.components.rememberToastState
import com.tridivroy.streamly.presentation.profile.components.ProfileHeader
import com.tridivroy.streamly.presentation.profile.components.ProfileSettingsList
import com.tridivroy.streamly.presentation.profile.components.SignInCard
import com.tridivroy.streamly.presentation.profile.components.WatchStatsRow
import com.tridivroy.streamly.presentation.settings.SettingsSheet

/** Stateful entry point: wires [ProfileViewModel] to the stateless [ProfileScreen]. */
@Composable
fun ProfileRoute(
    onBack: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    onSignedOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val toastState = rememberToastState()
    val currentOnNavigateToDownloads by rememberUpdatedState(onNavigateToDownloads)
    val currentOnSignedOut by rememberUpdatedState(onSignedOut)

    val cacheClearedFormat = stringResource(R.string.profile_cache_cleared)

    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    ProfileUiEffect.NavigateToDownloads -> currentOnNavigateToDownloads()
                    ProfileUiEffect.NavigateToOnboarding -> currentOnSignedOut()
                    is ProfileUiEffect.CacheCleared ->
                        toastState.show(cacheClearedFormat.format(formatBytes(effect.freedBytes)))
                }
            }
        }
    }

    var showQualitySettings by rememberSaveable { mutableStateOf(false) }

    Box(modifier.fillMaxSize()) {
        ProfileScreen(
            uiState = uiState,
            onEvent = viewModel::onEvent,
            onBack = onBack,
            onQualityClick = { showQualitySettings = true },
        )
        StreamlyToastHost(state = toastState, modifier = Modifier.align(Alignment.BottomCenter))
    }

    if (showQualitySettings) {
        SettingsSheet(onDismiss = { showQualitySettings = false })
    }
}

@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    onEvent: (ProfileUiEvent) -> Unit,
    onBack: () -> Unit,
    onQualityClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Signing out is the one irreversible-feeling action here, so it asks first.
    var showSignOutConfirm by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeTopPadding(),
    ) {
        ProfileTopBar(onBack = onBack)

        // Signing in swaps the whole body, so the transition is the screen's main motion: the new
        // content fades up from slightly below while the old one fades out.
        AnimatedContent(
            targetState = uiState,
            transitionSpec = {
                (fadeIn(tween(260)) + slideInVertically(tween(260)) { it / 12 })
                    .togetherWith(fadeOut(tween(160)))
            },
            contentKey = { state ->
                when (state) {
                    ProfileUiState.Loading -> "loading"
                    is ProfileUiState.SignedOut -> "signedOut"
                    is ProfileUiState.SignedIn -> "signedIn"
                }
            },
            label = "profileState",
        ) { state ->
            when (state) {
                ProfileUiState.Loading -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }

                is ProfileUiState.SignedOut -> SignedOutContent(
                    pendingMethod = state.pendingMethod,
                    onSignIn = { method -> onEvent(ProfileUiEvent.OnSignInClick(method)) },
                )

                is ProfileUiState.SignedIn -> SignedInContent(
                    state = state,
                    onEvent = onEvent,
                    onQualityClick = onQualityClick,
                    onSignOutClick = { showSignOutConfirm = true },
                )
            }
        }
    }

    if (showSignOutConfirm) {
        SignOutConfirmDialog(
            onConfirm = {
                showSignOutConfirm = false
                onEvent(ProfileUiEvent.OnSignOutClick)
            },
            onDismiss = { showSignOutConfirm = false },
        )
    }
}

@Composable
private fun SignOutConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        titleContentColor = MaterialTheme.colorScheme.onBackground,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = StreamlyShape.Card,
        title = { Text(stringResource(R.string.profile_sign_out_confirm_title)) },
        text = { Text(stringResource(R.string.profile_sign_out_confirm_body)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.profile_sign_out),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.profile_sign_out_cancel),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        },
    )
}

@Composable
private fun ProfileTopBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 10.dp, end = 18.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(StreamlyShape.IconButton)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                // Left arrow rather than a chevron, matching the Player header.
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.profile_back),
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            text = stringResource(R.string.profile_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun SignedOutContent(
    pendingMethod: SignInMethod?,
    onSignIn: (SignInMethod) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .safeBottomPadding()
            .padding(horizontal = 14.dp),
    ) {
        SignInCard(pendingMethod = pendingMethod, onSignIn = onSignIn)
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun SignedInContent(
    state: ProfileUiState.SignedIn,
    onEvent: (ProfileUiEvent) -> Unit,
    onQualityClick: () -> Unit,
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .safeBottomPadding()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ProfileHeader(session = state.session)
        WatchStatsRow(stats = state.stats)
        ProfileSettingsList(
            themeMode = state.themeMode,
            cacheBytes = state.cacheBytes,
            onThemeModeSelect = { mode -> onEvent(ProfileUiEvent.OnThemeModeSelect(mode)) },
            onDownloadsClick = { onEvent(ProfileUiEvent.OnDownloadsClick) },
            onClearCacheClick = { onEvent(ProfileUiEvent.OnClearCacheClick) },
            onSignOutClick = onSignOutClick,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(StreamlyShape.Card)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .clickable(onClick = onQualityClick)
                .padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                imageVector = StreamlyIcons.Settings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(19.dp),
            )
            Text(
                text = stringResource(R.string.settings_quality),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = StreamlyIcons.ChevronRight,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(16.dp),
            )
        }

        Text(
            text = stringResource(R.string.profile_mock_notice),
            style = StreamlyType.Eyebrow,
            color = TextMuted,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121820, heightDp = 820)
@Composable
private fun ProfileSignedOutPreview() {
    StreamlyTheme(darkTheme = true) {
        ProfileScreen(
            uiState = ProfileUiState.SignedOut(),
            onEvent = {},
            onBack = {},
            onQualityClick = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121820, heightDp = 820)
@Composable
private fun ProfileSignedInPreview() {
    StreamlyTheme(darkTheme = true) {
        ProfileScreen(
            uiState = ProfileUiState.SignedIn(
                session = UserSession.forMethod(SignInMethod.Google),
                stats = WatchStats(videosWatched = 48, downloads = 6, favourites = 23),
                themeMode = ThemeMode.Dark,
                cacheBytes = 18_400_000,
            ),
            onEvent = {},
            onBack = {},
            onQualityClick = {},
        )
    }
}
