package com.tridivroy.streamly.core.storage

import android.content.Context
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Measures and clears Coil's thumbnail cache for the Profile screen's "Clear cache" row.
 *
 * Deliberately scoped to images only. Streamly's other cache is the Media3 download cache, and
 * clearing that would silently delete the viewer's offline videos — a destructive act hiding behind
 * an innocuous label. Downloads are removed from the Downloads screen, one row at a time, on purpose.
 *
 * Coil's `diskCache` is still experimental in Coil 2, so the opt-in is explicit here
 * rather than left as a warning: if the API changes, this one file is what needs revisiting.
 */
@OptIn(ExperimentalCoilApi::class)
@Singleton
class ImageCacheCleaner @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /** Current size of the image disk cache in bytes, or 0 when there is no disk cache. */
    suspend fun size(): Long = withContext(Dispatchers.IO) {
        runCatching { context.imageLoader.diskCache?.size ?: 0L }.getOrDefault(0L)
    }

    /** Empties the memory and disk caches, returning how much disk space was reclaimed. */
    suspend fun clear(): Long = withContext(Dispatchers.IO) {
        runCatching {
            val loader = context.imageLoader
            val before = loader.diskCache?.size ?: 0L
            loader.memoryCache?.clear()
            loader.diskCache?.clear()
            before - (loader.diskCache?.size ?: 0L)
        }.getOrDefault(0L)
    }
}
