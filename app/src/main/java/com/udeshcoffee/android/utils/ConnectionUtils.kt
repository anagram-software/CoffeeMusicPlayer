package com.udeshcoffee.android.utils

import android.content.Context
import android.net.ConnectivityManager

/**
 * Created by Udathari on 9/15/2017.
 */
fun isNetworkAvailable(context: Context, shouldCheckWifi: Boolean): Boolean {
    return try {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = cm.activeNetworkInfo
        // if no network is available networkInfo will be null
        // otherwise check if we are connected
        if (!shouldCheckWifi)
            networkInfo != null && networkInfo.isConnectedOrConnecting
        else
            networkInfo != null && networkInfo.isConnectedOrConnecting && networkInfo.type == ConnectivityManager.TYPE_WIFI
    } catch (ex: Throwable) {
        false
    }

}