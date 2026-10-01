package com.tridivroy.streamly.presentation.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tridivroy.streamly.R
import com.tridivroy.streamly.domain.model.PlaybackQuality
import com.tridivroy.streamly.domain.model.ThemeMode

/** Bottom sheet for theme and playback/download quality, persisted via DataStore. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    onDismiss: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        SettingsContent(uiState = uiState, onEvent = viewModel::onEvent)
    }
}

@Composable
private fun SettingsContent(
    uiState: SettingsUiState,
    onEvent: (SettingsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        SettingsUiState.Loading -> Box(modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        is SettingsUiState.Success -> Column(
            modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )

            OptionGroup(
                title = R.string.settings_theme,
                options = ThemeMode.entries,
                selected = uiState.preferences.themeMode,
                label = ::themeLabel,
                onSelect = { onEvent(SettingsUiEvent.OnThemeModeSelect(it)) },
            )

            OptionGroup(
                title = R.string.settings_quality,
                options = PlaybackQuality.entries,
                selected = uiState.preferences.playbackQuality,
                label = ::qualityLabel,
                onSelect = { onEvent(SettingsUiEvent.OnPlaybackQualitySelect(it)) },
            )
        }
    }
}

@Composable
private fun <T> OptionGroup(
    @StringRes title: Int,
    options: List<T>,
    selected: T,
    label: (T) -> Int,
    onSelect: (T) -> Unit,
) {
    Text(
        text = stringResource(title),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 4.dp),
    )
    Column(Modifier.selectableGroup()) {
        options.forEach { option ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(selected = option == selected, onClick = { onSelect(option) }, role = Role.RadioButton)
                    .padding(horizontal = 12.dp),
            ) {
                RadioButton(selected = option == selected, onClick = null, modifier = Modifier.padding(12.dp))
                Text(stringResource(label(option)), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@StringRes
private fun themeLabel(mode: ThemeMode): Int = when (mode) {
    ThemeMode.System -> R.string.settings_theme_system
    ThemeMode.Light -> R.string.settings_theme_light
    ThemeMode.Dark -> R.string.settings_theme_dark
}

@StringRes
private fun qualityLabel(quality: PlaybackQuality): Int = when (quality) {
    PlaybackQuality.Auto -> R.string.settings_quality_auto
    PlaybackQuality.High -> R.string.settings_quality_high
    PlaybackQuality.Medium -> R.string.settings_quality_medium
    PlaybackQuality.Low -> R.string.settings_quality_low
}
