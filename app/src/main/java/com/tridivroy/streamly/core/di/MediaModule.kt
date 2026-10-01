package com.tridivroy.streamly.core.di

import android.content.Context
import androidx.annotation.OptIn
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MediaModule {

    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 30_000
    private const val SEEK_INCREMENT_MS = 10_000L

    /** Network source shared by playback and the download manager. */
    @OptIn(UnstableApi::class)
    @Provides
    @Singleton
    fun provideHttpDataSourceFactory(): HttpDataSource.Factory =
        DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(CONNECT_TIMEOUT_MS)
            .setReadTimeoutMs(READ_TIMEOUT_MS)

    /**
     * Picks the right source per item: `.m3u8` URLs (or MimeTypes.APPLICATION_M3U8)
     * become HLS sources via media3-exoplayer-hls, everything else progressive.
     *
     * Reads go through the download cache first (read-only: streaming never writes to it), so any
     * downloaded video plays offline from every player that uses this factory.
     */
    @OptIn(UnstableApi::class)
    @Provides
    @Singleton
    fun provideMediaSourceFactory(
        @ApplicationContext context: Context,
        httpDataSourceFactory: HttpDataSource.Factory,
        downloadCache: Cache,
    ): MediaSource.Factory {
        val upstreamFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(downloadCache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setCacheWriteDataSinkFactory(null)
        return DefaultMediaSourceFactory(cacheDataSourceFactory)
    }

    /**
     * App-wide player. Screens must not release it; they set media items and
     * play/pause. It pauses automatically when the whole app goes to background.
     *
     * Must be first injected on the main thread: the player binds to the
     * creating thread's looper, and the lifecycle observer must be added there.
     */
    @OptIn(UnstableApi::class)
    @Provides
    @Singleton
    fun provideExoPlayer(
        @ApplicationContext context: Context,
        mediaSourceFactory: MediaSource.Factory,
    ): ExoPlayer {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        val player = ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setSeekBackIncrementMs(SEEK_INCREMENT_MS)
            .setSeekForwardIncrementMs(SEEK_INCREMENT_MS)
            .build()

        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                player.pause()
            }
        })

        return player
    }
}
