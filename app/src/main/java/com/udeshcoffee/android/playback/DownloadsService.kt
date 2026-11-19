package com.udeshcoffee.android.playback

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.Scheduler
import com.udeshcoffee.android.R
import com.udeshcoffee.android.data.download.DownloadRepository
import com.udeshcoffee.android.ui.MainActivity
import org.koin.android.ext.android.inject

/**
 * Runs the downloads of [DownloadRepository] (a Media3 DownloadService: foreground while
 * downloading, with a progress notification). No scheduler: unfinished downloads resume when the
 * app next starts the service.
 */
@OptIn(UnstableApi::class)
class DownloadsService : DownloadService(
        NOTIFICATION_ID, DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL, CHANNEL_ID, R.string.downloads_channel_name, 0) {

    private val repository: DownloadRepository by inject()
    private val notifications by lazy { DownloadNotificationHelper(this, CHANNEL_ID) }

    override fun getDownloadManager(): DownloadManager = repository.manager

    override fun getScheduler(): Scheduler? = null

    override fun getForegroundNotification(downloads: MutableList<Download>, notMetRequirements: Int): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        return notifications.buildProgressNotification(this, R.drawable.ic_notification, open, null, downloads, notMetRequirements)
    }

    private companion object {
        const val NOTIFICATION_ID = 2
        const val CHANNEL_ID = "downloads"
    }
}
