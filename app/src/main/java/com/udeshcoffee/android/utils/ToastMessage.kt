package com.udeshcoffee.android.utils

import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.Observer

class ToastMessage: SingleLiveEvent<String>() {

    fun observe(owner: LifecycleOwner, observer: ToastObserver) {
        super.observe(owner, Observer {
            it?.let { it1 -> observer.onNewMessage(it1) }
        })
    }

    fun setMessage(message: String) {
        value = message
    }

    interface ToastObserver {
        fun onNewMessage(message: String)
    }

}