package com.tridivroy.streamly.presentation.player.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/**
 * Visibility of the player's overlay controls — the centre play/pause button and the scrubber.
 *
 * Hoisted out of [FloatingPlayerViewport] so interactions that happen elsewhere on the screen can
 * also keep the controls up: seeking from a Key Moments chip is a deliberate interaction with
 * playback, and the overlay should behave as if the scrubber itself had been dragged.
 *
 * [interactionCount] is the auto-hide countdown's restart signal rather than a timestamp: using it as
 * a `LaunchedEffect` key means each new interaction cancels the pending hide and starts a fresh one,
 * with no clock arithmetic and no timer left running after the viewport leaves composition.
 */
@Stable
class PlayerControlsState(
    isVisible: Boolean = true,
    interactionCount: Int = 0,
) {

    var isVisible by mutableStateOf(isVisible)
        internal set

    var interactionCount by mutableIntStateOf(interactionCount)
        private set

    /** A tap on the video surface: show the controls, or hide them if they are already up. */
    fun toggle() {
        isVisible = !isVisible
        interactionCount++
    }

    /**
     * Any deliberate interaction with playback — scrubbing, seeking to a key moment, play/pause.
     * Shows the controls and restarts the countdown.
     */
    fun keepVisible() {
        isVisible = true
        interactionCount++
    }

    internal fun hide() {
        isVisible = false
    }

    internal companion object {
        /** Survives rotation so the controls do not pop back up mid-playback. */
        val Saver: Saver<PlayerControlsState, List<Any>> = Saver(
            save = { listOf(it.isVisible, it.interactionCount) },
            restore = { PlayerControlsState(it[0] as Boolean, it[1] as Int) },
        )
    }
}

@Composable
fun rememberPlayerControlsState(): PlayerControlsState =
    rememberSaveable(saver = PlayerControlsState.Saver) { PlayerControlsState() }

/** How long the controls stay up after the last interaction while the video is playing. */
const val CONTROLS_AUTO_HIDE_MS = 3_000L
