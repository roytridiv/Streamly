package com.tridivroy.streamly.core.storage

import android.content.Context
import android.os.StatFs
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** A reading of the volume Streamly's download cache lives on. All figures in bytes. */
data class StorageUsage(
    val totalBytes: Long = 0L,
    val freeBytes: Long = 0L,
    /** What Streamly's own downloads occupy. */
    val streamlyBytes: Long = 0L,
) {
    /** Everything on the volume that is neither free nor Streamly's. */
    val otherBytes: Long get() = (totalBytes - freeBytes - streamlyBytes).coerceAtLeast(0L)

    val usedBytes: Long get() = (totalBytes - freeBytes).coerceAtLeast(0L)

    /** 0f–1f shares for the segmented bar, so the UI does no arithmetic. */
    val streamlyFraction: Float get() = fractionOf(streamlyBytes)
    val otherFraction: Float get() = fractionOf(otherBytes)

    private fun fractionOf(bytes: Long): Float =
        if (totalBytes > 0) (bytes.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f

    val isKnown: Boolean get() = totalBytes > 0
}

/**
 * Reads the device's storage figures for the Downloads screen's usage card.
 *
 * The handoff mocks a fixed "40.8 / 64 GB"; this reports the real volume the download cache sits on,
 * via [StatFs] on the app's files directory. Streamly's own share is passed in by the caller, which
 * already has the download list, so this class owns no state.
 */
@Singleton
class DeviceStorage @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /** Hits the filesystem, so it runs off the main thread. */
    suspend fun read(streamlyBytes: Long): StorageUsage = withContext(Dispatchers.IO) {
        runCatching {
            val stats = StatFs(context.filesDir.absolutePath)
            StorageUsage(
                totalBytes = stats.blockSizeLong * stats.blockCountLong,
                freeBytes = stats.blockSizeLong * stats.availableBlocksLong,
                streamlyBytes = streamlyBytes,
            )
        }.getOrElse {
            // An unreadable volume means the card hides itself rather than showing zeroes.
            StorageUsage(streamlyBytes = streamlyBytes)
        }
    }
}
