package com.udeshcoffee.android.data.remote.genius

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

interface GeniusService {
    @GET("search")
    suspend fun search(@Query("q") query: String): SearchResponse
}

@Serializable
data class SearchResponse(val response: Response? = null)

@Serializable
data class Response(val hits: List<Hit> = emptyList())

@Serializable
data class Hit(val type: String = "", val result: Result)

@Serializable
data class Result(
        @SerialName("title_with_featured") val titleWithFeatured: String = "",
        val path: String = "",
        @SerialName("primary_artist") val primaryArtist: Artist
)

@Serializable
data class Artist(val id: Long = 0, val name: String = "")
