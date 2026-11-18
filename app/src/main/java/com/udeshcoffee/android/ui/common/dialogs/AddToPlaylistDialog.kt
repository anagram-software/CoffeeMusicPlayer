package com.udeshcoffee.android.ui.common.dialogs

import androidx.lifecycle.lifecycleScope
import android.widget.ArrayAdapter
import kotlinx.coroutines.launch
import com.udeshcoffee.android.utils.ProcessLifecycleScope
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.appcompat.app.AlertDialog
import android.widget.Toast
import com.udeshcoffee.android.data.MediaRepository
import com.udeshcoffee.android.model.Playlist
import com.udeshcoffee.android.model.Song
import org.koin.android.ext.android.inject
import java.util.*

/**
* Created by Udathari on 10/19/2017.
*/
class AddToPlaylistDialog: androidx.fragment.app.DialogFragment() {

    val mediaRepository: MediaRepository by inject()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val songs = this.arguments!!.getParcelableArrayList<Song>(ARGUMENT_SONGS)
        if (songs == null) {
            Toast.makeText(context, "No songs here", Toast.LENGTH_SHORT).show()
            dismissAllowingStateLoss()
        }

        val thisPlaylistId = this.arguments!!.getLong(ARGUMENT_THIS_PLAYLIST_ID, -2155)

        // Playlists load asynchronously; "Add New" is always the last row.
        var playlists: List<Playlist> = emptyList()
        val adapter = ArrayAdapter<String>(requireContext(), android.R.layout.select_dialog_item, mutableListOf("Add New"))
        lifecycleScope.launch {
            playlists = mediaRepository.loadPlaylistsExcept(thisPlaylistId)
            adapter.clear()
            adapter.addAll(playlists.map { it.title } + "Add New")
        }

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Add to playlist")
                .setAdapter(adapter) { _, i ->
                    if (i >= playlists.size) {
                        NewPlaylistDialog.create().show(parentFragmentManager, "NewPlaylistDialog")
                        dismiss()
                    } else {
                        songs?.let { addToPlaylist(playlists[i].id, it) }
                    }
                }

        return builder.create()
    }

    private fun addToPlaylist(playlistId: Long, songs: ArrayList<Song>) {
        ProcessLifecycleScope.launch { mediaRepository.addToPlaylist(playlistId, songs) }
        Toast.makeText(context, "Added " + songs.size + " to playlist", Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val ARGUMENT_SONGS = "ARGUMENT_SONGS"
        const val ARGUMENT_THIS_PLAYLIST_ID = "ARGUMENT_THIS_PLAYLIST_ID"

        fun create(songs: ArrayList<Song>, thisPlaylistId: Long? = null): AddToPlaylistDialog {
            val addToPlaylistDialog = AddToPlaylistDialog()
            val bundle = Bundle()
            bundle.putParcelableArrayList(AddToPlaylistDialog.ARGUMENT_SONGS, songs)
            thisPlaylistId?.let { bundle.putLong(AddToPlaylistDialog.ARGUMENT_THIS_PLAYLIST_ID, it) }
            addToPlaylistDialog.arguments = bundle
            return addToPlaylistDialog
        }
    }
}