package com.udeshcoffee.android.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.udeshcoffee.android.service.MusicBinder
import com.udeshcoffee.android.service.MusicService
import java.util.*

/**
 * Created by Udathari on 8/22/2017.
 */
object ServiceConnectionUtil {

    val TAG = "ServiceConnectionUtil"

    var binder : MusicBinder? = null
    private var serviceBinders : WeakHashMap<Context, ServiceBinder> = WeakHashMap()

    fun bind(context: Context, callback: ServiceConnection?) : ServiceConnectionToken? {
        Log.d(TAG, "bind")
        val intent = Intent(context, MusicService::class.java)
        context.startService(intent)
        val serviceBinder = ServiceBinder(callback)
        if (context.bindService(intent, serviceBinder, 0)){
            Log.d(TAG, "binded")
            serviceBinders.put(context, serviceBinder)
            return ServiceConnectionToken(context)
        }
        return null
    }

    fun unbind(serviceConnectionToken: ServiceConnectionToken) {
        val serviceBinder = serviceBinders.remove(serviceConnectionToken.context)
        serviceBinder?.let { serviceConnectionToken.context.unbindService(it) }
        if (serviceBinders.isEmpty())
            binder = null
    }

    class ServiceBinder(private val callback: ServiceConnection?) : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {

            binder = service as MusicBinder

            callback?.onServiceConnected(className, service)
        }

        override fun onServiceDisconnected(className: ComponentName) {
            callback?.onServiceDisconnected(className)
            binder = null
        }
    }

    class ServiceConnectionToken(val context: Context)
}