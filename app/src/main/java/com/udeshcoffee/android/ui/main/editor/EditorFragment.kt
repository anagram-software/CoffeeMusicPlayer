package com.udeshcoffee.android.ui.main.editor

import android.app.Activity.RESULT_OK
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.ActionBar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import com.udeshcoffee.android.R
import com.udeshcoffee.android.loadArtwork
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.setRoundColor
import com.udeshcoffee.android.ui.dialogs.PermissionToSdCardDialog
import com.udeshcoffee.android.ui.dialogs.SearchSongDialog
import com.udeshcoffee.android.ui.player.lyrics.LyricsFragment
import com.udeshcoffee.android.utils.isNetworkAvailable
import com.udeshcoffee.android.utils.loadAlbumArtworkFromUri


/**
 * Created by Udathari on 9/28/2017.
 */
class EditorFragment : Fragment(), EditorContract.View {

    val TAG = "EditorFragment"

    val SELECT_IMAGE = 17775

    override var presenter: EditorContract.Presenter? = null
    lateinit var dialog: ProgressDialog

    var actionBar: ActionBar? = null

    // Fields
    lateinit var albumArt: ImageView
    lateinit var selectFromGallery: Button
    lateinit var reset: Button
    lateinit var title: EditText
    lateinit var album: EditText
    lateinit var artist: EditText
    lateinit var genre: EditText
    lateinit var year: EditText
    lateinit var trackno: EditText
    lateinit var discno: EditText
    lateinit var path: EditText

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.frag_edit_song, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)

        dialog = ProgressDialog(context)
        dialog.setOnDismissListener{
            presenter?.disposeCollectionDisposable()
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
                presenter?.actionSelectImage()
            }

            reset = findViewById(R.id.action_reset)
            reset.setOnClickListener{
                presenter?.actionReset()
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
        presenter?.start()
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.edit_song_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when(item?.itemId) {
            android.R.id.home -> {
                activity?.onBackPressed()
                hideKeyboard()
            }
            R.id.action_collect_metadata -> {
                presenter?.actionSearch()
            }
            R.id.action_save -> {
                presenter?.save(
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
        presenter?.stop()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            PermissionToSdCardDialog.REQUEST_CODE -> {
                if (resultCode == RESULT_OK) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        context!!.contentResolver
                                .takePersistableUriPermission(data?.data,
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    }
                } else {
                    finish()
                }
            }
            SELECT_IMAGE -> {
                Log.d(TAG, "onResult")
                if (resultCode == RESULT_OK) {
                    Log.d(TAG, "onResult Ok")
                    data?.data?.let { presenter?.imageSelected(it) }
                }
            }
        }
    }

    fun onSearchRequest(id: Long, title: String, artist: String) {
        Log.d(TAG, "onSearchRequest id:$id, title:$title, artist:$artist")
        if (isNetworkAvailable(context!!, false))
            presenter?.search(title, artist)
        else
            Toast.makeText(context, "No Connection", Toast.LENGTH_SHORT).show()
    }

    override fun setAlbumArt(song: Song) {
        context?.let { song.loadArtwork(it, albumArt) }
    }

    override fun setAlbumArt(uri: Uri) {
        context?.let { loadAlbumArtworkFromUri(it, uri, albumArt) }
    }

    override fun setAlbumArt(url: String) {
        context?.let { loadAlbumArtworkFromUri(it, url, albumArt) }
    }

    override fun setData(title: String?, album: String?, artist: String?, genre: String?, year: String?, trackNo: Int?, discNo: Int?) {
        title?.let { this.title.setText(it) }
        album?.let {this.album.setText(it)}
        artist?.let {this.artist.setText(it)}
        genre?.let {this.genre.setText(it)}
        year?.let {this.year.setText(it)}
        trackNo?.let {this.trackno.setText(it.toString())}
        discNo?.let {this.discno.setText(it.toString())}
    }

    override fun setPath(path: String) {
        this.path.setText(path)
    }

    override fun enableDisableReset(isEnable: Boolean) {
        reset.isEnabled = isEnable
    }

    override fun finish() {
        activity?.onBackPressed()
    }

    override fun showSearchDialog(id:Long ,title: String, artist: String) {
        if (!isNetworkAvailable(context!!, false)) {
            Toast.makeText(context, "No Connection", Toast.LENGTH_SHORT).show()
            return
        }
        val mDialog = SearchSongDialog()
        mDialog.setTargetFragment(this, LyricsFragment.SEARCH_LYRICS)
        val bundle = Bundle()
        bundle.putLong(SearchSongDialog.ARGUMENT_ID, id)
        bundle.putString(SearchSongDialog.ARGUMENT_TITLE, title)
        bundle.putString(SearchSongDialog.ARGUMENT_ARTIST, artist)
        mDialog.arguments = bundle
        mDialog.show(fragmentManager, "SearchLyricDialog")
    }

    override fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun showHideProgressDialog(isShow: Boolean, message: String?) {
        message?.let {
            dialog.setMessage(it)
        }
        if (isShow) {
            dialog.show()
        } else {
            if (dialog.isShowing) {
                dialog.dismiss()
            }
        }
    }

    override fun showPermissionDialog() {
        val takePermissionDialog = PermissionToSdCardDialog()
        takePermissionDialog.setTargetFragment(this, 0)
        takePermissionDialog.show(fragmentManager, "PermissionToSdCardDialog")
    }

    override fun showSelectImageUI() {
        val photoPickerIntent = Intent(Intent.ACTION_PICK)
        photoPickerIntent.type = "image/*"
        startActivityForResult(photoPickerIntent, SELECT_IMAGE)
    }

    private fun hideKeyboard() {
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
    }


}