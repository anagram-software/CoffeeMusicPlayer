package com.udeshcoffee.android.data.remote.genius

import com.udeshcoffee.android.data.remote.genius.SearchResponse
import io.reactivex.Observable
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Created by Udathari on 9/15/2017.
 */
interface SearchService {
    @GET("search")
    fun search(@Query("q") song: String): Observable<SearchResponse>

    @GET("search")
    fun searchSynchronous(@Query("q") song: String): Call<SearchResponse>

}