package com.udeshcoffee.android.data.remote

import android.util.Log
import com.annimon.stream.Collectors
import com.annimon.stream.Stream
import com.udeshcoffee.android.data.remote.genius.Response
import com.udeshcoffee.android.data.remote.genius.SearchService
import com.udeshcoffee.android.data.remote.itunes.ITunesService
import com.udeshcoffee.android.data.remote.itunes.SearchResponse
import com.udeshcoffee.android.data.remote.lastfm.ArtistResponse
import com.udeshcoffee.android.data.remote.lastfm.ImageArtist
import com.udeshcoffee.android.data.remote.lastfm.LastFMService
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.safety.Whitelist
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.text.Normalizer
import java.util.regex.Pattern

/**
 * Created by Udathari on 12/5/2017.
 */
object RemoteDataSource {

    private val geniusBaseUrl: String = "http://api.genius.com/"
    private val itunesBaseUrl: String = "https://itunes.apple.com/"
    private val lastFMBaseUrl: String = "http://ws.audioscrobbler.com/"

    private val itunesParameters = arrayOf(
            Pair("country", "US")
    )

    private val geniusParameters = arrayOf(
            Pair("access_token", "eACAhiFiFbGDWCsoAauVF-S-hra_cSDB5JC7RBv2RbIzmEPn7WEjhIQEZIDBgpzK")
    )

    private val lastFMParameters = arrayOf(
            Pair("api_key", "885092446fd7c7e6ef725a9289ff3740"),
            Pair("format", "json")
    )

    private val USER_AGENT = "Mozilla/5.0 (Linux; U; Android 6.0.1; ko-kr; Build/IML74K) AppleWebkit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30"

    private fun createOkHttpClient(parameters: Array<Pair<String, String>>?): OkHttpClient {
        val httpClient = OkHttpClient.Builder()
        httpClient.addInterceptor { chain ->
            val original = chain.request()
            val originalHttpUrl = original.url()
            val urlBuilder = originalHttpUrl.newBuilder()
            parameters?.let {
                for (parameter in it) {
                    urlBuilder.addQueryParameter(parameter.first, parameter.second)
                }
            }
            val url = urlBuilder.build()
            // Request customization: add request headers
            val requestBuilder = original.newBuilder().url(url)
            val request = requestBuilder.build()
            chain.proceed(request)
        }
        return httpClient.build()
    }

    private fun createRetrofit(baseUrl: String, parameters: Array<Pair<String, String>>?, isReactive: Boolean): Retrofit {
        val builder = Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
        if (isReactive) builder.addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        return builder.client(createOkHttpClient(parameters))
                .build()
    }

    // Itunes
    private fun getItunesService(): ITunesService {
        val retrofit = createRetrofit(itunesBaseUrl, itunesParameters, true)
        return retrofit.create<ITunesService>(ITunesService::class.java)
    }

    fun searchItunes(song: String, artist: String): Observable<SearchResponse> {
        return getItunesService().search(song)
                .map { filterItunesSearch(it, artist) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .take(1)
    }

    private fun filterItunesSearch(searchResponse: SearchResponse?, artist: String): SearchResponse {
        val results = Stream.of(searchResponse?.results)
                .filter {
                    Log.d("Itunes", "${it.artistName} ${it.trackName}")
                    if (it.artistName != null)
                        it.artistName!!.contains(artist, true)
                    else
                        false
                }
                .collect(Collectors.toList())
        return SearchResponse(results.size, results)
    }

    // Genius
    private fun getGeniusSearchService(isReactive: Boolean): SearchService {
        val retrofit = createRetrofit(geniusBaseUrl, geniusParameters, isReactive)
        return retrofit.create<SearchService>(SearchService::class.java)
    }

    fun searchGenius(song: String, artist: String, shouldCheckEqual: Boolean): Observable<com.udeshcoffee.android.data.remote.genius.SearchResponse> {
        return getGeniusSearchService(true).search(song)
                .map {
                    return@map filterGeniusSearch(it, song, artist, shouldCheckEqual)
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .take(1)
    }

    fun searchGeniusSynchronous(song: String, artist: String): com.udeshcoffee.android.data.remote.genius.SearchResponse? {
        val call = getGeniusSearchService(false).searchSynchronous(song)
        return try {
            filterGeniusSearch(call.execute().body(), song, artist, true)
        } catch (e: Exception) {
            null
        }
    }

    private fun filterGeniusSearch(searchResponse: com.udeshcoffee.android.data.remote.genius.SearchResponse?,
                                   title: String, artist: String, shouldCheckEqual: Boolean): com.udeshcoffee.android.data.remote.genius.SearchResponse {
        val hits = Stream.of(searchResponse?.response?.hits)
                .filter { it.result.primary_artist.name.contains(artist, true) }
                .collect(Collectors.toList())
        val equalHits = hits.filter { it.result.title_with_featured.equals(title, true) }
        val response = if (equalHits.isEmpty() || !shouldCheckEqual) {
            Response(hits)
        } else {
            Response(equalHits)
        }
        return com.udeshcoffee.android.data.remote.genius.SearchResponse(response)
    }

    fun loadGenius(path: String): Observable<String?> {
        return Observable.just(path)
                .map {
                    return@map loadGeniusSynchronous(path)
                }
                .take(1)
    }

    fun loadGeniusSynchronous(path: String): String {
        val lyricsPage: Document
        val text: String

        try {
            lyricsPage = Jsoup.connect("http://www.genius.com$path").userAgent(USER_AGENT).get()
            val lyricsDiv = lyricsPage.select(".lyrics")
            if (lyricsDiv.isEmpty()) {
                return ""
            } else {
                text = Jsoup.clean(lyricsDiv.html(), Whitelist.none().addTags("br")).trim()
            }
        } catch (e: HttpStatusException) {
            return ""
        } catch (e: IOException) {
            return ""
        } catch (e: StringIndexOutOfBoundsException) {
            return ""
        }

        val pattern = Pattern.compile("\\[.+]")
        val builder = StringBuilder()
        for (line in text.split("<br> ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
            val strippedLine = line.replace("\\s".toRegex(), "")
            if (!(strippedLine.isEmpty() && builder.isEmpty())) {
                if (!pattern.matcher(strippedLine).matches()) {
                    builder.append(line.replace("\\P{Print}".toRegex(), "")).append("<br/>")
                } else {
                    builder.append("<font color=#1976D2><b>").append(line.replace("[^a-zA-Z0-9\\s]".toRegex(), " ")).append("</b></font>").append("<br/>")
                }
            }
        }
        if (builder.length > 5)
            builder.delete(builder.length - 5, builder.length)
        return Normalizer.normalize(builder.toString(), Normalizer.Form.NFD)
    }


    private fun loadLastFMImageSynchronous(url: String): String {
        val lastFMPage: Document
        val text: String

        try {
            Log.d("LastFM", "Image URL $url")
            lastFMPage = Jsoup.connect(url).userAgent(USER_AGENT).get()
            val lastFMImageDiv = lastFMPage.select(".header-new-background-image")
            val lastFMImageDivHTML = lastFMImageDiv.outerHtml()
            if (lastFMImageDivHTML.isEmpty()) {
                Log.d("LastFM", "Image Div empty")
                return ""
            } else {
                text = lastFMImageDivHTML.split("content=\"")[1].split("\"></div>")[0]
            }
        } catch (e: HttpStatusException) {
            return ""
        } catch (e: IOException) {
            return ""
        } catch (e: StringIndexOutOfBoundsException) {
            return ""
        }

        Log.d("LastFM", "Image Div $text")
        return Normalizer.normalize(text, Normalizer.Form.NFD)
    }

    // LastFM
    private fun getLastFMService(): LastFMService {
        val retrofit = createRetrofit(lastFMBaseUrl, lastFMParameters, true)
        return retrofit.create<LastFMService>(LastFMService::class.java)
    }

    fun searchLastFMArtist(artist: String): Single<ImageArtist> {
        return getLastFMService().getArtistInfo(artist)
                .map {
                    if (it.artist != null) {
                        val image = loadLastFMImageSynchronous(it.artist.url)
                        return@map ImageArtist(it.artist, image)
                    } else throw Exception("Null artist")
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .firstOrError()
    }

}