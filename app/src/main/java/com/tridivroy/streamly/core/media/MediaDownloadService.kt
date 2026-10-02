package com.tridivroy.streamly.core.media

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.PlatformScheduler
import androidx.media3.exoplayer.scheduler.Scheduler
import com.tridivroy.streamly.MainActivity
import com.tridivroy.streamly.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.math.roundToInt

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

    override fun getDownloadManager(): DownloadManager = injectedDownloadManager

    override fun getScheduler(): Scheduler = PlatformScheduler(this, JOB_ID)

    /**
     * Built by hand rather than with `DownloadNotificationHelper`.
     *
     * The helper returns a finished [Notification], and actions cannot be appended to one after the
     * fact — so pausing or cancelling a download meant opening the app. This builds the same progress
     * notification and adds Pause/Resume and Cancel, which is the whole point of a download running in
     * the background.
     */
    override fun getForegroundNotification(
        downloads: MutableList<Download>,
        notMetRequirements: Int,
    ): Notification {
        val paused = downloads.isNotEmpty() && downloads.all { it.stopReason == DownloadTracker.STOP_REASON_PAUSED }
        val percent = downloads
            .map { it.percentDownloaded }
            .filter { it >= 0f }
            .takeIf { it.isNotEmpty() }
            ?.average()
            ?.roundToInt()

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(getString(R.string.download_notification_title))
            .setContentText(notificationText(downloads.size, paused, percent))
            .setContentIntent(openAppIntent())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (percent != null) {
            builder.setProgress(100, percent, /* indeterminate = */ false)
        } else {
            builder.setProgress(0, 0, /* indeterminate = */ true)
        }

        /*
         * Pause and resume go through the stop-reason path with a null id (= every download), not
         * `buildPauseDownloadsIntent`.
         *
         * `ACTION_PAUSE_DOWNLOADS` flips a manager-level flag that leaves each download reporting
         * `STATE_QUEUED` with no stop reason: on device the download simply kept going and the button
         * never flipped to Resume, because nothing distinguished it from a download waiting its turn.
         * A stop reason is the same mechanism the Downloads screen uses, so the two agree and the
         * paused state is visible in both.
         */
        if (paused) {
            builder.addAction(
                android.R.drawable.ic_media_play,
                getString(R.string.download_notification_resume),
                serviceIntent(stopReasonIntent(Download.STOP_REASON_NONE)),
            )
        } else {
            builder.addAction(
                android.R.drawable.ic_media_pause,
                getString(R.string.download_notification_pause),
                serviceIntent(stopReasonIntent(DownloadTracker.STOP_REASON_PAUSED)),
            )
        }
        builder.addAction(
            android.R.drawable.ic_menu_close_clear_cancel,
            getString(R.string.download_notification_cancel),
            serviceIntent(buildRemoveAllDownloadsIntent(this, MediaDownloadService::class.java, false)),
        )

        return builder.build()
    }

    /** A null `id` applies the stop reason to every download. */
    private fun stopReasonIntent(stopReason: Int): Intent =
        buildSetStopReasonIntent(
            /* context = */ this,
            /* clazz = */ MediaDownloadService::class.java,
            /* id = */ null,
            /* stopReason = */ stopReason,
            /* foreground = */ false,
        )

    private fun notificationText(count: Int, paused: Boolean, percent: Int?): String = when {
        paused -> getString(R.string.download_notification_paused, count)
        percent != null -> getString(R.string.download_notification_progress, count, percent)
        else -> getString(R.string.download_notification_preparing, count)
    }

    /**
     * The service is already in the foreground when this notification is showing, so a command sent to
     * it is allowed even on O+ background-start restrictions.
     */
    private fun serviceIntent(intent: Intent): PendingIntent =
        PendingIntent.getService(this, requestCode(intent), intent, pendingIntentFlags())

    /**
     * Pause and resume share one action string, so they need different request codes or the second
     * `PendingIntent` would reuse the first one's extras and the button would stop working.
     */
    private fun requestCode(intent: Intent): Int =
        intent.action.hashCode() xor intent.getIntExtra(KEY_STOP_REASON, Int.MIN_VALUE)

    private fun openAppIntent(): PendingIntent =
        PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            pendingIntentFlags(),
        )

    private fun pendingIntentFlags(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

    private companion object {
        const val FOREGROUND_NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "downloads"
        const val JOB_ID = 1
    }
}
