package com.tridivroy.streamly.core.network

import android.util.Log
import com.tridivroy.streamly.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideHttpClient(json: Json): HttpClient = HttpClient(CIO) {
        expectSuccess = true

        defaultRequest {
            url(BuildConfig.BASE_URL)
        }

        install(ContentNegotiation) {
            json(json)
        }

        install(HttpTimeout) {
            connectTimeoutMillis = 15_000
            requestTimeoutMillis = 30_000
            socketTimeoutMillis = 30_000
        }

        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    Log.d("HttpClient", message)
                }
            }
            level = if (BuildConfig.DEBUG) LogLevel.BODY else LogLevel.NONE
        }
    }
}
