package com.tridivroy.streamly.presentation.common

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/*
 * Inset helpers.
 *
 * The app draws edge to edge, so video reaches the screen edges on purpose. What must never reach
 * them is anything the user taps. Landscape is where this bites: the status bar shrinks to nothing
 * but a cutout or a side navigation bar appears on one edge, and `statusBarsPadding()` alone leaves
 * content sitting under it.
 *
 * Hence `safeDrawing` (system bars + cutout) for ordinary chrome, and `safeContent` for controls
 * drawn over fullscreen video, where the bars are hidden and `safeDrawing` collapses to zero while
 * the gesture regions are still very much live.
 */

/**
 * Top and side padding for a screen's own chrome: clears the status bar, and in landscape a display
 * cutout or side navigation bar.
 *
 * Bottom is deliberately excluded — the bottom bar handles it and consumes it for the content above.
 */
@Composable
fun Modifier.safeTopPadding(): Modifier = windowInsetsPadding(
    WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
)

/** Bottom and side padding, for content that runs to the bottom of the window. */
@Composable
fun Modifier.safeBottomPadding(): Modifier = windowInsetsPadding(
    WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
)

/**
 * Padding for controls drawn on top of fullscreen video.
 *
 * Uses `safeContent` (= `safeDrawing` + `safeGestures`) rather than `safeDrawing`, because fullscreen
 * hides the system bars: `safeDrawing` then reports zero while the home-indicator strip and the
 * back-gesture edges are still intercepting touches. `safeContent` keeps buttons and the scrubber out
 * of regions the system will take for itself.
 */
@Composable
fun Modifier.safeOverlayPadding(sides: WindowInsetsSides): Modifier = windowInsetsPadding(
    WindowInsets.safeContent.only(sides),
)
