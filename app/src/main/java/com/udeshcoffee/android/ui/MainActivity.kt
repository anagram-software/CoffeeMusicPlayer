package com.udeshcoffee.android.ui

import android.Manifest
import android.content.*
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.transition.Fade
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.findNavController
import androidx.preference.PreferenceManager
import com.cantrowitz.rxbroadcast.RxBroadcast
import com.google.android.material.navigation.NavigationView
import com.greysonparrelli.permiso.Permiso
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import com.udeshcoffee.android.R
import com.udeshcoffee.android.extensions.*
import com.udeshcoffee.android.service.MusicService
import com.udeshcoffee.android.ui.miniplayer.MiniPlayerFragment
import com.udeshcoffee.android.ui.player.player.PlayerFragment
import com.udeshcoffee.android.ui.player.queue.QueueFragment
import com.udeshcoffee.android.utils.ServiceConnectionUtil
import com.udeshcoffee.android.views.FadableLayout
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import org.koin.android.ext.android.inject
import java.util.concurrent.TimeUnit

/**
 * Created by Udathari on 8/25/2017.
 */

class MainActivity : AppCompatActivity(), ServiceConnection, NavController.OnDestinationChangedListener, NavigationView.OnNavigationItemSelectedListener {

    private lateinit var sharedPreferences: SharedPreferences
    private var serviceToken: ServiceConnectionUtil.ServiceConnectionToken? = null

    private val miniPlayerFragment: MiniPlayerFragment by inject()
    private val playerFragment: PlayerFragment by inject()
    private val queueFragment: QueueFragment by inject()

    private lateinit var controlBack: FadableLayout
    private lateinit var slidingPanel: SlidingUpPanelLayout
    private lateinit var drawer: NavigationView
    private lateinit var drawerLayout: DrawerLayout

    private var loadListOnConnection = false
    private var pendingUri: Uri? = null

    private lateinit var navController: NavController


    // State
    private var controlBackSongId: Long = -1
    private var isQueueVisible = false

    private lateinit var broadcastDisposable: Disposable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        volumeControlStream = AudioManager.STREAM_MUSIC

        Permiso.getInstance().setActivity(this)
        Permiso.getInstance().requestPermissions(object : Permiso.IOnPermissionResult {
            override fun onPermissionResult(resultSet: Permiso.ResultSet) {
                if (resultSet.areAllPermissionsGranted()) {
                    bindToService()
                } else {
                    Toast.makeText(this@MainActivity, "Permission check failed", Toast.LENGTH_LONG).show()
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

        navController = findNavController(R.id.nav_host_fragment)

        drawer = findViewById(R.id.navigation)
        drawer.setNavigationItemSelectedListener(this)
        drawerLayout = findViewById(R.id.drawer_layout)

        slidingPanel = findViewById(R.id.slidingpanel)
        slidingPanel.panelState = SlidingUpPanelLayout.PanelState.HIDDEN
        slidingPanel.addPanelSlideListener(object : SlidingUpPanelLayout.PanelSlideListener {
            override fun onPanelSlide(panel: View?, slideOffset: Float) {
                if (slideOffset <= 0.2f) {
                    miniPlayerFragment.setAlpha(1f)
                } else if (slideOffset > 0.2f && slideOffset <= 0.6f) {
                    miniPlayerFragment.setVisibility(View.VISIBLE)
                    miniPlayerFragment.setAlpha(1 - (slideOffset - 0.2f) / 0.4f)
                } else if (slideOffset > 0.6f) {
                    miniPlayerFragment.setVisibility(View.INVISIBLE)
                }
            }

            override fun onPanelStateChanged(panel: View?, previousState: SlidingUpPanelLayout.PanelState?, newState: SlidingUpPanelLayout.PanelState?) {
                if (newState == SlidingUpPanelLayout.PanelState.COLLAPSED) {
                    miniPlayerFragment.setVisibility(View.VISIBLE)
                    miniPlayerFragment.setAlpha(1f)
                }
            }

        })

        // Control Back
        controlBack = findViewById(R.id.controls_back)

        // Mini Player Fragment
        replaceFragmentToActivity(miniPlayerFragment, R.id.mini_player_container)

        replaceFragmentToActivity(playerFragment, R.id.player_container)
        ViewCompat.requestApplyInsets(findViewById(R.id.player_container))

        //        try {
//            //current version
//            val packageInfo = packageManager.getPackageInfo(packageName, 0)
//            val versionCode = packageInfo.versionCode
//
//            //version where changelog has been viewed
//            val lastViewWhatsNew = sharedPreferences.getInt(PreferenceUtil.LAST_VIEWED_WHATS_NEW, 0)
//            if (lastViewWhatsNew == 0) {
//                sharedPreferences.edit { putInt(PreferenceUtil.LAST_VIEWED_WHATS_NEW, versionCode) }
//            } else if (lastViewWhatsNew < versionCode) {
//                sharedPreferences.edit { putInt(PreferenceUtil.LAST_VIEWED_WHATS_NEW, versionCode) }
//                WhatsNewDialog.create().show(supportFragmentManager, "WhatsNewDialog")
//            }
//        } catch (e: PackageManager.NameNotFoundException) {
//            Log.w(TAG,"Unable to get version code.")
//        }

        intent?.let {
            handleIntent(it)
            this.intent = Intent()
        }
    }

    override fun onResume() {
        super.onResume()
        Permiso.getInstance().setActivity(this)

        val filter = IntentFilter()
        filter.addAction(MusicService.InternalIntents.METADATA_CHANGED)
        filter.addAction(MusicService.InternalIntents.SERVICE_CONNECTED)

        broadcastDisposable = RxBroadcast.fromLocalBroadcast(this, filter)
                .subscribe {
                    when (it.action) {
                        MusicService.InternalIntents.METADATA_CHANGED -> {
                            setControlBack()
                        }
                        MusicService.InternalIntents.SERVICE_CONNECTED -> {
                            setControlBack()
                        }
                    }
                }
        setControlBack()
    }

    override fun onPause() {
        super.onPause()
        if (!broadcastDisposable.isDisposed)
            broadcastDisposable.dispose()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceToken?.let { unbindFromService(it) }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Permiso.getInstance().onRequestPermissionResult(requestCode, permissions, grantResults)
    }

    override fun onDestinationChanged(controller: NavController, destination: NavDestination, arguments: Bundle?) {
        Log.d(TAG, "${drawer.checkedItem?.itemId} ${destination.id}")
        if (drawer.checkedItem?.itemId != destination.id)
            drawer.setCheckedItem(destination.id)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.navigationNowPlay -> {
                closeDrawerLayout()
                openNowPlay()
            }
            else -> {
                closeDrawerLayout()
                if (navController.currentDestination != null && navController.currentDestination!!.id != item.itemId) {
                    navController.navigate(item.itemId)
                }
            }
        }
        return true
    }


    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let {
            handleIntent(it)
            this.intent = Intent()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_search -> {
                navController.navigate(R.id.searchFragment)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun handleIntent(intent: Intent) {
        Single.fromCallable {
            when (intent.action) {
                Intent.ACTION_VIEW -> {
                    val service = getService()
                    if (service == null)
                        pendingUri = intent.data
                    else
                        intent.data?.let { service.initFile(it) }
                }
                Intent.ACTION_MAIN -> {
                    val service = getService()
                    if (service == null)
                        loadListOnConnection = true
                    else
                        service.loadLastListAndInit()
                }
                else -> null
            }
        }
                .delaySubscription(350, TimeUnit.MILLISECONDS)
                .subscribe()
    }

    fun openDrawerLayout() {
        drawerLayout.openDrawer(GravityCompat.START)
    }

    private fun closeDrawerLayout() {
        drawerLayout.closeDrawer(GravityCompat.START)
    }

    fun openNowPlay() {
        if (slidingPanel.panelState == SlidingUpPanelLayout.PanelState.COLLAPSED)
            slidingPanel.panelState = SlidingUpPanelLayout.PanelState.EXPANDED
    }

    fun closeNowPlay() {
        slidingPanel.panelState = SlidingUpPanelLayout.PanelState.COLLAPSED
    }

    fun hideNowPlay() {
        slidingPanel.panelState = SlidingUpPanelLayout.PanelState.HIDDEN
    }

    fun initNowPlay() {
        if (slidingPanel.panelState == SlidingUpPanelLayout.PanelState.HIDDEN)
            closeNowPlay()
    }

    fun setSlidingView(view: View) {
        slidingPanel.setScrollableView(view)
    }

    private fun setControlBack() {
        val service = getService()
        service?.let {service ->
            service.currentSong()?.let {
                if (controlBackSongId != it.id) {
                    it.loadSongColor(this, controlBack)
                    controlBackSongId = it.id
                }
            }
        }
    }

    fun showQueue() {
        isQueueVisible = true
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.player_container, queueFragment)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            queueFragment.enterTransition = Fade()
            playerFragment.exitTransition = Fade()
        }
        transaction.commit()
        slidingPanel.isTouchEnabled = false
        ViewCompat.requestApplyInsets(findViewById(R.id.player_container))
    }

    fun showPlayer() {
        isQueueVisible = false
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.player_container, playerFragment)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            playerFragment.enterTransition = Fade()
            queueFragment.exitTransition = Fade()
        }
        transaction.commit()
        slidingPanel.isTouchEnabled = true
        ViewCompat.requestApplyInsets(findViewById(R.id.player_container))
    }

    override fun onBackPressed() {
        Log.d(TAG, "onBackPressedSuper")
        when {
            isQueueVisible -> showPlayer()
            slidingPanel.panelState == SlidingUpPanelLayout.PanelState.EXPANDED -> closeNowPlay()
            drawerLayout.isDrawerOpen(drawer) -> closeDrawerLayout()
            else -> {
                super.onBackPressed()
            }
        }
    }


    fun bindToService() {
        serviceToken = bindToService(this)
        serviceToken?.let { Log.d(TAG, "serviceToken") }
    }

    override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
        Log.d(TAG, "connected")
        LocalBroadcastManager.getInstance(this).sendBroadcast(Intent().setAction(MusicService.InternalIntents.SERVICE_CONNECTED))
        pendingUri?.let {
            getService()?.initFile(it)
            pendingUri = null
        }
        if (loadListOnConnection) {
            getService()?.loadLastListAndInit()
            loadListOnConnection = false
        }
    }

    override fun onServiceDisconnected(p0: ComponentName?) {}

    companion object {
        private const val TAG = "MainActivity"
    }
}