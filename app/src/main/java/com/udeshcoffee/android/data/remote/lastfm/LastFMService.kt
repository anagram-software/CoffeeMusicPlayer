package com.udeshcoffee.android.data.remote.lastfm

import com.udeshcoffee.android.data.remote.lastfm.ArtistResponse
import io.reactivex.Observable
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Created by Udathari on 9/15/2017.
 */
interface LastFMService {
    @GET("2.0/?method=artist.getinfo")
    fun getArtistInfo(@Query("artist") artist: String,
                      @Query("autocorrect") autoCorrect: String = "1"): Observable<ArtistResponse>

}