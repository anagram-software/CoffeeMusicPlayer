package com.udeshcoffee.android.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.util.Log
import android.widget.Toast
import com.udeshcoffee.android.R
import com.udeshcoffee.android.data.DataRepository
import com.udeshcoffee.android.data.MediaRepository
import com.udeshcoffee.android.utils.isNetworkAvailable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.koin.android.ext.android.inject


/**
 * Created by Udathari on 11/20/2017.
 */
class CollectionService : Service() {

    val TAG = "CollectionService"

    val dataRepository: DataRepository by inject()
    val mediaRepository: MediaRepository by inject()

    lateinit var mBuilder: NotificationCompat.Builder
    private var channel: NotificationChannel? = null
    private var collectedCount = 0
    private var isCollecting = false

    var lyricDisposable: Disposable? = null

    companion object {
        val ACTION_COLLECT_LYRICS = "com.udeshcoffee.android.collectionservice.ACTION_COLLECT_LYRICS"
        val ACTION_STOP = "com.udeshcoffee.android.collectionservice.ACTION_STOP"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")

        channel = createNotificationChannel()

        // Actions
        val intent = Intent(ACTION_STOP)
        val pendingIntent = PendingIntent.getService(this, 0, intent, 0)

        mBuilder = NotificationCompat.Builder(this)
        mBuilder.setSmallIcon(R.drawable.ic_download)
                .addAction(R.drawable.ic_close_white_24dp, getString(R.string.action_cancel),pendingIntent)
        channel?.let { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mBuilder.setChannelId(it.id)
        }}
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStart")
        when(intent?.action) {
            ACTION_COLLECT_LYRICS -> {
                if (isCollecting)
                    Toast.makeText(this, "Already collecting lyrics", Toast.LENGTH_SHORT).show()
                else {
                    if (isNetworkAvailable(this, false)) {
                        Toast.makeText(this, "Collecting lyrics in background", Toast.LENGTH_SHORT).show()
                        collectLyrics()
                    } else {
                        Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show()
                        stopSelf()
                    }
                }
            }
            ACTION_STOP -> {
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(p0: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        stopForeground(false)
        disposeLyricCollection()
    }

    /* Lyric Collection */

    private fun collectLyrics() {
        Log.d(TAG, "collectLyrics")
        disposeLyricCollection()
        isCollecting = true
        collectedCount = 0
        createNotification(ACTION_COLLECT_LYRICS, collectedCount, false)
        lyricDisposable = mediaRepository.getSongs()
                .take(1)
                .flatMapIterable{it -> it}
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.newThread())
                .subscribe({
                    val isCollected = dataRepository.downloadLyricsIfMissing(it)
                    if (isCollected){
                        collectedCount++
                        createNotification(ACTION_COLLECT_LYRICS, collectedCount, false)
                    }
                },{},{
                    Log.d(TAG, "finish collection")
                    createNotification(ACTION_COLLECT_LYRICS, collectedCount, true)
                    stopSelf()
                })
    }

    private fun disposeLyricCollection() {
        isCollecting = false
        lyricDisposable?.let {
            if (!it.isDisposed)
                it.dispose()
        }
    }

    private fun createNotification(action: String, progress: Int, isFinished: Boolean) {
        Log.d(TAG, "createNotification")
        val id = 2018
        when (action) {
            ACTION_COLLECT_LYRICS -> {
                mBuilder.setContentTitle("Collecting Lyrics")
                if (progress == 0)
                    mBuilder.setContentText("Searching missing lyrics")
                else
                    mBuilder.setContentText("$progress lyrics collected")
                if (isFinished) {
                    mBuilder.setProgress(0, 0, false)
                } else {
                    mBuilder.setProgress(0, 0, true)
                }
            }
        }
        val notification = mBuilder.build()
        startForeground(id, notification)
    }

    private fun createNotificationChannel(): NotificationChannel? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            val id = "coffee_player_collection_channel"
            val name = "Collection Service"
            val description = "Collect data of Coffee Player"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(id, name, importance)

            channel.description = description
            channel.enableLights(false)
            channel.enableVibration(false)
            val notificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            return channel
        } else {
            return null
        }
    }


}
