package com.tridivroy.streamly.core.theme

import androidx.compose.ui.graphics.Color

/*
 * Streamly's "Nordic Mint / Sage Green" palette.
 *
 * The brand is a cool, near-black Nordic ground (#121820) lit by a mint accent (#2EC4B6), with
 * sage green as the secondary and a frost blue tertiary. The dark scheme is the canonical one —
 * it is what the video surfaces are designed against — and the light scheme is the same hues
 * re-balanced for a pale ground. Dynamic colour is off by default (see StreamlyTheme) so these
 * values are what ships.
 */

// --- Brand anchors -------------------------------------------------------------------------------

/** The Nordic ground: app background and base surface in dark mode. */
val NordicNight = Color(0xFF121820)

/** The mint accent: primary actions, selected tabs, progress. */
val MintAccent = Color(0xFF2EC4B6)

// --- Dark scheme ---------------------------------------------------------------------------------

val MintDark = MintAccent
val OnMintDark = Color(0xFF00201C)
val MintContainerDark = Color(0xFF164F49)
val OnMintContainerDark = Color(0xFF9FF0E6)

val SageDark = Color(0xFFA3C4A8)
val OnSageDark = Color(0xFF0D2614)
val SageContainerDark = Color(0xFF35503C)
val OnSageContainerDark = Color(0xFFBFE0C4)

val FrostDark = Color(0xFF8FB8D4)
val OnFrostDark = Color(0xFF0C2432)
val FrostContainerDark = Color(0xFF2A4356)
val OnFrostContainerDark = Color(0xFFCBE3F5)

val BackgroundDark = NordicNight
val OnBackgroundDark = Color(0xFFE3E8EE)
val SurfaceDark = NordicNight
val OnSurfaceDark = Color(0xFFE3E8EE)
val SurfaceVariantDark = Color(0xFF2A3440)
val OnSurfaceVariantDark = Color(0xFFAEBAC6)
val SurfaceContainerLowDark = Color(0xFF171E27)
val SurfaceContainerDark = Color(0xFF1A222C)
val SurfaceContainerHighDark = Color(0xFF222C38)
val OutlineDark = Color(0xFF44515E)
val OutlineVariantDark = Color(0xFF2A3440)

val ErrorDark = Color(0xFFFFB4AB)
val OnErrorDark = Color(0xFF690005)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainerDark = Color(0xFFFFDAD6)

// --- Light scheme --------------------------------------------------------------------------------

/** Mint is darkened on light grounds; #2EC4B6 does not reach 4.5:1 against white. */
val MintLight = Color(0xFF00796E)
val OnMintLight = Color(0xFFFFFFFF)
val MintContainerLight = Color(0xFFA8F2E8)
val OnMintContainerLight = Color(0xFF00201C)

val SageLight = Color(0xFF41634C)
val OnSageLight = Color(0xFFFFFFFF)
val SageContainerLight = Color(0xFFCCE9D2)
val OnSageContainerLight = Color(0xFF0D2614)

val FrostLight = Color(0xFF2E5A77)
val OnFrostLight = Color(0xFFFFFFFF)
val FrostContainerLight = Color(0xFFCDE5F7)
val OnFrostContainerLight = Color(0xFF0C2432)

val BackgroundLight = Color(0xFFF5F9F8)
val OnBackgroundLight = Color(0xFF121820)
val SurfaceLight = Color(0xFFF5F9F8)
val OnSurfaceLight = Color(0xFF121820)
val SurfaceVariantLight = Color(0xFFDBE5E3)
val OnSurfaceVariantLight = Color(0xFF404A54)
val SurfaceContainerLowestLight = Color(0xFFFFFFFF)
val SurfaceContainerLowLight = Color(0xFFEFF4F3)
val SurfaceContainerLight = Color(0xFFE9EFEE)
val SurfaceContainerHighLight = Color(0xFFE3EAE9)
val OutlineLight = Color(0xFF707A85)
val OutlineVariantLight = Color(0xFFC0CBC9)

val ErrorLight = Color(0xFFBA1A1A)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFFDAD6)
val OnErrorContainerLight = Color(0xFF410002)

// --- Media tokens --------------------------------------------------------------------------------

/**
 * Fixed colours for video surfaces. These sit behind or on top of video frames, so they must not
 * follow the light/dark scheme: letterbox bars stay near-black and overlay text stays legible
 * whatever the frame underneath it looks like.
 */
object StreamlyMediaColors {
    /** Letterbox / shutter behind a video frame. Slightly warmer than pure black, to match the brand. */
    val Letterbox = Color(0xFF0B1016)

    /** Scrim under overlay text on a thumbnail or a Shorts frame. */
    val Scrim = Color(0xCC0B1016)

    /** Text and icons drawn directly on a video frame. */
    val OnMedia = Color(0xFFF2F6F8)
    val OnMediaVariant = Color(0xCCF2F6F8)

    /** Active "liked" tint in the Shorts feed. */
    val Like = Color(0xFFFF5A7A)
}
