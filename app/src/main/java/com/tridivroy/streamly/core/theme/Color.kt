package com.tridivroy.streamly.core.theme

import androidx.compose.ui.graphics.Color

/*
 * Streamly's "Nordic Mint" palette, from the design handoff
 * (design_handoff_streamly_nordic_mint/README.md → Design Tokens).
 *
 * Dark is the canonical scheme — every screen, and especially the video surfaces, is designed
 * against the Slate Charcoal ground. The light scheme below is the same hues re-balanced for a pale
 * ground, kept so the existing DataStore ThemeMode preference (System/Light/Dark) still works.
 *
 * Tokens the Material 3 ColorScheme has no slot for live in [StreamlyBrand].
 */

// --- Handoff tokens ------------------------------------------------------------------------------

/** App background. */
val SlateCharcoal = Color(0xFF121820)

/** Cards and surfaces. */
val SoftCharcoal = Color(0xFF1E2630)

/** Toasts, hover/pressed surfaces. */
val SurfaceRaised = Color(0xFF26303C)

/** Primary accent: lines, glows, badges, progress. */
val SageMint = Color(0xFF2EC4B6)

/** Mint text on dark mint tints. */
val MintText = Color(0xFF7FE0D6)

/** Selected fills: SageMint at 14% (the handoff's 12–16% band). */
val MintTint = SageMint.copy(alpha = 0.14f)

/** Text on solid mint. */
val OnMint = Color(0xFF0B1A1A)

val TextPrimary = Color(0xFFE9EDF0)
val TextSecondary = Color(0xFFC3CBD3)
val TextMuted = Color(0xFF8C97A3)

/** Hairline edges: TextPrimary at 10%. */
val OutlineSubtle = TextPrimary.copy(alpha = 0.10f)

/** Slightly stronger edge for borders that must read on their own. */
val OutlineStrong = TextPrimary.copy(alpha = 0.18f)

/** Non-Streamly storage in the Downloads usage bar. */
val StorageOther = Color(0xFF4A5562)

// --- Light scheme --------------------------------------------------------------------------------

/** Mint darkened for light grounds; #2EC4B6 does not reach 4.5:1 against white. */
val MintLight = Color(0xFF00796E)
val OnMintLight = Color(0xFFFFFFFF)
val MintTintLight = SageMint.copy(alpha = 0.16f)
val OnMintTintLight = Color(0xFF00413B)

val BackgroundLight = Color(0xFFF5F9F8)
val SurfaceContainerLowestLight = Color(0xFFFFFFFF)
val SurfaceContainerLowLight = Color(0xFFEFF4F3)
val SurfaceContainerLight = Color(0xFFE9EFEE)
val SurfaceContainerHighLight = Color(0xFFE1E9E8)
val SurfaceVariantLight = Color(0xFFDBE5E3)
val OnBackgroundLight = SlateCharcoal
val OnSurfaceVariantLight = Color(0xFF404A54)
val OutlineSubtleLight = SlateCharcoal.copy(alpha = 0.12f)
val OutlineStrongLight = SlateCharcoal.copy(alpha = 0.22f)

val ErrorDark = Color(0xFFFFB4AB)
val OnErrorDark = Color(0xFF690005)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainerDark = Color(0xFFFFDAD6)

val ErrorLight = Color(0xFFBA1A1A)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFFDAD6)
val OnErrorContainerLight = Color(0xFF410002)

/**
 * Brand colours with no Material ColorScheme slot, plus the ones that must stay fixed because they
 * sit on top of video frames — a letterbox bar or caption text cannot follow the light/dark scheme
 * and still be legible over an arbitrary frame.
 */
object StreamlyBrand {

    /** Muted meta text ("Channel · 1.2M views · 3 days ago", mono labels). */
    val TextMuted = com.tridivroy.streamly.core.theme.TextMuted

    /** Non-Streamly storage segment in the Downloads usage bar. */
    val StorageOther = com.tridivroy.streamly.core.theme.StorageOther

    /** Letterbox / shutter behind a video frame. */
    val Letterbox = Color(0xFF0A0E13)

    /** Scrim under overlay text on a thumbnail or a Shorts frame. */
    val Scrim = Color(0xB8121820)

    /** Text and icons drawn directly on a video frame. */
    val OnMedia = TextPrimary
    val OnMediaVariant = TextPrimary.copy(alpha = 0.80f)

    /** Frosted surfaces over video: Shorts rail, the player's centre play button. */
    val FrostedSurface = Color(0x66121820)
    val FrostedBorder = TextPrimary.copy(alpha = 0.16f)

    /** Mint glow rings. Handoff: `0 0 0 1 mint@22%`, `0 10 40 mint@32%`. */
    val GlowEdge = SageMint.copy(alpha = 0.22f)
    val GlowAmbient = SageMint.copy(alpha = 0.32f)

    /** Active "liked" tint in the Shorts rail. */
    val Like = SageMint
}
