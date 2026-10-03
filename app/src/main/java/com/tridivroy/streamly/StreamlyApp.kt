package com.tridivroy.streamly

import android.app.Application
import com.tridivroy.streamly.core.di.SharedModule
import com.tridivroy.streamly.core.media.NowPlayingStore
import com.tridivroy.streamly.core.media.ShortsPlayerPool
import com.tridivroy.streamly.domain.repository.DownloadRepository
import com.tridivroy.streamly.domain.repository.PreferencesRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import javax.inject.Provider

@HiltAndroidApp
class StreamlyApp : Application() {

    /*
     * Providers, not instances. Between them these pull in the Media3 download manager with its
     * database and disk cache, two ExoPlayers, the shared player and DataStore. Building all of
     * that while the Application is still constructing would be slow and fragile; each Provider is
     * only called the first time a shared screen actually needs that collaborator.
     *
     * ShortsPlayerPool is deliberately unscoped — each Shorts ViewModel gets its own pool and
     * releases it — so `get()` returns a fresh pool per call, which is the existing contract.
     */
    @Inject lateinit var downloadRepository: Provider<DownloadRepository>

    @Inject lateinit var preferencesRepository: Provider<PreferencesRepository>

    @Inject lateinit var shortsPlayerPool: Provider<ShortsPlayerPool>

    @Inject lateinit var nowPlayingStore: Provider<NowPlayingStore>

    override fun onCreate() {
        super.onCreate()
        /*
         * The shared layer is started here rather than lazily, because `BuildConfig` is the one
         * thing `:shared` cannot see — it is generated per Android module. Starting it in
         * `onCreate` means the base URL, the log level and every platform collaborator are
         * registered before any injection or composition can read them.
         */
        SharedModule.start(
            config = SharedModule.SharedConfig(
                baseUrl = BuildConfig.BASE_URL,
                // Bodies can carry user data, so they are only logged in debug builds.
                logBodies = BuildConfig.DEBUG,
            ),
            downloads = { downloadRepository.get() },
            shortsPlayers = { shortsPlayerPool.get() },
            nowPlaying = { nowPlayingStore.get() },
            preferences = { preferencesRepository.get() },
        )
    }
}
