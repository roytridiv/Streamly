package com.tridivroy.streamly.core.di

import com.tridivroy.streamly.core.media.DownloadTracker
import com.tridivroy.streamly.data.local.datastore.UserPreferencesRepository
import com.tridivroy.streamly.domain.repository.DownloadRepository
import com.tridivroy.streamly.domain.repository.PreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Bindings for the repositories that are still Android's own: preferences sit on DataStore and
 * downloads sit on Media3, neither of which has a multiplatform equivalent here. The video
 * repository moved to `:shared` and is bridged in [SharedBridgeModule].
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPreferencesRepository(impl: UserPreferencesRepository): PreferencesRepository

    @Binds
    @Singleton
    abstract fun bindDownloadRepository(impl: DownloadTracker): DownloadRepository
}
