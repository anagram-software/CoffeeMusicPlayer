package com.udeshcoffee.android.data.remote

import android.util.Log
import io.reactivex.observers.DisposableObserver
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException
import java.lang.Exception
import java.net.SocketTimeoutException

/**
 * Created by Udathari on 9/27/2017.
 */
abstract class CallbackWrapper<T> : DisposableObserver<T>() {

    protected abstract fun onSuccess(t: T)

    override fun onNext(t: T) {
        onSuccess(t)
    }

    override fun onError(e: Throwable) {
        when (e) {
            is HttpException -> {
                val responseBody = e.response().errorBody()
                Log.d("CallbackWrapper", "HttpException ${e.response()}" + responseBody?.let { getErrorMessage(it) })
            }
            is SocketTimeoutException -> Log.d("CallbackWrapper", "SocketTimeoutException")
            is IOException -> Log.d("CallbackWrapper", "SocketTimeoutException")
            else -> Log.d("CallbackWrapper", "$e")
        }
    }

    override fun onComplete() {

    }

    private fun getErrorMessage(responseBody: ResponseBody): String? {
        return try {
            val jsonObject = JSONObject(responseBody.string());
            jsonObject.getString("message")
        } catch (e: Exception) {
            e.message
        }
    }
}