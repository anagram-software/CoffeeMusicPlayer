package com.udeshcoffee.android.utils

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer

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