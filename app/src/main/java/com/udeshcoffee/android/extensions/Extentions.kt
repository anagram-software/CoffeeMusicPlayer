package com.udeshcoffee.android.extensions

import android.content.ContentResolver
import android.content.ServiceConnection
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v4.app.FragmentTransaction
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.transition.Fade
import android.util.LongSparseArray
import android.widget.TextView
import com.squareup.sqlbrite2.BriteContentResolver
import com.squareup.sqlbrite2.SqlBrite
import com.udeshcoffee.android.service.MusicService
import com.udeshcoffee.android.ui.BasePresenter
import com.udeshcoffee.android.ui.main.MainActivity
import com.udeshcoffee.android.utils.ServiceConnectionUtil
import com.udeshcoffee.android.utils.ServiceConnectionUtil.ServiceConnectionToken
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers


/**
 * Created by Udathari on 8/22/2017.
 */

//  Extension to add fragments to activities
fun AppCompatActivity.replaceFragmentToActivity(fragment: Fragment, frameId: Int, tag: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        fragment.enterTransition = Fade()
        fragment.exitTransition = Fade()
    }
    val transaction = supportFragmentManager.beginTransaction()
    transaction.replace(frameId, fragment, tag)
    transaction.commit()
}

//  Extension to add fragments to activities
fun AppCompatActivity.replaceFragmentToActivity(fragment: Fragment, frameId: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        fragment.enterTransition = Fade()
        fragment.exitTransition = Fade()
    }
    val transaction = supportFragmentManager.beginTransaction()
    transaction.replace(frameId, fragment)
    transaction.commit()
}

//  Extension to bind activities to the service
fun AppCompatActivity.bindToService(callback: ServiceConnection?): ServiceConnectionToken? =
        ServiceConnectionUtil.bind(this, callback)

//  Extension to unbind activities from the service
fun AppCompatActivity.unbindFromService(token: ServiceConnectionToken) {
    ServiceConnectionUtil.unbind(token)
}

//  Extension to get service from presenters
fun AppCompatActivity.getService(): MusicService? = ServiceConnectionUtil.binder?.service?.get()

fun FragmentActivity.openDrawer() {
    (this as MainActivity).openDrawerLayout()
}

/* Presenter */

//  Extension to get service from presenters
fun <T> BasePresenter<T>.getService(): MusicService? = ServiceConnectionUtil.binder?.service?.get()

// Add a fragment
fun Fragment.replaceFragment(container: Int, fragment: Fragment) {
    val transaction = this.activity!!.supportFragmentManager.beginTransaction()
    transaction.replace(container, fragment)
    transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
    transaction.commit()
}

// Remove Fragment
fun Fragment.removeFragment(fragment: Fragment) {
    val transaction = this.activity!!.supportFragmentManager.beginTransaction()
    transaction.remove(fragment)
    transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
    transaction.commit()
}

fun <T> LongSparseArray<T>.toList(): ArrayList<T> {
    val list: ArrayList<T> = ArrayList()
    var count = 0
    while (count < this.size()) {
        list.add(this.valueAt(count))
        count += 1
    }
    return list
}

// Utils
fun Int.getColorWithAlpha(alpha: Int): Int =
        Color.argb(alpha, Color.red(this), Color.green(this), Color.blue(this))

fun Int.getColorWithAlpha(factor: Float): Int =
        Color.argb(Math.round(Color.alpha(this) * factor), Color.red(this), Color.green(this), Color.blue(this))


/* SQL Brite Utils */

// Get a wrapped content resolver
fun ContentResolver.toBrite(): BriteContentResolver {
    val sqlBrite = SqlBrite.Builder().build()
    return sqlBrite.wrapContentProvider(this, Schedulers.io())
}

/* String */

fun String.toLastFMArtistQuery(): String {
    return this.toLowerCase()
            .split("ft\\.".toRegex())[0]
            .split("feat\\.".toRegex())[0]
            .split("\\[".toRegex())[0]
            .split(",".toRegex())[0]
            .trim()
}

/* TextView */
fun TextView.setRoundColor(color: Int) {
    val bgShape = this.background as GradientDrawable
    bgShape.setColor(ContextCompat.getColor(context, color))
}

fun TextView.setTextWithMilliSecondsToTimer(milliseconds: Long) {
    Observable.just(milliseconds)
            .map {
                var finalTimerString = ""
                val secondsString: String
                var minuteString: String

                // Convert total duration into time
                val hours = (milliseconds / (1000 * 60 * 60)).toInt()
                val minutes = (milliseconds % (1000 * 60 * 60)).toInt() / (1000 * 60)
                val seconds = (milliseconds % (1000 * 60 * 60) % (1000 * 60) / 1000).toInt()

                minuteString = "" + minutes

                // Add hours if there
                if (hours > 0) {
                    finalTimerString = hours.toString() + ":"
                    if (minutes < 10) {
                        minuteString = "0" + minutes
                    }
                }

                // Prepending 0 to seconds if it is one digit
                secondsString = if (seconds < 10) {
                    "0" + seconds
                } else {
                    "" + seconds
                }

                finalTimerString = finalTimerString + minuteString + ":" + secondsString

                // return timer string
                return@map finalTimerString
            }
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .take(1)
            .subscribe { this.text = it }
}