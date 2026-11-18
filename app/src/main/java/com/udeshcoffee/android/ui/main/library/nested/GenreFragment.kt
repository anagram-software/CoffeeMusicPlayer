package com.udeshcoffee.android.ui.main.library.nested

import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import androidx.lifecycle.lifecycleScope
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import com.udeshcoffee.android.R
import com.udeshcoffee.android.data.MediaRepository
import com.udeshcoffee.android.extensions.openCollectionLongDialog
import com.udeshcoffee.android.interfaces.OnItemClickListener
import com.udeshcoffee.android.model.Genre
import com.udeshcoffee.android.recyclerview.EmptyRecyclerView
import com.udeshcoffee.android.ui.common.adapters.GenreAdapter
import com.udeshcoffee.android.ui.main.detail.genredetail.GenreDetailFragment
import org.koin.android.ext.android.inject

/**
* Created by Udathari on 8/27/2017.
*/

class GenreFragment : androidx.fragment.app.Fragment() {

    val TAG = "GenreFragment"

    private val mediaRepository: MediaRepository by inject()

    private var dataJob: Job? = null
    private var genreAdpt: GenreAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.frag_linear, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val genreView = view.findViewById<EmptyRecyclerView>(R.id.linear_list)
        // specify an adapter (see also next example)
        genreAdpt = GenreAdapter()
        genreView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        genreView.setEmptyView(view.findViewById(R.id.empty_view))
        genreView.hasFixedSize()

        genreAdpt?.setOnItemClickListener(object : OnItemClickListener {
            override fun onItemClick(position: Int) {
                genreAdpt?.getItem(position)?.let { showDetailUI(it) }
            }

            override fun onItemLongClick(position: Int) {
                genreAdpt?.getItem(position)?.let {
                    viewLifecycleOwner.lifecycleScope.launch {
                                val songs = mediaRepository.loadGenreSongs(it.id)
                                openCollectionLongDialog(it.name, songs)
                            }
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
        dataJob = viewLifecycleOwner.lifecycleScope.launch {
            mediaRepository.observeGenres().collect { genreAdpt?.accept(it) }
        }
    }

    fun dispose(){
        dataJob?.cancel()
    }

    fun showDetailUI(detail: Genre) {
        view?.findNavController()?.navigate(R.id.genreDetailFragment, GenreDetailFragment.createBundle(detail))
    }

    companion object {
        fun create(): GenreFragment = GenreFragment()
    }

}
