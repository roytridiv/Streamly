package com.tridivroy.streamly.core.media

import android.app.Notification
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.PlatformScheduler
import androidx.media3.exoplayer.scheduler.Scheduler
import com.tridivroy.streamly.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Foreground service that runs the app-wide [DownloadManager], so downloads keep going when the
 * app is backgrounded. [PlatformScheduler] restarts it when requirements (network) come back,
 * including after a reboot.
 */
@OptIn(UnstableApi::class)
@AndroidEntryPoint
class MediaDownloadService : DownloadService(
    FOREGROUND_NOTIFICATION_ID,
    DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
    CHANNEL_ID,
    R.string.download_channel_name,
    R.string.download_channel_description,
) {
    // Field-injected by Hilt before DownloadService.onCreate() calls getDownloadManager().
    @Inject lateinit var injectedDownloadManager: DownloadManager

    private val notificationHelper by lazy { DownloadNotificationHelper(this, CHANNEL_ID) }

    override fun getDownloadManager(): DownloadManager = injectedDownloadManager

    override fun getScheduler(): Scheduler = PlatformScheduler(this, JOB_ID)

    override fun getForegroundNotification(downloads: MutableList<Download>, notMetRequirements: Int): Notification =
        notificationHelper.buildProgressNotification(
            this,
            android.R.drawable.stat_sys_download,
            /* contentIntent = */ null,
            /* message = */ null,
            downloads,
            notMetRequirements,
        )

    private companion object {
        const val FOREGROUND_NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "downloads"
        const val JOB_ID = 1
    }
}
