package com.udeshcoffee.android.ui.player.lyrics

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.udeshcoffee.android.R
import com.udeshcoffee.android.api.genius.Result
import com.udeshcoffee.android.interfaces.OnItemClickListener
import com.udeshcoffee.android.ui.adapters.LyricAdapter
import com.udeshcoffee.android.ui.dialogs.CustomLyricDialog
import com.udeshcoffee.android.ui.dialogs.SearchSongDialog
import com.udeshcoffee.android.utils.isNetworkAvailable
import org.koin.android.ext.android.inject

/**
* Created by Udathari on 8/25/2017.
*/
class LyricsFragment : Fragment(), LyricsContract.View {

    val TAG = "LyricsFragment"

    override val presenter: LyricsContract.Presenter by inject()

    private var isRetryShowing = false

    // Layouts
    private lateinit var errorLayout: LinearLayout
    private lateinit var progressLayout: LinearLayout
    private lateinit var lyricLayout: ScrollView
    private lateinit var multipleLayout: LinearLayout

    // Widgets
    private lateinit var toolbar: Toolbar
    private lateinit var errorText: TextView
    private lateinit var errorAction: TextView
    private lateinit var progressText: TextView
    private lateinit var lyricText: TextView
    private lateinit var multipleRV: RecyclerView

    // Multiple
    private lateinit var adapter: LyricAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.frag_lyrics, container, false)

        // Set up player view
        with(root) {
            errorLayout = findViewById(R.id.lyric_error)
            progressLayout = findViewById(R.id.lyric_progress)
            lyricLayout = findViewById(R.id.lyric)
            multipleLayout = findViewById(R.id.lyric_multiple)

            toolbar = findViewById(R.id.lyric_toolbar)
            toolbar.inflateMenu(R.menu.lyric_menu)
            toolbar.setOnMenuItemClickListener {
                when(it.itemId) {
                    R.id.action_lyrics_size -> {
                        presenter.changeLyricsSize()
                    }
                    R.id.action_add_lyrics -> {
                        presenter.openAddLyrics()
                    }
                    R.id.action_search -> {
                        presenter.openSearch()
                    }
                }
                return@setOnMenuItemClickListener false
            }

            errorText = findViewById(R.id.lyric_error_text)
            progressText = findViewById(R.id.lyric_progress_text)
            lyricText = findViewById(R.id.lyric_text)

            errorAction = findViewById(R.id.lyric_error_action)
            errorAction.setOnClickListener {
                if (isRetryShowing)
                    presenter.fetchLyrics()
                else
                    presenter.openSearch()
            }

            // Multiple
            multipleRV = findViewById(R.id.lyric_multiple_recyclerview)
            multipleRV.layoutManager = LinearLayoutManager(context)
            multipleRV.hasFixedSize()
            adapter = LyricAdapter()
            adapter.listener = object : OnItemClickListener{
                override fun onItemClick(position: Int) {
                    presenter.selectFromMultiple(adapter.getItem(position))
                }

                override fun onItemLongClick(position: Int) {}
            }
            multipleRV.adapter = adapter
        }

        return root
    }

    override fun onResume() {
        super.onResume()
        presenter.view = this
        presenter.start()
    }

    override fun onPause() {
        super.onPause()
        presenter.stop()
    }

    override fun showLoading() {
        if (isAdded) {
            context?.let {
                if (isNetworkAvailable(it, false)) {
                    hideLyricLayout()
                    hideErrorLayout()
                    hideMultipleLayout()
                    lyricLayout.visibility = View.INVISIBLE
                    progressLayout.visibility = View.VISIBLE
                    progressText.text = getString(R.string.msg_loading)
                } else {
                    setFailed()
                }
            }
        }
    }

    override fun showSearching() {
        if (isAdded) {
            context?.let {
                if (isNetworkAvailable(it, false)) {
                    hideLyricLayout()
                    hideErrorLayout()
                    hideMultipleLayout()
                    lyricLayout.visibility = View.INVISIBLE
                    progressLayout.visibility = View.VISIBLE
                    progressText.text = getString(R.string.msg_searching)
                } else {
                    setFailed()
                }
            }
        }
    }

    override fun setLyrics(lyrics: String) {
        if (isAdded) {
            hideErrorLayout()
            hideProgressLayout()
            hideMultipleLayout()
            lyricLayout.visibility = View.VISIBLE
            val result = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                Html.fromHtml(lyrics, Html.FROM_HTML_MODE_LEGACY)
            } else {
                @Suppress("DEPRECATION")
                Html.fromHtml(lyrics)
            }
            lyricText.text = result
        }
    }

    override fun setLyricsSize(size: Int) {
        when(size) {
            LyricsPresenter.LyricsSize.SMALL -> {
                lyricText.textSize = resources.getDimension(R.dimen.lyrics_text_small)
            }
            LyricsPresenter.LyricsSize.MEDIUM -> {
                lyricText.textSize = resources.getDimension(R.dimen.lyrics_text_medium)
            }
            LyricsPresenter.LyricsSize.LARGE -> {
                lyricText.textSize = resources.getDimension(R.dimen.lyrics_text_large)
            }
        }
    }

    override fun setNotFound() {
        if (isAdded) {
            hideLyricLayout()
            hideProgressLayout()
            hideMultipleLayout()
            isRetryShowing = false
            errorLayout.visibility = View.VISIBLE
            errorText.text = getString(R.string.msg_not_found)
            errorAction.text = getString(R.string.action_search)
        }
    }

    override fun setMultipleFound(items: List<Result>) {
        if (isAdded) {
            hideLyricLayout()
            hideProgressLayout()
            hideErrorLayout()
            multipleLayout.visibility = View.VISIBLE
            adapter.accept(items)
        }
    }

    override fun onSearchRequest(id:Long ,title: String, artist: String) {
        Log.d(TAG, "onSearchRequest id:$id, title:$title, artist:$artist")
        presenter.search(id, title, artist)
    }

    override fun onAddLyrics(id:Long , lyrics: String) {
        presenter.addCustomLyrics(id, lyrics)
    }

    override fun setFailed() {
        if (isAdded) {
            hideLyricLayout()
            hideProgressLayout()
            hideMultipleLayout()
            isRetryShowing = true
            errorLayout.visibility = View.VISIBLE
            errorText.text = getString(R.string.msg_connection_failed)
            errorAction.text = getString(R.string.action_retry)
        }
    }

    override fun hideErrorLayout() {
        errorLayout.visibility = View.INVISIBLE
    }

    override fun hideProgressLayout() {
        progressLayout.visibility = View.INVISIBLE
    }

    override fun hideLyricLayout() {
        lyricText.text = ""
        lyricLayout.visibility = View.INVISIBLE
    }

    override fun hideMultipleLayout() {
        adapter.accept(ArrayList())
        multipleLayout.visibility = View.INVISIBLE
    }

    override fun showSearchDialog(id:Long ,title: String, artist: String) {
        SearchSongDialog.create(id, title, artist).also {
            it.setTargetFragment(this, SEARCH_LYRICS)
            it.show(fragmentManager, "SearchLyricDialog")
        }
    }

    override fun showCustomLyricDialog(id: Long) {
        CustomLyricDialog.create(id).also {
            it.setTargetFragment(this, ADD_LYRICS)
            it.show(fragmentManager, "CustomLyricDialog")
        }
    }

    companion object {
        val SEARCH_LYRICS = 0
        val ADD_LYRICS = 1
    }
}