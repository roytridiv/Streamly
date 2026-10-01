package com.tridivroy.streamly.core.di

import com.tridivroy.streamly.core.media.DownloadTracker
import com.tridivroy.streamly.data.local.datastore.UserPreferencesRepository
import com.tridivroy.streamly.data.repository.VideoRepositoryImpl
import com.tridivroy.streamly.domain.repository.DownloadRepository
import com.tridivroy.streamly.domain.repository.PreferencesRepository
import com.tridivroy.streamly.domain.repository.VideoRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindVideoRepository(impl: VideoRepositoryImpl): VideoRepository

    @Binds
    @Singleton
    abstract fun bindPreferencesRepository(impl: UserPreferencesRepository): PreferencesRepository

    @Binds
    @Singleton
    abstract fun bindDownloadRepository(impl: DownloadTracker): DownloadRepository
}
