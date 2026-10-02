package com.tridivroy.streamly.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = MintDark,
    onPrimary = OnMintDark,
    primaryContainer = MintContainerDark,
    onPrimaryContainer = OnMintContainerDark,
    secondary = SageDark,
    onSecondary = OnSageDark,
    secondaryContainer = SageContainerDark,
    onSecondaryContainer = OnSageContainerDark,
    tertiary = FrostDark,
    onTertiary = OnFrostDark,
    tertiaryContainer = FrostContainerDark,
    onTertiaryContainer = OnFrostContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    surfaceContainerLowest = BackgroundDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceVariantDark,
    inverseSurface = OnSurfaceDark,
    inverseOnSurface = SurfaceDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    scrim = StreamlyMediaColors.Letterbox,
)

private val LightColorScheme = lightColorScheme(
    primary = MintLight,
    onPrimary = OnMintLight,
    primaryContainer = MintContainerLight,
    onPrimaryContainer = OnMintContainerLight,
    secondary = SageLight,
    onSecondary = OnSageLight,
    secondaryContainer = SageContainerLight,
    onSecondaryContainer = OnSageContainerLight,
    tertiary = FrostLight,
    onTertiary = OnFrostLight,
    tertiaryContainer = FrostContainerLight,
    onTertiaryContainer = OnFrostContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceVariantLight,
    inverseSurface = OnSurfaceLight,
    inverseOnSurface = SurfaceLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    scrim = StreamlyMediaColors.Letterbox,
)

/**
 * Applies the Nordic Mint / Sage Green brand palette.
 *
 * Dynamic colour is deliberately **not** used: Streamly's identity is the fixed mint-on-Nordic-night
 * scheme, and the Shorts/Player overlays are tuned against it, so a wallpaper-derived scheme would
 * break both the brand and the contrast of text drawn over video.
 */
@Composable
fun StreamlyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content,
    )
}
