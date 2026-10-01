package com.tridivroy.streamly.core.media

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import com.tridivroy.streamly.domain.model.Video
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Two-player pool for the Shorts pager: page `i` always plays on player `i % 2`. While one player
 * plays the active page, the other has the next page prepared and paused on its first frame, so a
 * forward swipe starts instantly. Never more than [POOL_SIZE] players exist, and only one plays.
 *
 * Unscoped: each owner gets its own pool and must call [release]. Create on the main thread.
 */
@OptIn(UnstableApi::class)
class ShortsPlayerPool @Inject constructor(
    @ApplicationContext context: Context,
    mediaSourceFactory: MediaSource.Factory,
) {
    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
        .build()

    private val players: List<ExoPlayer> = List(POOL_SIZE) {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            .build()
            .apply { repeatMode = Player.REPEAT_MODE_ONE }
    }

    /** Page index loaded into each player slot, or `-1` when empty. */
    private val assignedIndex = IntArray(POOL_SIZE) { NO_INDEX }

    /** The player showing [index], or `null` if that page isn't loaded into the pool. */
    fun playerFor(index: Int): Player? =
        players[slotOf(index)].takeIf { assignedIndex[slotOf(index)] == index }

    /**
     * Plays [activeIndex] and preloads its neighbour (next page, or previous on the last page).
     * @return the preloaded index, or `null` if there is no neighbour.
     */
    fun activate(activeIndex: Int, videos: List<Video>): Int? {
        load(activeIndex, videos).apply { playWhenReady = true }

        val preloadIndex = when {
            activeIndex + 1 < videos.size -> activeIndex + 1
            activeIndex > 0 -> activeIndex - 1
            else -> null
        }
        preloadIndex?.let { index ->
            load(index, videos).apply {
                playWhenReady = false
                seekToDefaultPosition()
            }
        }
        return preloadIndex
    }

    fun play(index: Int) {
        playerFor(index)?.play()
    }

    fun pauseAll() {
        players.forEach { it.pause() }
    }

    fun release() {
        players.forEach { it.release() }
        assignedIndex.fill(NO_INDEX)
    }

    /** Ensures [index] is loaded into its slot; reuses the slot untouched if it already holds it. */
    private fun load(index: Int, videos: List<Video>): ExoPlayer {
        val slot = slotOf(index)
        val player = players[slot]
        if (assignedIndex[slot] != index) {
            player.setMediaItem(videos[index].toMediaItem())
            player.prepare()
            assignedIndex[slot] = index
        }
        return player
    }

    private fun slotOf(index: Int) = index % POOL_SIZE

    private companion object {
        const val POOL_SIZE = 2
        const val NO_INDEX = -1
    }
}
