package com.tridivroy.streamly.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.media3.common.Player
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/** A snapshot of where playback is, for scrubbers and progress lines. */
data class PlaybackProgress(
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
) {
    /** 0f–1f, or 0f while the duration is unknown (live, or still preparing). */
    val fraction: Float
        get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

    val positionSeconds: Long get() = positionMs / 1000
    val durationSeconds: Long get() = durationMs / 1000
}

/** How often the position is re-read while something is playing. */
const val DEFAULT_POLL_MS: Long = 500L

/**
 * Polls [handle] for its position while it is playing.
 *
 * media3 reports state changes through a listener but has no position callback, so a scrubber has to
 * poll. This keeps that in one place, ticks only while something is actually playing, and stops when
 * the composable leaves — so a paused or finished player costs nothing.
 */
@Composable
fun rememberPlaybackProgress(
    handle: Player?,
    pollIntervalMs: Long = DEFAULT_POLL_MS,
): State<PlaybackProgress> {
    val progress = remember { mutableStateOf(PlaybackProgress()) }

    // Snap to the player's current state immediately, and whenever it changes, so the UI is right
    // before the first tick and does not wait a poll interval to catch a pause.
    DisposableEffect(handle) {
        if (handle == null) {
            progress.value = PlaybackProgress()
            return@DisposableEffect onDispose {}
        }
        fun snapshot() {
            progress.value = PlaybackProgress(
                positionMs = handle.currentPosition.coerceAtLeast(0L),
                durationMs = handle.duration.takeIf { it > 0 } ?: 0L,
                isPlaying = handle.isPlaying,
                isBuffering = handle.playbackState == Player.STATE_BUFFERING,
            )
        }
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) = snapshot()
            override fun onPlaybackStateChanged(playbackState: Int) = snapshot()
            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int,
            ) = snapshot()
        }
        snapshot()
        handle.addListener(listener)
        onDispose { handle.removeListener(listener) }
    }

    val isPlaying = progress.value.isPlaying
    LaunchedEffect(handle, isPlaying, pollIntervalMs) {
        if (handle == null || !isPlaying) return@LaunchedEffect
        while (isActive) {
            delay(pollIntervalMs)
            progress.value = progress.value.copy(
                positionMs = handle.currentPosition.coerceAtLeast(0L),
                durationMs = handle.duration.takeIf { it > 0 } ?: 0L,
            )
        }
    }

    return progress
}
