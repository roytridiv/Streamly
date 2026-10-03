package com.tridivroy.streamly

import android.app.Application
import com.tridivroy.streamly.core.di.SharedModule
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class StreamlyApp : Application() {

    override fun onCreate() {
        super.onCreate()
        /*
         * The shared layer is started here rather than lazily from Hilt, because `BuildConfig` is
         * the one thing `:shared` cannot see: it is generated per Android module. Starting it in
         * `onCreate` means the base URL and the log level are set before any injection can read the
         * repository.
         */
        SharedModule.start(
            SharedModule.SharedConfig(
                baseUrl = BuildConfig.BASE_URL,
                // Bodies can carry user data, so they are only logged in debug builds.
                logBodies = BuildConfig.DEBUG,
            ),
        )
    }
}
