package com.udeshcoffee.android.data.remote.itunes

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName


/**
 * Created by Udathari on 12/5/2017.
 */
class SearchResponse(
        @SerializedName("results")
        var results: Results? = null
)

class Results(
        @SerializedName("songs")
        var songs: SongResponse? = null
)

class SongResponse(
        @SerializedName("data")
        @Expose
        var data: List<ItunesSong>? = null
)


class ItunesSong(
        @SerializedName("attributes")
        @Expose
        var attributes: Attributes? = null,
        @SerializedName("href")
        @Expose
        var href: String? = null,
        @SerializedName("id")
        @Expose
        var id: String? = null,
        @SerializedName("type")
        @Expose
        var type: String
)


class Attributes(
        @SerializedName("albumName")
        @Expose
        var albumName: String,
        @SerializedName("artistName")
        @Expose
        var artistName: String,
        @SerializedName("artwork")
        @Expose
        var artwork: Artwork,
        @SerializedName("discNumber")
        @Expose
        var discNumber: Int,
        @SerializedName("genreNames")
        @Expose
        var genreNames: List<String>? = null,
        @SerializedName("name")
        @Expose
        var name: String,
        @SerializedName("releaseDate")
        @Expose
        var releaseDate: String,
        @SerializedName("trackNumber")
        @Expose
        var trackNumber: String
)

class Artwork(
        @SerializedName("url")
        @Expose
        var url: String
)
