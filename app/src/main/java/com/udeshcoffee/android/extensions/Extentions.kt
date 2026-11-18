package com.udeshcoffee.android.extensions

import androidx.lifecycle.AndroidViewModel
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import android.transition.Fade
import android.util.LongSparseArray
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.udeshcoffee.android.ui.BasePresenter
import com.udeshcoffee.android.ui.MainActivity
import com.udeshcoffee.android.playback.PlayerConnection
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharedFlow
import org.koin.core.context.GlobalContext


/**
 * Created by Udathari on 8/22/2017.
 */

//  Extension to add fragments to activities
fun AppCompatActivity.replaceFragmentToActivity(fragment: androidx.fragment.app.Fragment, frameId: Int, tag: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        fragment.enterTransition = Fade()
        fragment.exitTransition = Fade()
    }
    val transaction = supportFragmentManager.beginTransaction()
    transaction.replace(frameId, fragment, tag)
    transaction.commit()
}

//  Extension to add fragments to activities
fun AppCompatActivity.replaceFragmentToActivity(fragment: androidx.fragment.app.Fragment, frameId: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        fragment.enterTransition = Fade()
        fragment.exitTransition = Fade()
    }
    val transaction = supportFragmentManager.beginTransaction()
    transaction.replace(frameId, fragment)
    transaction.commit()
}

fun FragmentActivity.openDrawer() {
    (this as MainActivity).openDrawerLayout()
}

/** The playback connection while it is connected to [com.udeshcoffee.android.playback.PlaybackService], else null. */
fun connectedPlayer(): PlayerConnection? = GlobalContext.get().get<PlayerConnection>().takeIf { it.isConnected }

/** Playback change events ([com.udeshcoffee.android.playback.PlaybackEvents] names), delivered on the main thread. */
fun playbackEvents(): SharedFlow<String> = GlobalContext.get().get<PlayerConnection>().events

fun AppCompatActivity.getPlayer(): PlayerConnection? = connectedPlayer()

fun AndroidViewModel.getPlayer(): PlayerConnection? = connectedPlayer()

fun <T> BasePresenter<T>.getPlayer(): PlayerConnection? = connectedPlayer()

// Add a fragment
fun Fragment.replaceFragment(container: Int, fragment: androidx.fragment.app.Fragment) {
    val transaction = this.activity!!.supportFragmentManager.beginTransaction()
    transaction.replace(container, fragment)
    transaction.setTransition(androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_FADE)
    transaction.commit()
}

// Remove Fragment
fun Fragment.removeFragment(fragment: androidx.fragment.app.Fragment) {
    val transaction = this.activity!!.supportFragmentManager.beginTransaction()
    transaction.remove(fragment)
    transaction.setTransition(androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_FADE)
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

/* String */

fun String.toLastFMArtistQuery(): String {
    return this.lowercase()
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
    val totalSeconds = milliseconds / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    text = if (hours > 0) String.format(java.util.Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    else String.format(java.util.Locale.ROOT, "%d:%02d", minutes, seconds)
}
/** The SearchView's query text, starting with the current value. */
fun androidx.appcompat.widget.SearchView.queryTextChanges(): kotlinx.coroutines.flow.Flow<String> = kotlinx.coroutines.flow.callbackFlow {
    setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
        override fun onQueryTextSubmit(query: String?): Boolean = false
        override fun onQueryTextChange(newText: String?): Boolean {
            trySend(newText ?: "")
            return true
        }
    })
    trySend(query?.toString() ?: "")
    awaitClose { setOnQueryTextListener(null) }
}
