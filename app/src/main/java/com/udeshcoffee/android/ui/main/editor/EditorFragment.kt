package com.udeshcoffee.android.ui.main.editor

import android.app.Activity.RESULT_OK
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import androidx.transition.Fade
import com.udeshcoffee.android.R
import com.udeshcoffee.android.extensions.loadArtwork
import com.udeshcoffee.android.extensions.setRoundColor
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.ui.common.dialogs.PermissionToSdCardDialog
import com.udeshcoffee.android.ui.common.dialogs.SearchSongDialog
import com.udeshcoffee.android.ui.player.lyrics.LyricsFragment
import com.udeshcoffee.android.utils.isNetworkAvailable
import com.udeshcoffee.android.utils.loadAlbumArtworkFromUri
import org.koin.android.ext.android.inject


/**
 * Created by Udathari on 9/28/2017.
 */
class EditorFragment : androidx.fragment.app.Fragment() {

    private val viewModel: EditorViewModel by inject()
    @Suppress("DEPRECATION")
    lateinit var dialog: ProgressDialog

    var actionBar: ActionBar? = null

    // Fields
    private lateinit var albumArt: ImageView
    private lateinit var selectFromGallery: Button
    private lateinit var reset: Button
    private lateinit var title: EditText
    private lateinit var album: EditText
    private lateinit var artist: EditText
    private lateinit var genre: EditText
    private lateinit var year: EditText
    private lateinit var trackno: EditText
    private lateinit var discno: EditText
    private lateinit var path: EditText

    init {
        enterTransition = Fade()
        exitTransition = Fade()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.frag_edit_song, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d(TAG, "Editor Fragment Creating")

        setHasOptionsMenu(true)

        val song = arguments!!.getParcelable<Song>(ARGUMENT_SONG)
        if (song == null) {
            Log.d(TAG, "No song")
            activity?.onBackPressed()
        }

        @Suppress("DEPRECATION")
        dialog = ProgressDialog(context)
        dialog.setOnDismissListener{
            viewModel.disposeCollectionDisposable()
        }
        view.apply {
            val toolbar = findViewById<Toolbar>(R.id.toolbar)
            (activity as AppCompatActivity).apply {
                setSupportActionBar(toolbar)
                this@EditorFragment.actionBar = supportActionBar
            }

            actionBar?.apply {
                setDisplayHomeAsUpEnabled(true)
                title = ""
            }

            val titleView = findViewById<TextView>(R.id.title_view)
            titleView.setRoundColor(R.color.editAccent)

            albumArt = findViewById(R.id.album_art)

            selectFromGallery = findViewById(R.id.action_select_image)
            selectFromGallery.setOnClickListener{
                showSelectImageUI()
            }

            reset = findViewById(R.id.action_reset)
            reset.setOnClickListener{
                viewModel.actionReset()
            }

            title = findViewById(R.id.editsongtitle)
            album = findViewById(R.id.editsongalbum)
            artist = findViewById(R.id.editsongartist)
            genre = findViewById(R.id.editsonggenre)
            year = findViewById(R.id.editsongyear)
            trackno = findViewById(R.id.editsongalbumno)
            discno = findViewById(R.id.editsongdiscno)
            path = findViewById(R.id.editsongpath)
        }

        viewModel.start(song!!)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.edit_song_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            android.R.id.home -> {
                activity?.onBackPressed()
                hideKeyboard()
            }
            R.id.action_collect_metadata -> {
                showSearchDialog()
            }
            R.id.action_save -> {
                viewModel.save(
                        title.text.toString(),
                        album.text.toString(),
                        artist.text.toString(),
                        genre.text.toString(),
                        year.text.toString(),
                        trackno.text.toString(),
                        discno.text.toString()
                )
                hideKeyboard()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.stop()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            PermissionToSdCardDialog.REQUEST_CODE -> {
                if (resultCode == RESULT_OK) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        data?.data?.let {
                            context!!.contentResolver
                                    .takePersistableUriPermission(it,
                                            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                        }
                    }
                } else {
                    activity?.onBackPressed()
                }
            }
            SELECT_IMAGE -> {
                Log.d(TAG, "onResult")
                if (resultCode == RESULT_OK) {
                    Log.d(TAG, "onResult Ok")
                    data?.data?.let { viewModel.imageSelected(it) }
                }
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel.apply {
            reloadImage.observe(this@EditorFragment, Observer { it ->
                when(it) {
                    EditorViewModel.ImageType.SONG -> {
                        context?.let { song.value?.loadArtwork(it, albumArt) }
                    }
                    EditorViewModel.ImageType.URI -> {
                        context?.let { loadAlbumArtworkFromUri(it, albumArtUri, albumArt) }
                    }
                    EditorViewModel.ImageType.URL -> {
                        context?.let { loadAlbumArtworkFromUri(it, albumArtUrl, albumArt) }
                    }
                }
            })
            song.observe(this@EditorFragment, Observer {
                title.setText(it?.title)
                album.setText(it?.albumName)
                artist.setText(it?.artistName)
                year.setText(it?.year.toString())
                trackno.setText(it?.track.toString())
                discno.setText(it?.discNumber.toString())
            })
            genre.observe(this@EditorFragment, Observer {
                this@EditorFragment.genre.setText(it)
            })
            path.observe(this@EditorFragment, Observer {
                this@EditorFragment.path.setText(it)
            })
            resetEnabled.observe(this@EditorFragment, Observer {
                it?.let { reset.isEnabled = it }
            })
            finish.observe(this@EditorFragment, Observer {
                activity?.onBackPressed()
            })
            showToast.observe(this@EditorFragment, Observer {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            })
            progressDialog.observe(this@EditorFragment, Observer {
                if (it == null) {
                    if (dialog.isShowing) {
                        dialog.dismiss()
                    }
                } else {
                    dialog.setMessage(it)
                    dialog.show()
                }
            })
            showPermissionDialog.observe(this@EditorFragment, Observer {
                val takePermissionDialog = PermissionToSdCardDialog()
                takePermissionDialog.setTargetFragment(this@EditorFragment, 0)
                fragmentManager?.let { it1 -> takePermissionDialog.show(it1, "PermissionToSdCardDialog") }
            })
            finish.observe(this@EditorFragment, Observer {
                activity?.onBackPressed()
            })
        }
    }

    fun onSearchRequest(id: Long, title: String, artist: String) {
        Log.d(TAG, "onSearchRequest id:$id, title:$title, artist:$artist")
        if (isNetworkAvailable(context!!, false))
            viewModel.search(title, artist)
        else
            Toast.makeText(context, "No Connection", Toast.LENGTH_SHORT).show()
    }

    private fun showSearchDialog() {
        if (!isNetworkAvailable(context!!, false)) {
            Toast.makeText(context, "No Connection", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.song.value?.let { it ->
            SearchSongDialog.create(it.id, it.title, it.artistName).also {
                it.setTargetFragment(this, LyricsFragment.SEARCH_LYRICS)
                fragmentManager?.let { it1 -> it.show(it1, "SearchLyricDialog") }
            }
        }
    }

    private fun showSelectImageUI() {
        val photoPickerIntent = Intent(Intent.ACTION_PICK)
        photoPickerIntent.type = "image/*"
        startActivityForResult(photoPickerIntent, SELECT_IMAGE)
    }

    private fun hideKeyboard() {
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
    }

    companion object {
        private const val ARGUMENT_SONG = "ARGUMENT_SONG"

        fun createBundle(song: Song): Bundle {
            val bundle = Bundle()
            bundle.putParcelable(ARGUMENT_SONG, song)
            return bundle
        }

        private const val TAG = "EditorFragment"
        private const val SELECT_IMAGE = 17775
    }

}