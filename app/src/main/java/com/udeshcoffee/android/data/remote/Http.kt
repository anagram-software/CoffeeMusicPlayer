package com.udeshcoffee.android.data.remote

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** The app's one OkHttp client (connection pool, TLS setup). Touch it off the main thread the first time. */
object Http {
    val client: OkHttpClient by lazy {
        OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()
    }
}
