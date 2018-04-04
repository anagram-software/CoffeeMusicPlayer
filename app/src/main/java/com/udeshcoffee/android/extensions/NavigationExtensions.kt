package com.udeshcoffee.android.extensions

import android.os.Build
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.transition.Fade
import android.view.View
import android.widget.Toast
import com.udeshcoffee.android.R
import com.udeshcoffee.android.model.*
import com.udeshcoffee.android.ui.dialogs.AddToPlaylistDialog
import com.udeshcoffee.android.ui.dialogs.CollectionLongDialog
import com.udeshcoffee.android.ui.dialogs.SongLongDialog
import com.udeshcoffee.android.ui.main.MainActivity
import com.udeshcoffee.android.ui.main.detail.albumdetail.AlbumDetailFragment
import com.udeshcoffee.android.ui.main.detail.artistdetail.ArtistDetailFragment
import com.udeshcoffee.android.ui.main.detail.genredetail.GenreDetailFragment
import com.udeshcoffee.android.ui.main.detail.playlistdetail.PlaylistDetailFragment
import com.udeshcoffee.android.ui.main.editor.EditorFragment
import com.udeshcoffee.android.ui.main.equalizer.EqualizerFragment
import com.udeshcoffee.android.ui.main.favorites.FavoritesFragment
import com.udeshcoffee.android.ui.main.library.LibraryFragment
import com.udeshcoffee.android.ui.main.playlist.PlaylistFragment
import com.udeshcoffee.android.ui.main.search.SearchFragment


/**
 * Created by Udathari on 1/14/2018.
 */

private const val main_container = R.id.main_container

fun FragmentManager.navigateToDetail(detail: Any, sharedElement: View? = null) {

    val transaction = this.beginTransaction()

    val fragment: Fragment

    val id = when (detail) {
        is Album -> {
            fragment = AlbumDetailFragment.create(detail)
            transaction.replace(main_container, fragment, MainActivity.Fragments.ALBUM_DETAIL)
                    .addToBackStack(MainActivity.Fragments.ALBUM_DETAIL)
            detail.id
        }
        is Artist -> {
            fragment = ArtistDetailFragment.create(detail)
            transaction.replace(main_container, fragment, MainActivity.Fragments.ARTIST_DETAIL)
                    .addToBackStack(MainActivity.Fragments.ARTIST_DETAIL)
            detail.id
        }
        is Genre -> {
            fragment = GenreDetailFragment.create(detail)
            transaction.replace(main_container, fragment, MainActivity.Fragments.GENRE_DETAIL)
                    .addToBackStack(MainActivity.Fragments.GENRE_DETAIL)
            detail.id
        }
        is Playlist -> {
            fragment = PlaylistDetailFragment.create(detail)
            transaction.replace(main_container, fragment, MainActivity.Fragments.PLAYLIST_DETAIL)
                    .addToBackStack(MainActivity.Fragments.PLAYLIST_DETAIL)
            detail.id
        }
        else -> throw IllegalArgumentException("Illegal detail")
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//        sharedElement?.let { transaction.addSharedElement(sharedElement, "$id") }
//        fragment.sharedElementEnterTransition = DetailsTransition()
        fragment.enterTransition = Fade()
        fragment.exitTransition = Fade()
//        fragment.sharedElementReturnTransition = DetailsTransition()
    }
    transaction.setReorderingAllowed(true)
    transaction.commit()
}

fun FragmentManager.navigateToEditor(song: Song) {
    val fragment = EditorFragment.create(song)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        fragment.enterTransition = Fade()
        fragment.exitTransition = Fade()
    }
    this.beginTransaction().replace(main_container, fragment, MainActivity.Fragments.EDITOR)
            .addToBackStack(MainActivity.Fragments.EDITOR)
            .commit()
}

fun FragmentManager.navigateToSearch() {
    val fragment = SearchFragment.create()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        fragment.enterTransition = Fade()
        fragment.exitTransition = Fade()
    }
    this.beginTransaction().replace(main_container, fragment, MainActivity.Fragments.SEARCH)
            .addToBackStack(MainActivity.Fragments.SEARCH)
            .commit()
}

fun FragmentManager.navigateToLibrary() {
    val fragment = LibraryFragment.create()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        fragment.enterTransition = Fade()
        fragment.exitTransition = Fade()
    }
    this.beginTransaction().replace(main_container, fragment, MainActivity.Fragments.LIBRARY)
            .addToBackStack(MainActivity.Fragments.LIBRARY)
            .commit()
}

fun FragmentManager.navigateToPlaylist() {
    val fragment = PlaylistFragment.create()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        fragment.enterTransition = Fade()
        fragment.exitTransition = Fade()
    }
    this.beginTransaction().replace(main_container, fragment, MainActivity.Fragments.PLAYLISTS)
            .addToBackStack(MainActivity.Fragments.PLAYLISTS)
            .commit()
}

fun FragmentManager.navigateToEqualizer() {
    val fragment = EqualizerFragment.create()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        fragment.enterTransition = Fade()
        fragment.exitTransition = Fade()
    }
    this.beginTransaction().replace(main_container, fragment, MainActivity.Fragments.EQUALIZER)
            .addToBackStack(MainActivity.Fragments.EQUALIZER)
            .commit()
}

fun FragmentManager.navigateToFavorites() {
    val fragment = FavoritesFragment.create()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        fragment.enterTransition = Fade()
        fragment.exitTransition = Fade()
    }
    this.beginTransaction().replace(main_container, fragment, MainActivity.Fragments.FAVORITES)
            .addToBackStack(MainActivity.Fragments.FAVORITES)
            .commit()
}

fun Fragment.openSongLongDialog(song: Song) {
    val mDialog = SongLongDialog()
    val bundle = Bundle()
    bundle.putParcelable(SongLongDialog.ARGUMENT_SONG, song)
    mDialog.arguments = bundle
    mDialog.show(fragmentManager, "SongLongDialog")
}

fun Fragment.openCollectionLongDialog(title: String, songs: List<Song>) {
    CollectionLongDialog.create(title, songs as ArrayList<Song>).show(fragmentManager, "CollectionLongDialog")
}

fun Fragment.openAddToPlaylistDialog(songs: ArrayList<Song>, thisPlaylistId: Long? = null) {
    if (songs.isNotEmpty()) {
        AddToPlaylistDialog.create(songs, thisPlaylistId).show(fragmentManager, "AddToPlaylistDialog")
    } else {
        Toast.makeText(context, "No songs available", Toast.LENGTH_SHORT).show()
    }
}

