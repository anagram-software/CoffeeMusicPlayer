package com.udeshcoffee.android.ui.main.library.nested

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.udeshcoffee.android.R
import com.udeshcoffee.android.data.MediaRepository
import com.udeshcoffee.android.doSharedTransaction
import com.udeshcoffee.android.interfaces.OnItemClickListener
import com.udeshcoffee.android.model.Genre
import com.udeshcoffee.android.openCollectionLongDialog
import com.udeshcoffee.android.recyclerview.EmptyRecyclerView
import com.udeshcoffee.android.ui.adapters.GenreAdapter
import com.udeshcoffee.android.ui.main.detail.genredetail.GenreDetailFragment
import com.udeshcoffee.android.ui.main.detail.genredetail.GenreDetailPresenter
import com.udeshcoffee.android.ui.main.MainActivity
import com.udeshcoffee.android.utils.Injection
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable

/**
 * Created by Udathari on 8/27/2017.
 */

class GenreFragment : Fragment() {

    val TAG = "GenreFragment"

    private lateinit var mediaRepository: MediaRepository

    private var disposable: Disposable? = null
    private var genreAdpt: GenreAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.frag_linear, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mediaRepository = Injection.provideMediaRepository(context!!.applicationContext)

        val genreView = view.findViewById<EmptyRecyclerView>(R.id.linear_list)
        // specify an adapter (see also next example)
        genreAdpt = GenreAdapter()
        genreView.layoutManager = LinearLayoutManager(context)
        genreView.setEmptyView(view.findViewById(R.id.empty_view))
        genreView.hasFixedSize()

        genreAdpt?.setOnItemClickListener(object : OnItemClickListener {
            override fun onItemClick(position: Int) {
                genreAdpt?.getItem(position)?.let { showDetailUI(it) }
            }

            override fun onItemLongClick(position: Int) {
                genreAdpt?.getItem(position)?.let {
                    mediaRepository.getGenreSongs(it.id)
                            .observeOn(AndroidSchedulers.mainThread())
                            .take(1)
                            .subscribe({ songs ->
                                openCollectionLongDialog(it.name, songs)
                            })
                }
            }
        })
        genreView.adapter = genreAdpt
    }

    override fun onResume() {
        super.onResume()
        fetchData()
    }

    override fun onPause() {
        super.onPause()
        dispose()
    }

    private fun fetchData() {
        dispose()
        disposable = mediaRepository.getGenres()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe{genreAdpt?.accept(it)}
    }

    fun dispose(){
        disposable?.let {
            if (!it.isDisposed)
                it.dispose()
        }
    }

    fun showDetailUI(genre: Genre) {
        val detailFragment = activity!!.supportFragmentManager.findFragmentByTag(MainActivity.Fragments.GENRE_DETAIL)
                as GenreDetailFragment? ?: GenreDetailFragment()
        GenreDetailPresenter(genre, detailFragment, Injection.provideMediaRepository(context!!.applicationContext),
                Injection.provideDataRepository(context!!.applicationContext))
        doSharedTransaction(R.id.main_container, detailFragment, MainActivity.Fragments.GENRE_DETAIL, genre)
    }

}
