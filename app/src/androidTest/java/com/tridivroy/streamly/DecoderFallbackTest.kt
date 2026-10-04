package com.tridivroy.streamly

import android.content.Context
import android.graphics.SurfaceTexture
import android.util.Log
import android.view.Surface
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tridivroy.streamly.core.di.MediaModule
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference

/**
 * Regression test for the "Decoder init failed: OMX.qcom.video.decoder.avc" crash: Tears of Steel's
 * lowest HLS rendition (224x100, avc1.42C00D) is advertised as supported by some Qualcomm hardware
 * decoders, which then refuse to configure for it. The app's renderers must fall back to another
 * decoder and keep playing.
 *
 * Plays that rendition directly (ABR only drops to it under bandwidth pressure, e.g. while a download
 * runs), so the test is deterministic. Needs network.
 */
@UnstableApi
@RunWith(AndroidJUnit4::class)
class DecoderFallbackTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun lowestTearsOfSteelRendition_playsWithAppRenderers() {
        val result = play(MediaModule.provideRenderersFactory(context))

        assertNull("Playback failed: ${result.error?.errorCodeName}", result.error)
        assertTrue("First frame never rendered", result.renderedFirstFrame)
        assertTrue("Position only reached ${result.positionMs} ms", result.positionMs >= TARGET_POSITION_MS)
    }

    private class Result(
        val error: PlaybackException?,
        val renderedFirstFrame: Boolean,
        val positionMs: Long,
        val videoDecoders: List<String>,
    )

    private fun play(renderersFactory: RenderersFactory): Result {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val error = AtomicReference<PlaybackException?>()
        val decoders = mutableListOf<String>()
        var renderedFirstFrame = false
        lateinit var player: ExoPlayer
        lateinit var surfaceTexture: SurfaceTexture
        lateinit var surface: Surface

        instrumentation.runOnMainSync {
            surfaceTexture = SurfaceTexture(0)
            surface = Surface(surfaceTexture)
            player = ExoPlayer.Builder(context, renderersFactory).build().apply {
                volume = 0f
                setVideoSurface(surface)
                addListener(object : Player.Listener {
                    override fun onPlayerError(e: PlaybackException) = error.set(e)
                    override fun onRenderedFirstFrame() {
                        renderedFirstFrame = true
                    }
                })
                addAnalyticsListener(object : AnalyticsListener {
                    override fun onVideoDecoderInitialized(
                        eventTime: AnalyticsListener.EventTime,
                        decoderName: String,
                        initializedTimestampMs: Long,
                        initializationDurationMs: Long,
                    ) {
                        decoders += decoderName
                    }
                })
                setMediaItem(MediaItem.fromUri(LOWEST_RENDITION_URL))
                prepare()
                play()
            }
        }

        var positionMs = 0L
        val deadline = System.currentTimeMillis() + TIMEOUT_MS
        while (System.currentTimeMillis() < deadline && error.get() == null && positionMs < TARGET_POSITION_MS) {
            Thread.sleep(POLL_MS)
            instrumentation.runOnMainSync { positionMs = player.currentPosition }
        }

        instrumentation.runOnMainSync {
            player.release()
            surface.release()
            surfaceTexture.release()
        }
        Log.i(TAG, "decoders=$decoders error=${error.get()?.errorCodeName} firstFrame=$renderedFirstFrame position=$positionMs")
        return Result(error.get(), renderedFirstFrame, positionMs, decoders)
    }

    private companion object {
        const val TAG = "DecoderFallbackTest"
        const val LOWEST_RENDITION_URL =
            "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/" +
                "tears-of-steel.ism/tears-of-steel-audio_eng=64008-video_eng=401000.m3u8"
        const val TARGET_POSITION_MS = 3_000L
        const val TIMEOUT_MS = 45_000L
        const val POLL_MS = 250L
    }
}
