package com.tridivroy.streamly.presentation.profile.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.tridivroy.streamly.R
import com.tridivroy.streamly.core.theme.StreamlyIcons
import com.tridivroy.streamly.core.theme.StreamlyShape
import com.tridivroy.streamly.core.theme.StreamlyType
import com.tridivroy.streamly.core.theme.TextMuted
import com.tridivroy.streamly.domain.model.ThemeMode
import com.tridivroy.streamly.presentation.common.formatBytes

/**
 * The settings block: the theme toggle, then rows for Downloads, Clear cache and Sign out.
 *
 * Playback quality deliberately stays in the existing settings sheet rather than being duplicated
 * here — one control, one home.
 */
@Composable
fun ProfileSettingsList(
    themeMode: ThemeMode,
    cacheBytes: Long?,
    onThemeModeSelect: (ThemeMode) -> Unit,
    onDownloadsClick: () -> Unit,
    onClearCacheClick: () -> Unit,
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.profile_settings_title),
            style = StreamlyType.Eyebrow,
            color = TextMuted,
            modifier = Modifier.padding(start = 4.dp),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(StreamlyShape.Card)
                .background(MaterialTheme.colorScheme.surfaceContainer),
        ) {
            ThemeRow(selected = themeMode, onSelect = onThemeModeSelect)
            RowDivider()
            SettingsRow(
                icon = StreamlyIcons.Download,
                label = stringResource(R.string.nav_downloads),
                value = null,
                onClick = onDownloadsClick,
            )
            RowDivider()
            SettingsRow(
                icon = StreamlyIcons.Trash,
                label = stringResource(R.string.profile_clear_cache),
                // Only ever the image cache, so the label says so and offline videos are safe.
                value = cacheBytes?.let { formatBytes(it) },
                onClick = onClearCacheClick,
            )
            RowDivider()
            SettingsRow(
                icon = StreamlyIcons.SignOut,
                label = stringResource(R.string.profile_sign_out),
                value = null,
                destructive = true,
                onClick = onSignOutClick,
            )
        }
    }
}

@Composable
private fun ThemeRow(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_theme),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Row(
            modifier = Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ThemeMode.entries.forEach { mode ->
                ThemeChip(
                    label = stringResource(mode.labelRes()),
                    selected = mode == selected,
                    onClick = { onSelect(mode) },
                )
            }
        }
    }
}

@Composable
private fun ThemeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = StreamlyShape.asymmetricPill(CHIP_HEIGHT)
    val background by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.background
        },
        animationSpec = tween(TRANSITION_MS),
        label = "themeChipBackground",
    )
    val border by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = tween(TRANSITION_MS),
        label = "themeChipBorder",
    )
    val content by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(TRANSITION_MS),
        label = "themeChipContent",
    )

    Box(
        modifier = modifier
            .height(CHIP_HEIGHT)
            .clip(shape)
            .background(background)
            .border(1.dp, border, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = content, maxLines = 1)
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    label: String,
    value: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    destructive: Boolean = false,
) {
    val content = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = content, modifier = Modifier.size(19.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = content,
            modifier = Modifier.weight(1f),
        )
        if (value != null) {
            Text(text = value, style = StreamlyType.MonoValue, color = TextMuted)
        }
        Icon(
            imageVector = StreamlyIcons.ChevronRight,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun RowDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .padding(start = 16.dp)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}

private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.System -> R.string.settings_theme_system
    ThemeMode.Light -> R.string.settings_theme_light
    ThemeMode.Dark -> R.string.settings_theme_dark
}

private val CHIP_HEIGHT = 34.dp
private const val TRANSITION_MS = 200
