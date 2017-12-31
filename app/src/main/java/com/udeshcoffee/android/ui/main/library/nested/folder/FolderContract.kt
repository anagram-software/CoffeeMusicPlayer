package com.udeshcoffee.android.ui.main.library.nested.folder

import com.udeshcoffee.android.model.Folder
import com.udeshcoffee.android.model.HistoryEntry
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.ui.main.SongContainingPresenter
import com.udeshcoffee.android.ui.main.SongContainingView
import io.reactivex.Observable
import java.io.File
import java.util.*

/**
 * Created by Udathari on 9/5/2017.
 */
interface FolderContract {

    interface View: SongContainingView<Presenter> {

        fun populateItems(items: List<Folder>, scrollTo: Int)

        fun setPath(path: String)

        fun showHideLoading(show: Boolean)

        fun showErrorToast(error: String)

        fun showCollectionLongDialog(title: String, items: Observable<List<Song>>)

    }

    interface Presenter: SongContainingPresenter {

        fun upClicked()

        fun folderItemClicked(item: Folder, lastVisibleItem: Int)

        fun folderItemLongClicked(item: Folder)

        val history : ArrayList<HistoryEntry>

        var currentDir: File?

        var sortOrder: Int

    }

}