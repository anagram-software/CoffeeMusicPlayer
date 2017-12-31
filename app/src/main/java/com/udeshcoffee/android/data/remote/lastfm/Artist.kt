package com.udeshcoffee.android.data.remote.lastfm

import com.udeshcoffee.android.api.lastfm.Image

/**
* Created by Udathari on 9/15/2017.
*/
data class Artist(val name: String, val image: List<Image>?, val tags: Tags, val bio: Bio?)
