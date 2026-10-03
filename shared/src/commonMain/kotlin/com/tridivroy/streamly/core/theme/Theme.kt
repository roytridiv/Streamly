package com.tridivroy.streamly.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Nordic Mint, dark. The canonical scheme from the design handoff.
 *
 * Token mapping worth knowing when reading the screens: `surfaceContainer` is SoftCharcoal (cards),
 * `surfaceContainerHigh` is SurfaceRaised (toasts, pressed), and `primaryContainer` /
 * `onPrimaryContainer` are MintTint / MintText — the selected-chip pair. Anything with no slot is in
 * [StreamlyBrand].
 */
private val DarkColorScheme = darkColorScheme(
    primary = SageMint,
    onPrimary = OnMint,
    primaryContainer = MintTint,
    onPrimaryContainer = MintText,
    secondary = MintText,
    onSecondary = OnMint,
    secondaryContainer = MintTint,
    onSecondaryContainer = MintText,
    tertiary = MintText,
    onTertiary = OnMint,
    tertiaryContainer = SurfaceRaised,
    onTertiaryContainer = TextPrimary,
    background = SlateCharcoal,
    onBackground = TextPrimary,
    surface = SlateCharcoal,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceRaised,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = StreamlyBrand.Letterbox,
    surfaceContainerLow = SlateCharcoal,
    surfaceContainer = SoftCharcoal,
    surfaceContainerHigh = SurfaceRaised,
    surfaceContainerHighest = SurfaceRaised,
    inverseSurface = TextPrimary,
    inverseOnSurface = SlateCharcoal,
    outline = OutlineStrong,
    outlineVariant = OutlineSubtle,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    scrim = StreamlyBrand.Letterbox,
)

/**
 * The same hues re-balanced for a pale ground. The handoff is dark-only; this exists so the
 * DataStore `ThemeMode` preference (System/Light/Dark) keeps working rather than silently doing
 * nothing. Components that sit on video keep their fixed [StreamlyBrand] colours in both schemes.
 */
private val LightColorScheme = lightColorScheme(
    primary = MintLight,
    onPrimary = OnMintLight,
    primaryContainer = MintTintLight,
    onPrimaryContainer = OnMintTintLight,
    secondary = MintLight,
    onSecondary = OnMintLight,
    secondaryContainer = MintTintLight,
    onSecondaryContainer = OnMintTintLight,
    tertiary = MintLight,
    onTertiary = OnMintLight,
    tertiaryContainer = SurfaceContainerHighLight,
    onTertiaryContainer = OnBackgroundLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = BackgroundLight,
    onSurface = OnBackgroundLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceVariantLight,
    inverseSurface = OnBackgroundLight,
    inverseOnSurface = BackgroundLight,
    outline = OutlineStrongLight,
    outlineVariant = OutlineSubtleLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    scrim = StreamlyBrand.Letterbox,
)

/**
 * Applies the Nordic Mint theme.
 *
 * Dynamic colour is deliberately not used: Streamly's identity is the fixed mint-on-Slate-Charcoal
 * scheme, and the Shorts and Player overlays are tuned against it, so a wallpaper-derived scheme
 * would break both the brand and the contrast of text drawn over video.
 */
@Composable
fun StreamlyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        shapes = StreamlyShapes,
        content = content,
    )
}
