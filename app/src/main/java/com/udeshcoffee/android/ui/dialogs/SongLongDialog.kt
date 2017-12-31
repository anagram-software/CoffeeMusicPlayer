package com.udeshcoffee.android.ui.dialogs

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.widget.Toast
import com.udeshcoffee.android.R
import com.udeshcoffee.android.data.DataRepository
import com.udeshcoffee.android.doSharedTransaction
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.playSong
import com.udeshcoffee.android.queueSong
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
class SongLongDialog: DialogFragment() {

    private lateinit var dataRepository: DataRepository

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        dataRepository = Injection.provideDataRepository(context!!)

        val song = this.arguments!!.getParcelable<Song>(ARGUMENT_SONG)

        val items = arrayOf(getString(R.string.action_play),
                getString(R.string.action_play_next),
                getString(R.string.action_queue),
                getString(R.string.action_go_to_album),
                getString(R.string.action_go_to_artist),
                getString(R.string.action_add_to_playlist),
                getString(R.string.action_edit),
                getString(R.string.action_delete),
                getString(R.string.action_add_to_favorites),
                getString(R.string.action_share))

        if (dataRepository.isFavoriteSync(song.id)){
            items[8] = getString(R.string.action_rem_from_favorites);
        }

        val builder = AlertDialog.Builder(context!!)

        builder.setTitle(song.title).setItems(items) { _, which ->
            when (which) {
                0 -> playSong(0, ArrayList<Song>().also{
                    it.add(song)
                }, true)
                1 -> queueSong(ArrayList<Song>().also{
                    it.add(song)
                }, true)
                2 -> queueSong(ArrayList<Song>().also{
                    it.add(song)
                }, false)
                3 -> showAlbum(song)
                4 -> showArtist(song)
                5 -> {
                    val addToPlaylistDialog = AddToPlaylistDialog()
                    val bundle = Bundle()
                    val tempSongs = java.util.ArrayList<Song>()
                    tempSongs.add(song)
                    bundle.putParcelableArrayList(AddToPlaylistDialog.ARGUMENT_SONGS, tempSongs)
                    addToPlaylistDialog.arguments = bundle
                    addToPlaylistDialog.show(fragmentManager, "AddToPlaylistDialog")

                }
                6 -> showEditor(song)
                7 -> {
                    val deleteSongDialog = DeleteSongDialog()
                    val bundle2 = Bundle()
                    bundle2.putParcelable("song", song)
                    deleteSongDialog.arguments = bundle2
                    deleteSongDialog.show(fragmentManager, "DeleteSongPlaylistDialog")
                }
                8 -> {
                    dataRepository.toggleFavorite(song.id){
                        if (it){
                            Toast.makeText(context, "Added to favorites", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Removed from favorites", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                9 -> {
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
        val detailFragment = activity!!.supportFragmentManager.findFragmentByTag(MainActivity.Fragments.ALBUM_DETAIL)
                as AlbumDetailFragment? ?: AlbumDetailFragment()
        AlbumDetailPresenter(song.getAlbum(), detailFragment, Injection.provideMediaRepository(context!!.applicationContext),
                Injection.provideDataRepository(context!!))
        doSharedTransaction(R.id.main_container, detailFragment, MainActivity.Fragments.ALBUM_DETAIL, song.getAlbum())
    }

    private fun showEditor(song: Song){
        dismiss()
        val detailFragment = activity!!.supportFragmentManager.findFragmentByTag(MainActivity.Fragments.EDITOR)
                as EditorFragment? ?: EditorFragment()
        EditorPresenter(detailFragment, song, Injection.provideMediaRepository(context!!.applicationContext),
                Injection.provideDataRepository(context!!))
        doSharedTransaction(R.id.main_container, detailFragment, MainActivity.Fragments.EDITOR, song)
    }

    private fun showArtist(song: Song){
        dismiss()
        val detailFragment = activity!!.supportFragmentManager.findFragmentByTag(MainActivity.Fragments.ARTIST_DETAIL)
                as ArtistDetailFragment? ?: ArtistDetailFragment()
        ArtistDetailPresenter(song.getArtist(), detailFragment, Injection.provideMediaRepository(context!!.applicationContext),
                Injection.provideDataRepository(context!!))
        doSharedTransaction(R.id.main_container, detailFragment, MainActivity.Fragments.ARTIST_DETAIL, song.getArtist())
    }


    companion object {
        val ARGUMENT_SONG = "ARGUMENT_SONG"
    }
}