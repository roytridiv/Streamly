package com.tridivroy.streamly.core.media

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.tridivroy.streamly.domain.model.Video
import com.tridivroy.streamly.presentation.shorts.NowPlayingController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** What the mini-player shows: the video loaded in the shared player, and how far along it is. */
data class NowPlaying(
    val video: Video,
    val isPlaying: Boolean = false,
    /** 0f–1f. 0f until the duration is known. */
    val progress: Float = 0f,
)

/**
 * Reflects the state of the app-wide [ExoPlayer] so screens that are not the Player — Home and
 * Downloads, which show the mini-player — can display what is playing without owning the player.
 *
 * This store makes no playback decisions of its own beyond [togglePlay] and [pause]; it mirrors the
 * player and forgets the video as soon as the player moves on to a different item or stops. [PlayerViewModel]
 * is what tells it which [Video] a media ID belongs to, since a `MediaItem` only carries the ID.
 *
 * Must first be injected on the main thread — the player is bound to its creating thread's looper.
 */
@Singleton
class NowPlayingStore @Inject constructor(
    private val exoPlayer: ExoPlayer,
) : NowPlayingController {

    private val _nowPlaying = MutableStateFlow<NowPlaying?>(null)
    val nowPlaying: StateFlow<NowPlaying?> = _nowPlaying.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var ticker: Job? = null

    /** Videos the player has been handed, so a media ID can be resolved back to its metadata. */
    private val known = mutableMapOf<String, Video>()

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = sync()
            override fun onIsPlayingChanged(isPlaying: Boolean) = sync()
            override fun onPlaybackStateChanged(playbackState: Int) = sync()
        })
    }

    /** Called when a screen starts playing [video] on the shared player. */
    fun onPlaybackStarted(video: Video) {
        known[video.id] = video
        sync()
    }

    /** Mini-player play/pause. A no-op when nothing is loaded. */
    fun togglePlay() {
        if (exoPlayer.currentMediaItem == null) return
        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
    }

    /**
     * Stops playback and forgets the video, so the mini-player disappears.
     *
     * Clearing the media items is what removes the bar: [sync] drops the state as soon as the player
     * holds no item. This is the one place that ends playback outright -- everywhere else pauses.
     */
    fun dismiss() {
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        _nowPlaying.value = null
        stopTicking()
    }

    /**
     * Pauses the shared player, for when another surface takes over the audio — the Shorts pool, which
     * has players of its own. AGENTS.md allows 1-2 active players; this keeps the shared one quiet
     * rather than letting it bleed under a short.
     */
    override fun pause() {
        if (exoPlayer.isPlaying) exoPlayer.pause()
    }

    private fun sync() {
        val video = exoPlayer.currentMediaItem?.mediaId?.let(known::get)
        if (video == null) {
            _nowPlaying.value = null
            stopTicking()
            return
        }
        _nowPlaying.value = NowPlaying(
            video = video,
            isPlaying = exoPlayer.isPlaying,
            progress = currentProgress(),
        )
        if (exoPlayer.isPlaying) startTicking() else stopTicking()
    }

    /**
     * media3 has no position callback, so the progress line is polled — but only while something is
     * actually playing, and only twice a second, which is enough for a 2dp bar.
     */
    private fun startTicking() {
        if (ticker?.isActive == true) return
        ticker = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            while (true) {
                delay(PROGRESS_POLL_MS)
                val progress = currentProgress()
                _nowPlaying.update { current -> current?.copy(progress = progress) }
            }
        }
    }

    private fun stopTicking() {
        ticker?.cancel()
        ticker = null
    }

    private fun currentProgress(): Float {
        val duration = exoPlayer.duration
        if (duration <= 0) return 0f
        return (exoPlayer.currentPosition.toFloat() / duration).coerceIn(0f, 1f)
    }

    private companion object {
        const val PROGRESS_POLL_MS = 500L
    }
}
