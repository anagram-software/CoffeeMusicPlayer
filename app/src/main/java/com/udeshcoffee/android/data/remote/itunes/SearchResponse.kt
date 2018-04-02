package com.udeshcoffee.android.data.remote.itunes

import com.google.gson.annotations.SerializedName



/**
 * Created by Udathari on 12/5/2017.
 */
class SearchResponse(
        @SerializedName("resultCount")
        var resultCount: Int = 0,
        @SerializedName("results")
        var results: List<Results>? = null
) {

    class Results {
        @SerializedName("wrapperType")
        var wrapperType: String? = null
        @SerializedName("kind")
        var kind: String? = null
        @SerializedName("artistId")
        var artistId: Int = 0
        @SerializedName("collectionId")
        var collectionId: Int = 0
        @SerializedName("trackId")
        var trackId: Int = 0
        @SerializedName("artistName")
        var artistName: String? = null
        @SerializedName("collectionName")
        var collectionName: String? = null
        @SerializedName("trackName")
        var trackName: String? = null
        @SerializedName("collectionCensoredName")
        var collectionCensoredName: String? = null
        @SerializedName("trackCensoredName")
        var trackCensoredName: String? = null
        @SerializedName("artistViewUrl")
        var artistViewUrl: String? = null
        @SerializedName("collectionViewUrl")
        var collectionViewUrl: String? = null
        @SerializedName("trackViewUrl")
        var trackViewUrl: String? = null
        @SerializedName("previewUrl")
        var previewUrl: String? = null
        @SerializedName("artworkUrl30")
        var artworkUrl30: String? = null
        @SerializedName("artworkUrl60")
        var artworkUrl60: String? = null
        @SerializedName("artworkUrl100")
        var artworkUrl100: String? = null
        @SerializedName("collectionPrice")
        var collectionPrice: Double = 0.toDouble()
        @SerializedName("trackPrice")
        var trackPrice: Double = 0.toDouble()
        @SerializedName("releaseDate")
        var releaseDate: String? = null
        @SerializedName("collectionExplicitness")
        var collectionExplicitness: String? = null
        @SerializedName("trackExplicitness")
        var trackExplicitness: String? = null
        @SerializedName("discCount")
        var discCount: Int = 0
        @SerializedName("discNumber")
        var discNumber: Int = 0
        @SerializedName("trackCount")
        var trackCount: Int = 0
        @SerializedName("trackNumber")
        var trackNumber: Int = 0
        @SerializedName("trackTimeMillis")
        var trackTimeMillis: Int = 0
        @SerializedName("country")
        var country: String? = null
        @SerializedName("currency")
        var currency: String? = null
        @SerializedName("primaryGenreName")
        var primaryGenreName: String? = null
        @SerializedName("isStreamable")
        var isStreamable: Boolean = false
    }
}