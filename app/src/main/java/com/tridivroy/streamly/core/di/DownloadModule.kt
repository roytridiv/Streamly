package com.tridivroy.streamly.core.di

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.offline.DownloadManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import java.util.concurrent.Executors
import javax.inject.Singleton

@OptIn(UnstableApi::class)
@Module
@InstallIn(SingletonComponent::class)
object DownloadModule {

    private const val DOWNLOAD_DIRECTORY = "downloads"
    private const val DOWNLOAD_THREADS = 4
    private const val MAX_PARALLEL_DOWNLOADS = 2

    /** Holds the download index and the cache's own index. */
    @Provides
    @Singleton
    fun provideDatabaseProvider(@ApplicationContext context: Context): DatabaseProvider =
        StandaloneDatabaseProvider(context)

    /**
     * Downloaded media. Never evicts: files stay until the user removes the download. Only one
     * SimpleCache may exist per directory, hence the singleton. Stored in `filesDir` so the OS
     * doesn't clear it like `cacheDir`.
     */
    @Provides
    @Singleton
    fun provideDownloadCache(
        @ApplicationContext context: Context,
        databaseProvider: DatabaseProvider,
    ): Cache = SimpleCache(File(context.filesDir, DOWNLOAD_DIRECTORY), NoOpCacheEvictor(), databaseProvider)

    @Provides
    @Singleton
    fun provideDownloadManager(
        @ApplicationContext context: Context,
        databaseProvider: DatabaseProvider,
        downloadCache: Cache,
        httpDataSourceFactory: HttpDataSource.Factory,
    ): DownloadManager =
        DownloadManager(
            context,
            databaseProvider,
            downloadCache,
            httpDataSourceFactory,
            // Fetches HLS segments in parallel within one download.
            Executors.newFixedThreadPool(DOWNLOAD_THREADS),
        ).apply { maxParallelDownloads = MAX_PARALLEL_DOWNLOADS }
}
