package com.udeshcoffee.android.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.udeshcoffee.android.R
import com.udeshcoffee.android.data.DataRepository
import com.udeshcoffee.android.data.MediaRepository
import com.udeshcoffee.android.utils.isNetworkAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Downloads missing lyrics for the whole library in the background.
 * Replaces the old CollectionService: long-running work runs as a WorkManager job with a
 * `dataSync` foreground notification, which is what recent Android versions allow.
 */
class LyricsCollectionWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params), KoinComponent {

    private val dataRepository: DataRepository by inject()
    private val mediaRepository: MediaRepository by inject()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        createChannel(applicationContext)
        setForeground(foregroundInfo(collected = 0))

        val songs = mediaRepository.loadSongs()
        var collected = 0
        for (song in songs) {
            if (isStopped) break
            val found = try {
                dataRepository.downloadLyricsIfMissing(song)
            } catch (e: Exception) {
                Log.w(TAG, "Lyrics lookup failed for ${song.id}", e)
                false
            }
            if (found) {
                collected++
                setForeground(foregroundInfo(collected))
            }
        }
        if (!isStopped) notifyFinished(collected)
        Result.success()
    }

    private fun baseNotification(): NotificationCompat.Builder =
            NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_download)
                    .setContentTitle("Collecting Lyrics")
                    .setOnlyAlertOnce(true)

    private fun foregroundInfo(collected: Int): ForegroundInfo {
        val notification = baseNotification()
                .setContentText(if (collected == 0) "Searching missing lyrics" else "$collected lyrics collected")
                .setProgress(0, 0, true)
                .setOngoing(true)
                .addAction(R.drawable.ic_close_white_24dp, applicationContext.getString(R.string.action_cancel),
                        WorkManager.getInstance(applicationContext).createCancelPendingIntent(id))
                .build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        else
            ForegroundInfo(NOTIFICATION_ID, notification)
    }

    private fun notifyFinished(collected: Int) {
        val notification = baseNotification()
                .setContentText("$collected lyrics collected")
                .build()
        try {
            NotificationManagerCompat.from(applicationContext).notify(FINISHED_NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Notification permission not granted.
        }
    }

    companion object {
        private const val TAG = "LyricsCollection"
        private const val WORK_NAME = "collect_lyrics"
        private const val CHANNEL_ID = "coffee_player_collection_channel"
        private const val NOTIFICATION_ID = 2018
        private const val FINISHED_NOTIFICATION_ID = 2019

        /** Starts collecting unless a collection is already running. */
        fun start(context: Context) {
            if (!isNetworkAvailable(context, false)) {
                Toast.makeText(context, "No internet connection", Toast.LENGTH_SHORT).show()
                return
            }
            val request = OneTimeWorkRequestBuilder<LyricsCollectionWorker>()
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .build()
            WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request)
            Toast.makeText(context, "Collecting lyrics in background", Toast.LENGTH_SHORT).show()
        }

        private fun createChannel(context: Context) {
            val channel = NotificationChannel(CHANNEL_ID, "Collection Service", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Collect data of Coffee Player"
                enableLights(false)
                enableVibration(false)
            }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
