package com.udeshcoffee.android.ui.dialogs

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import com.udeshcoffee.android.R
import com.udeshcoffee.android.doSharedTransaction
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.ui.MiniPlayerActivity
import com.udeshcoffee.android.ui.main.detail.artistdetail.ArtistDetailFragment
import com.udeshcoffee.android.ui.detail.albumdetail.ArtistDetailPresenter
import com.udeshcoffee.android.ui.main.MainActivity
import com.udeshcoffee.android.ui.main.detail.albumdetail.AlbumDetailFragment
import com.udeshcoffee.android.ui.main.detail.albumdetail.AlbumDetailPresenter
import com.udeshcoffee.android.ui.main.editor.EditorFragment
import com.udeshcoffee.android.ui.main.editor.EditorPresenter
import com.udeshcoffee.android.utils.Injection

/**
 * Created by Udathari on 9/27/2017.
 */
class PlayerMoreDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val song = this.arguments!!.getParcelable<Song>(ARGUMENT_SONG)

        val items = arrayOf(
                getString(R.string.action_go_to_album),
                getString(R.string.action_go_to_artist),
                getString(R.string.action_add_to_playlist),
                getString(R.string.action_edit),
                getString(R.string.action_yt),
                getString(R.string.action_share))

        val builder = AlertDialog.Builder(context!!)

        builder.setTitle(song.title).setItems(items) { _, which ->
            when (which) {
                0 -> showAlbum(song)
                1 -> showArtist(song)
                2 -> {
                    val addToPlaylistDialog = AddToPlaylistDialog()
                    val bundle = Bundle()
                    val tempSongs = java.util.ArrayList<Song>()
                    tempSongs.add(song)
                    bundle.putParcelableArrayList(AddToPlaylistDialog.ARGUMENT_SONGS, tempSongs)
                    addToPlaylistDialog.arguments = bundle
                    addToPlaylistDialog.show(fragmentManager, "AddToPlaylistDialog")

                }
                3 -> showEditor(song)
                4 -> {
                    val intent = Intent(Intent.ACTION_SEARCH)
                    intent.`package` = "com.google.android.youtube"
                    intent.putExtra("query", song.artistName + " " + song.title)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                }
                5 -> {
                    val shareIntent = Intent()
                    shareIntent.action = Intent.ACTION_SEND
                    val audioUri = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, "" + song.id)
                    shareIntent.putExtra(Intent.EXTRA_STREAM, audioUri)
                    shareIntent.type = "audio/*"
                    startActivity(Intent.createChooser(shareIntent, "Send with"))
                }
            }
        }

        return builder.create()
    }

    private fun showAlbum(song: Song){
        dismiss()
        (activity as MiniPlayerActivity).closeNowPlay()
        val detailFragment = activity!!.supportFragmentManager.findFragmentByTag(MainActivity.Fragments.ALBUM_DETAIL)
                as AlbumDetailFragment? ?: AlbumDetailFragment()
        AlbumDetailPresenter(song.getAlbum(), detailFragment, Injection.provideMediaRepository(context!!.applicationContext),
                Injection.provideDataRepository(context!!.applicationContext))
        doSharedTransaction(R.id.main_container, detailFragment, MainActivity.Fragments.ALBUM_DETAIL, song.getAlbum())
    }

    private fun showEditor(song: Song){
        dismiss()
        (activity as MiniPlayerActivity).closeNowPlay()
        val detailFragment = activity!!.supportFragmentManager.findFragmentByTag(MainActivity.Fragments.EDITOR)
                as EditorFragment? ?: EditorFragment()
        EditorPresenter(detailFragment, song, Injection.provideMediaRepository(context!!.applicationContext),
                Injection.provideDataRepository(context!!.applicationContext))
        doSharedTransaction(R.id.main_container, detailFragment, MainActivity.Fragments.EDITOR, song)
    }

    private fun showArtist(song: Song){
        dismiss()
        (activity as MiniPlayerActivity).closeNowPlay()
        val detailFragment = activity!!.supportFragmentManager.findFragmentByTag(MainActivity.Fragments.ARTIST_DETAIL)
                as ArtistDetailFragment? ?: ArtistDetailFragment()
        ArtistDetailPresenter(song.getArtist(), detailFragment, Injection.provideMediaRepository(context!!.applicationContext),
                Injection.provideDataRepository(context!!.applicationContext))
        doSharedTransaction(R.id.main_container, detailFragment, MainActivity.Fragments.ARTIST_DETAIL, song.getArtist())
    }


    companion object {
        val ARGUMENT_SONG = "ARGUMENT_SONG"
    }
}