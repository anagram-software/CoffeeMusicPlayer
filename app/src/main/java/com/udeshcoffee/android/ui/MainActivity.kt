package com.udeshcoffee.android.ui

import com.udeshcoffee.android.playback.PlaybackEvents
import com.udeshcoffee.android.extensions.getPlayer
import com.udeshcoffee.android.extensions.padForSystemBars
import android.app.Activity.RESULT_OK
import android.app.SearchManager
import android.provider.MediaStore
import android.content.*
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.transition.Fade
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import com.udeshcoffee.android.utils.ProcessLifecycleScope
import androidx.core.content.edit
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.*
import androidx.preference.PreferenceManager
import com.google.android.material.navigation.NavigationView
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import com.udeshcoffee.android.R
import com.udeshcoffee.android.extensions.*
import com.udeshcoffee.android.playback.PlayerConnection
import com.udeshcoffee.android.ui.miniplayer.MiniPlayerFragment
import com.udeshcoffee.android.ui.player.player.PlayerFragment
import com.udeshcoffee.android.ui.player.queue.QueueFragment
import com.udeshcoffee.android.data.MediaRepository
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.ui.common.dialogs.DeleteSongDialog
import com.udeshcoffee.android.utils.MediaAccess
import com.udeshcoffee.android.utils.Permissions
import com.udeshcoffee.android.utils.PreferenceUtil
import com.udeshcoffee.android.views.FadableLayout
import org.koin.android.ext.android.inject

/**
 * Created by Udathari on 8/25/2017.
 */

class MainActivity : AppCompatActivity(), NavController.OnDestinationChangedListener, NavigationView.OnNavigationItemSelectedListener {

    private lateinit var sharedPreferences: SharedPreferences
    private val playerConnection: PlayerConnection by inject()
    private var isPlayerConnected = false
    private var connectionJob: Job? = null

    private val miniPlayerFragment: MiniPlayerFragment by inject()
    private val playerFragment: PlayerFragment by inject()
    private val queueFragment: QueueFragment by inject()
    private val mediaRepository: MediaRepository by inject()

    private lateinit var controlBack: FadableLayout
    private lateinit var slidingPanel: SlidingUpPanelLayout
    private lateinit var drawer: NavigationView
    private lateinit var drawerLayout: DrawerLayout

    private var loadListOnConnection = false
    private var pendingUri: Uri? = null
    private var pendingSearch: String? = null

    private lateinit var navController: NavController


    // State
    private var controlBackSongId: Long = -1
    private var isQueueVisible = false

    private var eventsJob: Job? = null

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (Permissions.hasReadAudio(this)) {
            onAudioPermissionGranted()
        } else {
            Toast.makeText(this, R.string.permission_audio_required, Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private val deleteLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == RESULT_OK)
            Toast.makeText(this, "Song deleted", Toast.LENGTH_SHORT).show()
    }

    /** Deletes songs from the device, asking for confirmation in a way that fits the API level. */
    fun deleteSongs(songs: List<Song>) {
        if (songs.isEmpty()) return
        val consent = MediaAccess.deleteRequest(contentResolver, songs.map { MediaAccess.songUri(it.id) })
        if (consent != null)
            deleteLauncher.launch(IntentSenderRequest.Builder(consent).build())
        else
            DeleteSongDialog.create(songs.first()).show(supportFragmentManager, "DeleteSongDialog")
    }

    /** Intercepts back only while the queue, the expanded player or the drawer is showing. */
    private val backCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            when {
                isQueueVisible -> showPlayer()
                slidingPanel.panelState == SlidingUpPanelLayout.PanelState.EXPANDED -> closeNowPlay()
                drawerLayout.isDrawerOpen(drawer) -> closeDrawerLayout()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<View>(R.id.main_content).padForSystemBars()

        volumeControlStream = AudioManager.STREAM_MUSIC

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        navController = findNavController(R.id.nav_host_fragment)

        drawer = findViewById(R.id.navigation)
        drawer.setNavigationItemSelectedListener(this)
        drawerLayout = findViewById(R.id.drawer_layout)
        drawerLayout.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerOpened(drawerView: View) = updateBackCallback()
            override fun onDrawerClosed(drawerView: View) = updateBackCallback()
        })
        onBackPressedDispatcher.addCallback(this, backCallback)

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
                updateBackCallback()
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

        navController.addOnDestinationChangedListener(this)

        if (Permissions.hasReadAudio(this))
            onAudioPermissionGranted()
        else
            permissionLauncher.launch(Permissions.startupPermissions())
    }

    override fun onResume() {
        super.onResume()

        eventsJob = lifecycleScope.launch {
            playbackEvents()
                    .filter { it == PlaybackEvents.METADATA_CHANGED || it == PlaybackEvents.SERVICE_CONNECTED }
                    .collect { setControlBack() }
        }

        setControlBack()
    }

    override fun onPause() {
        super.onPause()
        eventsJob?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        navController.removeOnDestinationChangedListener(this)
        connectionJob?.cancel()
        if (isPlayerConnected) playerConnection.disconnect()
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
                    if (item.itemId == R.id.libraryFragment) {
                        navController.popBackStack(R.id.libraryFragment, false)
                    } else {
                        navController.popBackStack(R.id.libraryFragment, false)
                        navController.navigate(item.itemId)
                    }
                }
            }
        }
        return true
    }


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
        this.intent = Intent()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_search -> {
                navController.navigate(R.id.searchFragment)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Handles launch intents on the main thread (MediaController must be used there). If the
     * player isn't connected yet, the action is remembered and run in [onPlayerConnected].
     */
    private fun handleIntent(intent: Intent) {
        val player = getPlayer()
        when (intent.action) {
            Intent.ACTION_VIEW -> {
                if (player == null) pendingUri = intent.data
                else intent.data?.let { player.playUri(it) }
            }
            MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH -> {
                val query = intent.getStringExtra(SearchManager.QUERY) ?: ""
                if (player == null) pendingSearch = query else player.playFromSearch(query)
            }
            Intent.ACTION_MAIN -> {
                if (player == null) loadListOnConnection = true else player.restoreQueueIfEmpty()
            }
        }
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
        val service = getPlayer()
        service?.let {it ->
            it.currentSong()?.let {
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
        updateBackCallback()
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
        updateBackCallback()
        ViewCompat.requestApplyInsets(findViewById(R.id.player_container))
    }

    private fun updateBackCallback() {
        backCallback.isEnabled = isQueueVisible ||
                slidingPanel.panelState == SlidingUpPanelLayout.PanelState.EXPANDED ||
                drawerLayout.isDrawerOpen(drawer)
    }

    private fun onAudioPermissionGranted() {
        connectPlayer()
        importMediaStorePlaylistsOnce()
    }

    /** Playlists moved from MediaStore to the app database in DB version 7; copy existing ones once. */
    private fun importMediaStorePlaylistsOnce() {
        if (sharedPreferences.getBoolean(PreferenceUtil.PLAYLISTS_IMPORTED, false)) return
        // Application-wide scope: the import should finish even if this activity goes away.
        ProcessLifecycleScope.launch {
            try {
                val count = mediaRepository.importMediaStorePlaylists()
                sharedPreferences.edit { putBoolean(PreferenceUtil.PLAYLISTS_IMPORTED, true) }
                if (count > 0)
                    Toast.makeText(applicationContext, resources.getQuantityString(R.plurals.playlists_imported, count, count), Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Log.w(TAG, "Playlist import failed", e)
            }
        }
    }


    private fun connectPlayer() {
        if (isPlayerConnected) return
        connectionJob = lifecycleScope.launch {
            playbackEvents().filter { it == PlaybackEvents.SERVICE_CONNECTED }.collect { onPlayerConnected() }
        }
        playerConnection.connect()
        isPlayerConnected = true
    }

    private fun onPlayerConnected() {
        pendingSearch?.let {
            getPlayer()?.playFromSearch(it)
            pendingSearch = null
        }
        pendingUri?.let {
            getPlayer()?.playUri(it)
            pendingUri = null
        }
        if (loadListOnConnection) {
            getPlayer()?.restoreQueueIfEmpty()
            loadListOnConnection = false
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}