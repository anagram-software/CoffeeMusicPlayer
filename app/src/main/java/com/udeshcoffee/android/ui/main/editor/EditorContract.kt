package com.udeshcoffee.android.ui.main.editor

import android.net.Uri
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.ui.BasePresenter
import com.udeshcoffee.android.ui.BaseView

/**
* Created by Udathari on 9/28/2017.
*/
interface EditorContract {

    interface View: BaseView<Presenter> {

        fun setAlbumArt(song: Song)

        fun setAlbumArt(uri: Uri)

        fun setAlbumArt(url: String)

        fun setData(title: String? = null, album: String? = null, artist: String? = null,
                    genre: String? = null, year: String?= null, trackNo: Int? = null, discNo: Int? = null)

        fun setPath(path: String)

        fun enableDisableReset(isEnable: Boolean)

        fun finish()

        fun showSearchDialog(id:Long ,title: String, artist: String)

        fun showToast(message: String)

        fun showHideProgressDialog(isShow: Boolean, message: String? = null)

        fun showPermissionDialog()

        fun showSelectImageUI()

    }

    interface Presenter: BasePresenter<View> {

        fun imageSelected(uri: Uri)

        fun actionSearch()

        fun actionSelectImage()

        fun actionReset()

        fun save(title: String, album: String, artist: String, genre: String, year: String, trackNo: String, discNo: String)

        fun search(title: String, artist: String)

        fun disposeCollectionDisposable()

        var song: Song

    }
}