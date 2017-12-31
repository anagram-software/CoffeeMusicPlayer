package com.udeshcoffee.android.ui

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.media.AudioManager
import android.os.Bundle
import android.os.IBinder
import android.preference.PreferenceManager
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import com.greysonparrelli.permiso.Permiso
import com.udeshcoffee.android.bindToService
import com.udeshcoffee.android.service.MusicService
import com.udeshcoffee.android.unbindFromService
import com.udeshcoffee.android.utils.ServiceConnectionUtil.ServiceConnectionToken

/**
 * Created by Udathari on 8/25/2017.
 */

open class BaseActivity: AppCompatActivity(), ServiceConnection {

    open val TAG = "BaseActivity"

    lateinit var sharedPreferences: SharedPreferences
    private var serviceToken: ServiceConnectionToken? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        volumeControlStream = AudioManager.STREAM_MUSIC

        Permiso.getInstance().setActivity(this)
        Permiso.getInstance().requestPermissions(object : Permiso.IOnPermissionResult {
            override fun onPermissionResult(resultSet: Permiso.ResultSet) {
                if (resultSet.areAllPermissionsGranted()) {
                    bindToService()
                } else {
                    Toast.makeText(this@BaseActivity, "Permission check failed", Toast.LENGTH_LONG).show()
                    finish()
                }
            }

            override fun onRationaleRequested(callback: Permiso.IOnRationaleProvided, vararg permissions: String) {
                callback.onRationaleProvided()
            }
        }, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.WAKE_LOCK,
                Manifest.permission.READ_EXTERNAL_STORAGE)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
    }

    override fun onResume() {
        super.onResume()
        Permiso.getInstance().setActivity(this)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Permiso.getInstance().onRequestPermissionResult(requestCode, permissions, grantResults)
    }

    fun bindToService() {
        serviceToken = bindToService(this)
        serviceToken?.let { Log.d(TAG, "serviceToken") }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceToken?.let { unbindFromService(it) }
    }

    override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
        Log.d(TAG, "connected")
        LocalBroadcastManager.getInstance(this).sendBroadcast(Intent().setAction(MusicService.InternalIntents.SERVICE_CONNECTED))
    }

    override fun onServiceDisconnected(p0: ComponentName?) {}
}